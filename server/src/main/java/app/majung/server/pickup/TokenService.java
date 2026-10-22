package app.majung.server.pickup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class TokenService {
    private final SecureRandom random = new SecureRandom();

    String issue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    boolean matches(String token, String storedHash) {
        if (token == null || token.length() != 43 || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    String bearer(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() != 50) {
            throw new ApiException(401, "UNAUTHORIZED", "픽업 접근 토큰이 필요합니다.");
        }
        return header.substring(7);
    }
}
