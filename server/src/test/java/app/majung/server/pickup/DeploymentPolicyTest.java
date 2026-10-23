package app.majung.server.pickup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeploymentPolicyTest {
    private static final String TEST_KEY = "test-only-not-a-real-key-0123456789";

    @Test void localCreationDoesNotNeedKey() {
        DeploymentPolicy policy = policy(false, "", true, false, true);
        assertTrue(policy.developmentToolsEnabled());
        assertDoesNotThrow(() -> policy.requireCreationKey(null));
    }

    @Test void configuredKeyIsRequiredAndHidesDevelopmentToolsEvenLocally() {
        DeploymentPolicy policy = policy(false, TEST_KEY, true, false, true);
        assertFalse(policy.developmentToolsEnabled());
        assertThrows(ApiException.class, () -> policy.requireCreationKey(null));
        assertThrows(ApiException.class, () -> policy.requireCreationKey(TEST_KEY + "x"));
        assertDoesNotThrow(() -> policy.requireCreationKey(TEST_KEY));
    }

    @Test void deploymentRejectsMissingKeyOrDisabledSafeguards() {
        assertThrows(IllegalArgumentException.class, () -> policy(true, "", false, true, true));
        assertThrows(IllegalArgumentException.class, () -> policy(true, TEST_KEY, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> policy(true, TEST_KEY, false, false, true));
        assertThrows(IllegalArgumentException.class, () -> policy(true, TEST_KEY, false, true, false));
        assertDoesNotThrow(() -> policy(true, TEST_KEY, false, true, true));
    }

    @Test void rejectTooShortRetention() {
        assertThrows(IllegalArgumentException.class, () -> new DeploymentPolicy(false, "", true,
                false, Duration.ZERO, true));
    }

    private DeploymentPolicy policy(boolean deployment, String key, boolean development,
                                    boolean retention, boolean rateLimit) {
        return new DeploymentPolicy(deployment, key, development, retention, Duration.ofHours(24), rateLimit);
    }
}
