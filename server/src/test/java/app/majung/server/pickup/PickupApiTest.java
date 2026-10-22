package app.majung.server.pickup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:pickup-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PickupApiTest {
    @LocalServerPort
    int port;
    @Autowired
    MutableClock clock;
    @Autowired
    JdbcTemplate jdbcTemplate;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now());

        void reset() { now.set(Instant.parse("2026-10-23T03:00:00Z")); }
        void advance(Duration duration) { now.updateAndGet(value -> value.plus(duration)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    @BeforeEach
    void resetTime() {
        clock.reset();
    }

    @Test
    void completePickupFlowAndRemoveAccessAfterEnd() throws Exception {
        Session session = create();
        Guest guest = join(session);
        Response waiting = call("GET", session.path(), guest.token(), null);
        assertEquals("WAITING_APPROVAL", waiting.body().path("status").asText());
        assertTrue(waiting.body().path("place").isNull());
        assertError(call("GET", session.path() + "/vehicle-state", guest.token(), null), 403, "APPROVAL_REQUIRED");
        assertEquals(200, approve(session, guest).status());
        assertEquals(200, vehicle(session, 1, 300, clock.instant()).status());
        Response shared = call("GET", session.path() + "/vehicle-state", guest.token(), null);
        assertEquals("FRESH", shared.body().path("freshness").asText());
        assertEquals(300, shared.body().path("durationSeconds").asInt());
        assertEquals(30000, shared.body().path("validForMillis").asLong());
        assertEquals("no-store", shared.cacheControl());
        assertEquals(200, ready(session, guest, "READY", 1).status());
        assertEquals("READY", call("GET", session.path(), session.owner(), null)
                .body().path("participant").path("readiness").asText());
        assertError(call("POST", session.path() + "/completion", session.owner(), null), 409, "ARRIVAL_REQUIRED");
        assertEquals(200, call("POST", session.path() + "/arrival", session.owner(), null).status());
        assertEquals(200, call("GET", session.path() + "/vehicle-state", guest.token(), null).status());
        assertEquals("COMPLETED", call("POST", session.path() + "/completion", session.owner(), null)
                .body().path("status").asText());
        assertError(call("GET", session.path() + "/vehicle-state", guest.token(), null), 410, "PICKUP_ENDED");
        assertError(ready(session, guest, "PREPARING", 2), 410, "PICKUP_ENDED");
        assertError(vehicle(session, 2, 200, clock.instant()), 410, "PICKUP_ENDED");
        Response ended = call("GET", session.path(), guest.token(), null);
        assertTrue(ended.body().path("place").isNull());
        assertEquals(200, call("POST", session.path() + "/completion", session.owner(), null).status());
        assertNull(jdbcTemplate.queryForObject("select vehicle_latitude from pickup where id = ?", Double.class, session.id()));
    }

    @Test
    void enforceRoleAndPickupBoundaries() throws Exception {
        Session session = create();
        Session other = create();
        Guest guest = join(session);
        assertError(call("GET", session.path(), null, null), 401, "UNAUTHORIZED");
        assertError(call("GET", session.path(), other.owner(), null), 403, "FORBIDDEN");
        assertError(call("POST", session.path() + "/participants/" + guest.id() + "/approval", guest.token(), null), 403, "FORBIDDEN");
        assertError(call("POST", session.path() + "/cancellation", guest.token(), null), 403, "FORBIDDEN");
        approve(session, guest);
        assertError(call("PUT", session.path() + "/vehicle-state", guest.token(), Map.of(
                "latitude", 37.01, "longitude", 127.01, "durationSeconds", 300,
                "sourceTime", clock.instant().toString(), "sequence", 1)), 403, "FORBIDDEN");
        assertError(call("PUT", session.path() + "/readiness", session.owner(), Map.of("readiness", "READY", "sequence", 1)), 403, "FORBIDDEN");
        assertError(call("GET", other.path() + "/vehicle-state", guest.token(), null), 403, "FORBIDDEN");
    }

    @Test
    void invitationIsSingleUseAndHashesAreNotReturnedOrStoredAsRawTokens() throws Exception {
        Session session = create();
        Guest guest = join(session);
        assertError(joinRequest(session, session.invite()), 410, "INVITE_UNAVAILABLE");
        String ownerHash = jdbcTemplate.queryForObject("select owner_hash from pickup where id = ?", String.class, session.id());
        String participantHash = jdbcTemplate.queryForObject("select participant_hash from pickup where id = ?", String.class, session.id());
        assertEquals(64, ownerHash.length());
        assertNotEquals(session.owner(), ownerHash);
        assertNotEquals(guest.token(), participantHash);
        assertFalse(call("GET", session.path(), session.owner(), null).body().toString().contains(ownerHash));
    }

    @Test
    void rejectExpiredInvitationAndPendingApprovalAtExactBoundary() throws Exception {
        Session unused = create();
        Session pending = create();
        Guest guest = join(pending);
        clock.advance(Duration.ofMinutes(15));
        assertError(joinRequest(unused, unused.invite()), 410, "INVITE_UNAVAILABLE");
        assertError(approve(pending, guest), 410, "INVITE_EXPIRED");
        assertEquals("INVITE_EXPIRED", call("GET", pending.path(), guest.token(), null).body().path("status").asText());
    }

    @Test
    void rotationRevokesOldInviteAndPendingParticipant() throws Exception {
        Session session = create();
        Guest guest = join(session);
        Response rotated = call("POST", session.path() + "/invitations", session.owner(), null);
        assertEquals(200, rotated.status());
        assertError(call("GET", session.path(), guest.token(), null), 403, "FORBIDDEN");
        assertError(joinRequest(session, session.invite()), 403, "FORBIDDEN");
        assertEquals(201, joinRequest(session, rotated.body().path("inviteToken").asText()).status());
    }

    @Test
    void pickupExpiryHidesPlaceAndRejectsReadsAndWrites() throws Exception {
        Session session = create();
        Guest guest = join(session);
        approve(session, guest);
        vehicle(session, 1, 600, clock.instant());
        clock.advance(Duration.ofHours(2));
        Response expired = call("GET", session.path(), guest.token(), null);
        assertEquals("EXPIRED", expired.body().path("status").asText());
        assertTrue(expired.body().path("place").isNull());
        assertError(call("GET", session.path() + "/vehicle-state", guest.token(), null), 410, "PICKUP_ENDED");
        assertError(ready(session, guest, "READY", 1), 410, "PICKUP_ENDED");
        assertError(vehicle(session, 2, 300, clock.instant()), 410, "PICKUP_ENDED");
    }

    @Test
    void readinessIsIdempotentAndOlderEventsCannotRollItBack() throws Exception {
        Session session = create();
        Guest guest = join(session);
        approve(session, guest);
        assertEquals(200, ready(session, guest, "READY", 2).status());
        assertEquals(200, ready(session, guest, "READY", 2).status());
        assertError(ready(session, guest, "PREPARING", 1), 409, "STALE_EVENT");
        assertError(ready(session, guest, "PREPARING", 2), 409, "STALE_EVENT");
        assertEquals("READY", call("GET", session.path(), session.owner(), null).body()
                .path("participant").path("readiness").asText());
    }

    @Test
    void hideStaleTelemetryAndDoNotRefreshItThroughReplay() throws Exception {
        Session session = create();
        Guest guest = join(session);
        approve(session, guest);
        Instant source = clock.instant();
        assertEquals("WAITING", call("GET", session.path() + "/vehicle-state", guest.token(), null).body().path("freshness").asText());
        vehicle(session, 2, 300, source);
        assertError(vehicle(session, 1, 600, source), 409, "STALE_EVENT");
        clock.advance(Duration.ofSeconds(30));
        Response replay = vehicle(session, 2, 300, source);
        assertEquals(200, replay.status());
        assertEquals("STALE", replay.body().path("freshness").asText());
        assertEquals(0, replay.body().path("validForMillis").asLong());
        assertTrue(replay.body().path("latitude").isNull());
        assertTrue(replay.body().path("durationSeconds").isNull());
        assertError(vehicle(session, 3, 200, source.minusSeconds(1)), 409, "STALE_EVENT");
        assertError(vehicle(session, 3, 200, clock.instant().plusSeconds(1)), 400, "FUTURE_SOURCE_TIME");
        assertEquals("FRESH", vehicle(session, 3, 200, clock.instant()).body().path("freshness").asText());
    }

    @Test
    void invalidationKeepsSequenceAndRejectsLateTelemetry() throws Exception {
        Session session = create();
        Guest guest = join(session);
        assertError(call("DELETE", session.path() + "/vehicle-state", session.owner(), Map.of("sequence", 1)), 403, "APPROVAL_REQUIRED");
        approve(session, guest);
        vehicle(session, 1, 300, clock.instant());
        assertError(call("DELETE", session.path() + "/vehicle-state", guest.token(), Map.of("sequence", 2)), 403, "FORBIDDEN");
        Response cleared = call("DELETE", session.path() + "/vehicle-state", session.owner(), Map.of("sequence", 2));
        assertEquals(200, cleared.status());
        assertEquals(2, cleared.body().path("sequence").asLong());
        assertEquals(0, cleared.body().path("validForMillis").asLong());
        assertTrue(cleared.body().path("latitude").isNull());
        assertTrue(cleared.body().path("durationSeconds").isNull());
        assertEquals(200, call("DELETE", session.path() + "/vehicle-state", session.owner(), Map.of("sequence", 2)).status());
        assertError(vehicle(session, 1, 300, clock.instant()), 409, "STALE_EVENT");
        vehicle(session, 3, 200, clock.instant());
        assertError(call("DELETE", session.path() + "/vehicle-state", session.owner(), Map.of("sequence", 2)), 409, "STALE_EVENT");
        assertEquals("FRESH", call("GET", session.path() + "/vehicle-state", guest.token(), null).body().path("freshness").asText());
    }

    @Test
    void cancellationStopsTelemetryAndCannotBeReopened() throws Exception {
        Session session = create();
        Guest guest = join(session);
        approve(session, guest);
        vehicle(session, 1, 300, clock.instant());
        assertEquals(200, call("POST", session.path() + "/cancellation", session.owner(), null).status());
        assertEquals(200, call("POST", session.path() + "/cancellation", session.owner(), null).status());
        assertError(approve(session, guest), 410, "PICKUP_ENDED");
        assertError(call("POST", session.path() + "/arrival", session.owner(), null), 410, "PICKUP_ENDED");
        assertError(call("GET", session.path() + "/vehicle-state", guest.token(), null), 410, "PICKUP_ENDED");
        assertError(call("POST", session.path() + "/invitations", session.owner(), null), 410, "PICKUP_ENDED");
    }

    @Test
    void concurrentJoinsConsumeOneInvitationOnly() throws Exception {
        Session session = create();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Response> attempt = () -> joinRequest(session, session.invite());
            var results = executor.invokeAll(List.of(attempt, attempt));
            List<Integer> statuses = List.of(results.get(0).get().status(), results.get(1).get().status());
            assertEquals(1, statuses.stream().filter(status -> status == 201).count());
            assertEquals(1, statuses.stream().filter(status -> status == 410).count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentReadinessUpdatesKeepHighestSequence() throws Exception {
        Session session = create();
        Guest guest = join(session);
        approve(session, guest);
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Response>> updates = List.of(
                    () -> ready(session, guest, "PREPARING", 1),
                    () -> ready(session, guest, "READY", 2));
            var results = executor.invokeAll(updates);
            for (var result : results) {
                assertTrue(List.of(200, 409).contains(result.get().status()));
            }
            assertEquals(2, call("GET", session.path(), session.owner(), null).body()
                    .path("participant").path("readinessSequence").asInt());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCancellationAndTelemetryCannotRestoreLocation() throws Exception {
        Session session = create();
        Guest guest = join(session);
        approve(session, guest);
        vehicle(session, 1, 300, clock.instant());
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Response>> changes = List.of(
                    () -> call("POST", session.path() + "/cancellation", session.owner(), null),
                    () -> vehicle(session, 2, 200, clock.instant()));
            var results = executor.invokeAll(changes);
            assertEquals(200, results.get(0).get().status());
            assertTrue(List.of(200, 410).contains(results.get(1).get().status()));
            assertNull(jdbcTemplate.queryForObject("select vehicle_latitude from pickup where id = ?", Double.class, session.id()));
            assertError(call("GET", session.path() + "/vehicle-state", guest.token(), null), 410, "PICKUP_ENDED");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invalidInputHasConsistentErrorAndRequestId() throws Exception {
        assertError(call("POST", "/api/v1/pickups", null, Map.of("place",
                Map.of("label", " ", "latitude", 200, "longitude", 0))), 400, "INVALID_REQUEST");
        assertError(call("GET", "/api/v1/pickups/not-a-uuid", null, null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/api/v1/unknown", null, null), 404, "INVALID_REQUEST");
        Session session = create();
        assertError(call("DELETE", session.path(), session.owner(), null), 405, "INVALID_REQUEST");
        assertError(call("POST", session.path() + "/participants", null,
                Map.of("inviteToken", session.invite(), "displayName", " ")), 400, "INVALID_REQUEST");
    }

    private Session create() throws Exception {
        Response response = call("POST", "/api/v1/pickups", null,
                Map.of("place", Map.of("label", "예시 만남 장소", "latitude", 37.0, "longitude", 127.0)));
        assertEquals(201, response.status(), response.body().toString());
        return new Session(response.body().path("pickupId").asText(), response.body().path("driverToken").asText(),
                response.body().path("inviteToken").asText());
    }

    private Response joinRequest(Session session, String token) throws Exception {
        return call("POST", session.path() + "/participants", null, Map.of("inviteToken", token, "displayName", "테스트"));
    }

    private Guest join(Session session) throws Exception {
        Response response = joinRequest(session, session.invite());
        assertEquals(201, response.status());
        return new Guest(response.body().path("participantId").asText(), response.body().path("participantToken").asText());
    }

    private Response approve(Session session, Guest guest) throws Exception {
        return call("POST", session.path() + "/participants/" + guest.id() + "/approval", session.owner(), null);
    }

    private Response ready(Session session, Guest guest, String readiness, long sequence) throws Exception {
        return call("PUT", session.path() + "/readiness", guest.token(), Map.of("readiness", readiness, "sequence", sequence));
    }

    private Response vehicle(Session session, long sequence, long eta, Instant source) throws Exception {
        return call("PUT", session.path() + "/vehicle-state", session.owner(), Map.of("latitude", 37.01,
                "longitude", 127.01, "durationSeconds", eta, "sourceTime", source.toString(), "sequence", sequence));
    }

    private Response call(String method, String path, String token, Object body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json");
        if (token != null) { builder.header("Authorization", "Bearer " + token); }
        if (body != null) { builder.header("Content-Type", "application/json"); }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), json.readTree(response.body()),
                response.headers().firstValue("Cache-Control").orElse(""));
    }

    private void assertError(Response response, int status, String code) {
        assertEquals(status, response.status(), response.body().toString());
        assertEquals(code, response.body().path("code").asText());
        assertFalse(response.body().path("requestId").asText().isBlank());
        assertFalse(response.body().has("trace"));
    }

    record Response(int status, JsonNode body, String cacheControl) { }
    record Session(String id, String owner, String invite) {
        String path() { return "/api/v1/pickups/" + id; }
    }
    record Guest(String id, String token) { }
}
