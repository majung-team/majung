package app.majung.server.pickup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:deployment-http;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/initial-schema.sql",
        "majung.deployment=true", "majung.creation-key=test-only-not-a-real-key-0123456789",
        "majung.development-tools=false", "majung.retention.enabled=true",
        "majung.retention.cleanup-interval-ms=3600000",
        "majung.rate-limit.requests-per-minute=30", "majung.rate-limit.writes-per-minute=12"
})
@Import(PickupApiTest.TimeConfiguration.class)
class DeploymentHttpTest {
    private static final String TEST_KEY = "test-only-not-a-real-key-0123456789";
    private static final String PLACE = "{\"place\":{\"label\":\"예시 장소\",\"latitude\":37,\"longitude\":127}}";
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    @LocalServerPort int port;
    @Autowired PickupApiTest.MutableClock clock;

    @BeforeEach void newWindow() { clock.advance(Duration.ofMinutes(2)); }

    @Test void creationKeyIsRequiredAndNeverReturnedInConfiguration() throws Exception {
        assertEquals(403, call("POST", "/api/v1/pickups", null, null, PLACE, null).statusCode());
        assertEquals(403, call("POST", "/api/v1/pickups", "wrong", null, PLACE, null).statusCode());
        assertEquals(201, call("POST", "/api/v1/pickups", TEST_KEY, null, PLACE, null).statusCode());
        HttpResponse<String> configuration = call("GET", "/api/v1/config", null, null, null, null);
        assertEquals(200, configuration.statusCode());
        assertEquals("{\"developmentToolsEnabled\":false}", configuration.body());
        assertFalse(configuration.body().contains(TEST_KEY));
    }

    @Test void pickupTokensStillAllowPassengerFlowWithoutCreationKey() throws Exception {
        JsonNode created = json.readTree(call("POST", "/api/v1/pickups", TEST_KEY, null, PLACE, null).body());
        String path = "/api/v1/pickups/" + created.path("pickupId").asText();
        String owner = created.path("driverToken").asText();
        JsonNode joined = json.readTree(call("POST", path + "/participants", null, null,
                "{\"displayName\":\"테스트\",\"inviteToken\":\"" + created.path("inviteToken").asText() + "\"}", null).body());
        String guest = joined.path("participantToken").asText();
        JsonNode waiting = json.readTree(call("GET", path, null, guest, null, null).body());
        assertTrue(waiting.path("place").isNull());
        assertEquals(200, call("POST", path + "/participants/" + joined.path("participantId").asText()
                + "/approval", null, owner, null, null).statusCode());
        assertEquals(200, call("PUT", path + "/readiness", null, guest,
                "{\"readiness\":\"READY\",\"sequence\":1}", null).statusCode());
        assertEquals("READY", json.readTree(call("GET", path, null, owner, null, null).body())
                .path("participant").path("readiness").asText());
    }

    @Test void forgedForwardedAddressesCannotBypassWriteLimit() throws Exception {
        for (int i = 0; i < 12; i++) {
            assertEquals(403, call("POST", "/api/v1/pickups", null, null, PLACE, "192.0.2." + i).statusCode());
        }
        HttpResponse<String> rejected = call("POST", "/api/v1/pickups", TEST_KEY, null, PLACE, "198.51.100.1");
        assertEquals(429, rejected.statusCode());
        assertEquals("60", rejected.headers().firstValue("Retry-After").orElseThrow());
        assertEquals("no-store", rejected.headers().firstValue("Cache-Control").orElseThrow());
        JsonNode body = json.readTree(rejected.body());
        assertEquals("RATE_LIMITED", body.path("code").asText());
        assertFalse(body.path("requestId").asText().isBlank());
        assertEquals(200, call("GET", "/api/v1/config", null, null, null, null).statusCode());
    }

    private HttpResponse<String> call(String method, String path, String key, String token,
                                      String body, String forwarded) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json");
        if (key != null) builder.header("X-Majung-Creation-Key", key);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (forwarded != null) {
            builder.header("X-Forwarded-For", forwarded);
            builder.header("Forwarded", "for=" + forwarded);
        }
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
