package app.majung.server.pickup;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** One pickup is the transaction boundary. Never serialize this persistence object. */
@Entity
class Pickup {
    enum Status { INVITING, WAITING_APPROVAL, ACTIVE, ARRIVED, COMPLETED, CANCELLED }
    enum Readiness { WAITING, PREPARING, READY }

    @Id
    UUID id;
    @Version
    long version;
    String ownerHash;
    String inviteHash;
    Instant inviteExpiresAt;
    Instant expiresAt;
    Instant createdAt;
    Instant endedAt;
    @Enumerated(EnumType.STRING)
    Status status;
    String placeLabel;
    double placeLatitude;
    double placeLongitude;
    UUID participantId;
    String participantName;
    String participantHash;
    @Enumerated(EnumType.STRING)
    Readiness readiness;
    long readinessSequence;
    long vehicleSequence;
    Double vehicleLatitude;
    Double vehicleLongitude;
    Long durationSeconds;
    Instant vehicleSourceTime;
    Instant vehicleReceivedAt;

    protected Pickup() { }

    static Pickup create(String ownerHash, String inviteHash, Instant now, Instant inviteExpiresAt,
                         Instant expiresAt, PickupApi.Place place) {
        Pickup pickup = new Pickup();
        pickup.id = UUID.randomUUID();
        pickup.ownerHash = ownerHash;
        pickup.inviteHash = inviteHash;
        pickup.createdAt = now;
        pickup.inviteExpiresAt = inviteExpiresAt;
        pickup.expiresAt = expiresAt;
        pickup.status = Status.INVITING;
        pickup.placeLabel = place.label();
        pickup.placeLatitude = place.latitude();
        pickup.placeLongitude = place.longitude();
        pickup.readiness = Readiness.WAITING;
        return pickup;
    }

    boolean isTerminal() {
        return status == Status.COMPLETED || status == Status.CANCELLED;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    void clearVehicle() {
        vehicleLatitude = null;
        vehicleLongitude = null;
        durationSeconds = null;
        vehicleSourceTime = null;
        vehicleReceivedAt = null;
    }
}
