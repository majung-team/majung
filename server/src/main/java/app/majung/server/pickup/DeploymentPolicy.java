package app.majung.server.pickup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Server-only creation gate for a limited trial, not account or vehicle identity. */
@Component
class DeploymentPolicy {
    private final String creationKey;
    private final boolean developmentToolsEnabled;

    DeploymentPolicy(@Value("${majung.deployment:false}") boolean deployment,
                     @Value("${majung.creation-key:}") String creationKey,
                     @Value("${majung.development-tools:true}") boolean developmentTools,
                     @Value("${majung.retention.enabled:false}") boolean retentionEnabled,
                     @Value("${majung.retention.period:PT24H}") Duration retention,
                     @Value("${majung.rate-limit.enabled:true}") boolean rateLimitEnabled) {
        if (retention.compareTo(Duration.ofHours(1)) < 0) {
            throw new IllegalArgumentException("Retention must be at least one hour");
        }
        if (deployment && (creationKey.length() < 32 || developmentTools || !retentionEnabled || !rateLimitEnabled)) {
            throw new IllegalArgumentException("Deployment requires a creation key, disabled development tools, retention and rate limiting");
        }
        this.creationKey = creationKey;
        this.developmentToolsEnabled = developmentTools && creationKey.isEmpty();
    }

    boolean developmentToolsEnabled() { return developmentToolsEnabled; }

    void requireCreationKey(String supplied) {
        if (creationKey.isEmpty()) return;
        if (supplied == null || supplied.length() != creationKey.length()
                || !MessageDigest.isEqual(creationKey.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(403, "CREATION_RESTRICTED", "이 서버에서는 허용된 경로로만 픽업을 만들 수 있습니다.");
        }
    }
}
