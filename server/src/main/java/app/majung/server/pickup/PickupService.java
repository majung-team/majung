package app.majung.server.pickup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PickupService {
    private final PickupRepository pickupRepository;
    private final TokenService tokenService;
    private final Clock clock;
    private final Duration inviteTtl;
    private final Duration pickupTtl;
    private final Duration vehicleFreshness;

    PickupService(PickupRepository pickupRepository, TokenService tokenService, Clock clock,
                  @Value("${majung.invite-ttl}") Duration inviteTtl,
                  @Value("${majung.pickup-ttl}") Duration pickupTtl,
                  @Value("${majung.vehicle-freshness}") Duration vehicleFreshness) {
        if (inviteTtl.isNegative() || inviteTtl.isZero() || pickupTtl.compareTo(inviteTtl) < 0
                || vehicleFreshness.isNegative() || vehicleFreshness.isZero()) {
            throw new IllegalArgumentException("Invalid pickup lifetime configuration");
        }
        this.pickupRepository = pickupRepository;
        this.tokenService = tokenService;
        this.clock = clock;
        this.inviteTtl = inviteTtl;
        this.pickupTtl = pickupTtl;
        this.vehicleFreshness = vehicleFreshness;
    }

    PickupApi.Created createPickup(PickupApi.Place place) {
        Instant now = clock.instant();
        String ownerToken = tokenService.issue();
        String inviteToken = tokenService.issue();
        Pickup pickup = Pickup.create(tokenService.hash(ownerToken), tokenService.hash(inviteToken),
                now, now.plus(inviteTtl), now.plus(pickupTtl), place);
        pickupRepository.save(pickup);
        return new PickupApi.Created(pickup.id, ownerToken, inviteToken, pickup.inviteExpiresAt, pickup.expiresAt);
    }

    PickupApi.Joined joinPickup(UUID id, PickupApi.JoinRequest request) {
        Pickup pickup = locked(id);
        requireOpen(pickup);
        if (!clock.instant().isBefore(pickup.inviteExpiresAt) || pickup.inviteHash == null) {
            throw error(410, "INVITE_UNAVAILABLE", "초대가 만료되었거나 이미 사용됐습니다.");
        }
        if (!tokenService.matches(request.inviteToken(), pickup.inviteHash)) {
            throw error(403, "FORBIDDEN", "이 초대로 참여할 수 없습니다.");
        }
        if (pickup.status != Pickup.Status.INVITING) {
            throw error(409, "INVALID_STATE", "참여할 수 없는 픽업 상태입니다.");
        }
        String guestToken = tokenService.issue();
        pickup.participantId = UUID.randomUUID();
        pickup.participantName = request.displayName().strip();
        pickup.participantHash = tokenService.hash(guestToken);
        pickup.inviteHash = null;
        pickup.status = Pickup.Status.WAITING_APPROVAL;
        return new PickupApi.Joined(pickup.participantId, guestToken, pickup.status.name());
    }

    PickupApi.Invitation rotateInvitation(UUID id, String auth) {
        Pickup pickup = owner(id, auth);
        requireOpen(pickup);
        if (pickup.status != Pickup.Status.INVITING && pickup.status != Pickup.Status.WAITING_APPROVAL) {
            throw error(409, "INVALID_STATE", "위치 공유 중에는 초대를 다시 만들 수 없습니다.");
        }
        String inviteToken = tokenService.issue();
        pickup.inviteHash = tokenService.hash(inviteToken);
        Instant proposedExpiry = clock.instant().plus(inviteTtl);
        pickup.inviteExpiresAt = proposedExpiry.isBefore(pickup.expiresAt) ? proposedExpiry : pickup.expiresAt;
        pickup.participantHash = null;
        pickup.participantId = null;
        pickup.participantName = null;
        pickup.readiness = Pickup.Readiness.WAITING;
        pickup.readinessSequence = 0;
        pickup.status = Pickup.Status.INVITING;
        return new PickupApi.Invitation(inviteToken, pickup.inviteExpiresAt);
    }

    PickupApi.Snapshot approveParticipant(UUID id, UUID participantId, String auth) {
        Pickup pickup = owner(id, auth);
        requireOpen(pickup);
        if (!Objects.equals(pickup.participantId, participantId)) {
            throw error(404, "PARTICIPANT_NOT_FOUND", "참여 요청을 찾을 수 없습니다.");
        }
        if (pickup.status == Pickup.Status.ACTIVE || pickup.status == Pickup.Status.ARRIVED) {
            return toPickupResponse(pickup, true);
        }
        if (pickup.status != Pickup.Status.WAITING_APPROVAL) {
            throw error(409, "INVALID_STATE", "승인할 수 없는 상태입니다.");
        }
        if (!clock.instant().isBefore(pickup.inviteExpiresAt)) {
            throw error(410, "INVITE_EXPIRED", "참여 요청이 만료됐습니다. 새로 초대해주세요.");
        }
        pickup.status = Pickup.Status.ACTIVE;
        return toPickupResponse(pickup, true);
    }

    PickupApi.Snapshot getPickup(UUID id, String auth) {
        String token = tokenService.bearer(auth);
        Pickup pickup = locked(id);
        boolean isOwner = tokenService.matches(token, pickup.ownerHash);
        if (!isOwner && !tokenService.matches(token, pickup.participantHash)) {
            throw error(403, "FORBIDDEN", "이 픽업에 접근할 수 없습니다.");
        }
        return toPickupResponse(pickup, isOwner);
    }

    PickupApi.Snapshot updateReadiness(UUID id, String auth, PickupApi.ReadinessRequest request) {
        String token = tokenService.bearer(auth);
        Pickup pickup = locked(id);
        if (!tokenService.matches(token, pickup.participantHash)) {
            throw error(403, "FORBIDDEN", "탑승자만 준비 상태를 바꿀 수 있습니다.");
        }
        requireSharing(pickup);
        if (request.sequence() == pickup.readinessSequence && request.readiness() == pickup.readiness) {
            return toPickupResponse(pickup, false);
        }
        if (request.sequence() <= pickup.readinessSequence) {
            throw error(409, "STALE_EVENT", "이미 반영된 상태보다 새로운 순서로 요청해주세요.");
        }
        pickup.readiness = request.readiness();
        pickup.readinessSequence = request.sequence();
        return toPickupResponse(pickup, false);
    }

    PickupApi.VehicleState updateVehicle(UUID id, String auth, PickupApi.VehicleRequest request) {
        Pickup pickup = owner(id, auth);
        requireSharing(pickup);
        Instant now = clock.instant();
        if (request.sourceTime().isAfter(now)) {
            throw error(400, "FUTURE_SOURCE_TIME", "미래 시각의 차량 정보는 반영할 수 없습니다.");
        }
        if (request.sequence() == pickup.vehicleSequence
                && Objects.equals(request.latitude(), pickup.vehicleLatitude)
                && Objects.equals(request.longitude(), pickup.vehicleLongitude)
                && Objects.equals(request.durationSeconds(), pickup.durationSeconds)
                && Objects.equals(request.sourceTime(), pickup.vehicleSourceTime)) {
            return toVehicleResponse(pickup);
        }
        if (request.sequence() <= pickup.vehicleSequence || (pickup.vehicleSourceTime != null
                && request.sourceTime().isBefore(pickup.vehicleSourceTime))) {
            throw error(409, "STALE_EVENT", "이전 차량 정보로 현재 상태를 덮을 수 없습니다.");
        }
        pickup.vehicleSequence = request.sequence();
        pickup.vehicleLatitude = request.latitude();
        pickup.vehicleLongitude = request.longitude();
        pickup.durationSeconds = request.durationSeconds();
        pickup.vehicleSourceTime = request.sourceTime();
        pickup.vehicleReceivedAt = now;
        return toVehicleResponse(pickup);
    }

    PickupApi.VehicleState invalidateVehicle(UUID id, String auth, long sequence) {
        Pickup pickup = owner(id, auth);
        requireSharing(pickup);
        if (sequence == pickup.vehicleSequence && pickup.vehicleSourceTime == null) {
            return toVehicleResponse(pickup);
        }
        if (sequence <= pickup.vehicleSequence) {
            throw error(409, "STALE_EVENT", "이전 요청으로 차량 상태를 바꿀 수 없습니다.");
        }
        pickup.vehicleSequence = sequence;
        pickup.clearVehicle();
        return toVehicleResponse(pickup);
    }

    PickupApi.VehicleState getVehicle(UUID id, String auth) {
        String token = tokenService.bearer(auth);
        Pickup pickup = locked(id);
        if (!tokenService.matches(token, pickup.ownerHash)
                && !tokenService.matches(token, pickup.participantHash)) {
            throw error(403, "FORBIDDEN", "이 픽업에 접근할 수 없습니다.");
        }
        requireSharing(pickup);
        return toVehicleResponse(pickup);
    }

    PickupApi.Snapshot arrivePickup(UUID id, String auth) {
        Pickup pickup = owner(id, auth);
        requireSharing(pickup);
        pickup.status = Pickup.Status.ARRIVED;
        return toPickupResponse(pickup, true);
    }

    PickupApi.Snapshot endPickup(UUID id, String auth, Pickup.Status target) {
        Pickup pickup = owner(id, auth);
        if (pickup.status == target) {
            return toPickupResponse(pickup, true);
        }
        requireOpen(pickup);
        if (target == Pickup.Status.COMPLETED && pickup.status != Pickup.Status.ARRIVED) {
            throw error(409, "ARRIVAL_REQUIRED", "도착 후 탑승을 확인하고 완료해주세요.");
        }
        pickup.status = target;
        pickup.endedAt = clock.instant();
        pickup.inviteHash = null;
        pickup.clearVehicle();
        return toPickupResponse(pickup, true);
    }

    private Pickup locked(UUID id) {
        return pickupRepository.findLockedById(id)
                .orElseThrow(() -> error(404, "PICKUP_NOT_FOUND", "픽업을 찾을 수 없습니다."));
    }

    private Pickup owner(UUID id, String auth) {
        String token = tokenService.bearer(auth);
        Pickup pickup = locked(id);
        if (!tokenService.matches(token, pickup.ownerHash)) {
            throw error(403, "FORBIDDEN", "운전자만 이 작업을 할 수 있습니다.");
        }
        return pickup;
    }

    private void requireOpen(Pickup pickup) {
        if (pickup.isTerminal() || pickup.isExpired(clock.instant())) {
            throw error(410, "PICKUP_ENDED", "종료되었거나 만료된 픽업입니다.");
        }
    }

    private void requireSharing(Pickup pickup) {
        requireOpen(pickup);
        if (pickup.status != Pickup.Status.ACTIVE && pickup.status != Pickup.Status.ARRIVED) {
            throw error(403, "APPROVAL_REQUIRED", "운전자 확인 후 이용할 수 있습니다.");
        }
    }

    private PickupApi.Snapshot toPickupResponse(Pickup pickup, boolean isOwner) {
        boolean expired = !pickup.isTerminal() && pickup.isExpired(clock.instant());
        boolean inviteExpired = !pickup.isTerminal()
                && (pickup.status == Pickup.Status.INVITING || pickup.status == Pickup.Status.WAITING_APPROVAL)
                && !clock.instant().isBefore(pickup.inviteExpiresAt);
        boolean visible = !pickup.isTerminal() && !expired && (isOwner
                || pickup.status == Pickup.Status.ACTIVE || pickup.status == Pickup.Status.ARRIVED);
        PickupApi.Place place = visible
                ? new PickupApi.Place(pickup.placeLabel, pickup.placeLatitude, pickup.placeLongitude) : null;
        PickupApi.Participant participant = pickup.participantId == null ? null
                : new PickupApi.Participant(pickup.participantId, pickup.participantName,
                pickup.readiness, pickup.readinessSequence);
        String status = expired ? "EXPIRED" : inviteExpired ? "INVITE_EXPIRED" : pickup.status.name();
        return new PickupApi.Snapshot(pickup.id, status, place, participant,
                pickup.inviteExpiresAt, pickup.expiresAt, pickup.endedAt);
    }

    private PickupApi.VehicleState toVehicleResponse(Pickup pickup) {
        if (pickup.vehicleSourceTime == null) {
            return new PickupApi.VehicleState("WAITING", null, null, null, null, null, pickup.vehicleSequence, 0);
        }
        Instant sourceExpiry = pickup.vehicleSourceTime.plus(vehicleFreshness);
        Instant receiptExpiry = pickup.vehicleReceivedAt.plus(vehicleFreshness);
        Instant validUntil = sourceExpiry.isBefore(receiptExpiry) ? sourceExpiry : receiptExpiry;
        long validForMillis = Math.max(0, Duration.between(clock.instant(), validUntil).toMillis());
        boolean fresh = validForMillis > 0;
        return new PickupApi.VehicleState(fresh ? "FRESH" : "STALE",
                fresh ? pickup.vehicleLatitude : null, fresh ? pickup.vehicleLongitude : null,
                fresh ? pickup.durationSeconds : null, pickup.vehicleSourceTime,
                pickup.vehicleReceivedAt, pickup.vehicleSequence, validForMillis);
    }

    private ApiException error(int status, String code, String message) {
        return new ApiException(status, code, message);
    }
}
