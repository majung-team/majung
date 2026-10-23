package app.majung.server.pickup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/pickups", produces = "application/json")
class PickupApi {
    record Place(@NotBlank @Size(max = 120) String label,
                 @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                 @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) { }
    record CreateRequest(@Valid @NotNull Place place) { }
    record JoinRequest(@NotBlank @Size(min = 43, max = 43) String inviteToken,
                       @NotBlank @Size(max = 12) String displayName) { }
    record ReadinessRequest(@NotNull Pickup.Readiness readiness, @Positive long sequence) { }
    record VehicleInvalidation(@Positive long sequence) { }
    record VehicleRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                          @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
                          @NotNull @PositiveOrZero Long durationSeconds,
                          @NotNull Instant sourceTime, @Positive long sequence) { }
    record Created(UUID pickupId, String driverToken, String inviteToken,
                   Instant inviteExpiresAt, Instant expiresAt) { }
    record Invitation(String inviteToken, Instant expiresAt) { }
    record Joined(UUID participantId, String participantToken, String status) { }
    record Participant(UUID participantId, String displayName, Pickup.Readiness readiness,
                       long readinessSequence) { }
    record Snapshot(UUID pickupId, String status, Place place, Participant participant,
                    Instant inviteExpiresAt, Instant expiresAt, Instant endedAt) { }
    record VehicleState(String freshness, Double latitude, Double longitude, Long durationSeconds,
                        Instant sourceTime, Instant receivedAt, long sequence, long validForMillis) { }

    private final PickupService pickupService;
    private final DeploymentPolicy deploymentPolicy;

    PickupApi(PickupService pickupService, DeploymentPolicy deploymentPolicy) {
        this.pickupService = pickupService;
        this.deploymentPolicy = deploymentPolicy;
    }

    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    Created create(@Valid @RequestBody CreateRequest request,
                   @RequestHeader(value = "X-Majung-Creation-Key", required = false) String creationKey) {
        deploymentPolicy.requireCreationKey(creationKey);
        return pickupService.createPickup(request.place());
    }

    @PostMapping(value = "/{id}/participants", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    Joined join(@PathVariable UUID id, @Valid @RequestBody JoinRequest request) {
        return pickupService.joinPickup(id, request);
    }

    @PostMapping("/{id}/invitations")
    Invitation rotate(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.rotateInvitation(id, auth);
    }

    @PostMapping("/{id}/participants/{participantId}/approval")
    Snapshot approve(@PathVariable UUID id, @PathVariable UUID participantId,
                     @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.approveParticipant(id, participantId, auth);
    }

    @GetMapping("/{id}")
    Snapshot get(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.getPickup(id, auth);
    }

    @PutMapping(value = "/{id}/readiness", consumes = "application/json")
    Snapshot readiness(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth,
                       @Valid @RequestBody ReadinessRequest request) {
        return pickupService.updateReadiness(id, auth, request);
    }

    @PutMapping(value = "/{id}/vehicle-state", consumes = "application/json")
    VehicleState vehicle(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth,
                         @Valid @RequestBody VehicleRequest request) {
        return pickupService.updateVehicle(id, auth, request);
    }

    @DeleteMapping(value = "/{id}/vehicle-state", consumes = "application/json")
    VehicleState invalidateVehicle(@PathVariable UUID id,
                                  @RequestHeader(value = "Authorization", required = false) String auth,
                                  @Valid @RequestBody VehicleInvalidation request) {
        return pickupService.invalidateVehicle(id, auth, request.sequence());
    }

    @GetMapping("/{id}/vehicle-state")
    VehicleState vehicle(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.getVehicle(id, auth);
    }

    @PostMapping("/{id}/arrival")
    Snapshot arrive(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.arrivePickup(id, auth);
    }

    @PostMapping("/{id}/completion")
    Snapshot complete(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.endPickup(id, auth, Pickup.Status.COMPLETED);
    }

    @PostMapping("/{id}/cancellation")
    Snapshot cancel(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        return pickupService.endPickup(id, auth, Pickup.Status.CANCELLED);
    }
}
