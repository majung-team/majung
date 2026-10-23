package app.majung.server.pickup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:retention-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "majung.retention.enabled=true",
        "majung.retention.cleanup-interval-ms=3600000"
})
@Import(PickupApiTest.TimeConfiguration.class)
class PickupRetentionTest {
    @Autowired PickupRetentionJob job;
    @Autowired PickupRepository pickupRepository;
    @Autowired PickupApiTest.MutableClock clock;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test void deleteOnlyEndedOrExpiredRowsAtRetentionBoundary() {
        clock.reset();
        Instant cutoff = clock.instant().minus(Duration.ofHours(24));
        UUID completed = save(cutoff, clock.instant().plusSeconds(1));
        UUID expired = save(null, cutoff);
        UUID cancelled = save(cutoff.minusSeconds(1), clock.instant());
        UUID recentEnd = save(cutoff.plusSeconds(1), cutoff.minusSeconds(5));
        UUID recentExpiry = save(null, cutoff.plusSeconds(1));
        UUID active = save(null, clock.instant().plusSeconds(600));
        assertEquals(3, job.purgeExpiredPickups());
        for (UUID id : new UUID[]{completed, expired, cancelled}) {
            assertFalse(pickupRepository.existsById(id));
            assertEquals(0, jdbcTemplate.queryForObject("select count(*) from pickup where id = ?", Integer.class, id));
        }
        for (UUID id : new UUID[]{recentEnd, recentExpiry, active}) assertTrue(pickupRepository.existsById(id));
        assertEquals(0, job.purgeExpiredPickups());
    }

    private UUID save(Instant ended, Instant expiry) {
        Pickup pickup = Pickup.create("test-hash", "test-invite", clock.instant(), expiry, expiry,
                new PickupApi.Place("예시 장소", 37.0, 127.0));
        pickup.endedAt = ended;
        pickup.status = ended == null ? Pickup.Status.ACTIVE : Pickup.Status.COMPLETED;
        pickup.participantName = "테스트";
        pickup.vehicleLatitude = 37.0;
        pickupRepository.saveAndFlush(pickup);
        return pickup.id;
    }
}
