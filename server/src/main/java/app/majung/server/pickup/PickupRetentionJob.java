package app.majung.server.pickup;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "majung.retention.enabled", havingValue = "true")
class PickupRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(PickupRetentionJob.class);
    private final PickupRepository pickupRepository;
    private final Clock clock;
    private final Duration retention;

    PickupRetentionJob(PickupRepository pickupRepository, Clock clock,
                       @Value("${majung.retention.period:PT24H}") Duration retention) {
        this.pickupRepository = pickupRepository;
        this.clock = clock;
        this.retention = retention;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${majung.retention.cleanup-interval-ms:300000}",
               initialDelayString = "${majung.retention.cleanup-interval-ms:300000}")
    public int purgeExpiredPickups() {
        int deleted = pickupRepository.deleteRetainedBefore(clock.instant().minus(retention));
        if (deleted > 0) log.info("Retained pickups removed: count={}", deleted);
        return deleted;
    }
}
