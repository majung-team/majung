import test from "node:test";
import assert from "node:assert/strict";
import {
  parseInvitation,
  validSession,
  invitationUrl,
  vehicleDeadline,
  visibleVehicle,
  nextSequence,
  escapeHtml,
} from "./model.js";
const id = "12345678-1234-1234-1234-123456789abc";
const token = "x".repeat(43);
const vehicle = {
  freshness: "FRESH",
  latitude: 37,
  longitude: 127,
  durationSeconds: 120,
  validForMillis: 30000,
};
test("초대는 fragment의 UUID와 43자리 토큰만 허용", () => {
  assert.deepEqual(parseInvitation(`#join/${id}/${token}`), {
    pickupId: id,
    inviteToken: token,
  });
  for (const hash of [
    `#join/${id}/short`,
    `#join/../${token}`,
    `#join/${id}/${token}/extra`,
    "#main",
  ])
    assert.equal(parseInvitation(hash), null);
  assert.equal(
    new URL(invitationUrl("https://example.test", id, token)).search,
    "",
  );
});
test("탑승자 세션은 참여자 ID까지 있어야 복원", () => {
  assert.equal(validSession({ role: "driver", pickupId: id, token }), true);
  assert.equal(validSession({ role: "guest", pickupId: id, token }), false);
  assert.equal(
    validSession({ role: "guest", pickupId: id, token, participantId: id }),
    true,
  );
  assert.equal(validSession({ role: "admin", pickupId: id, token }), false);
});
test("정보 유효 시간은 서버 잔여 시간에서 왕복 시간을 차감", () => {
  assert.equal(vehicleDeadline(vehicle, 100, 500), 30100);
  assert.equal(
    vehicleDeadline({ ...vehicle, validForMillis: 50 }, 100, 500),
    500,
  );
  assert.equal(
    vehicleDeadline({ ...vehicle, freshness: "STALE" }, 100, 500),
    0,
  );
  assert.equal(
    vehicleDeadline({ ...vehicle, validForMillis: undefined }, 100, 500),
    0,
  );
});
test("승인 전·연결 끊김·만료·종료 때 이전 위치를 노출하지 않음", () => {
  assert.equal(visibleVehicle(vehicle, 1000, 999, true, "ACTIVE"), vehicle);
  for (const args of [
    [1000, 1000, true, "ACTIVE"],
    [1000, 900, false, "ACTIVE"],
    [1000, 900, true, "WAITING_APPROVAL"],
    [1000, 900, true, "COMPLETED"],
  ])
    assert.equal(visibleVehicle(vehicle, ...args), null);
  assert.equal(
    visibleVehicle({ ...vehicle, latitude: null }, 1000, 900, true, "ACTIVE"),
    null,
  );
});
test("준비 상태 순서와 사용자 입력 이스케이프", () => {
  assert.equal(nextSequence({ participant: { readinessSequence: 4 } }), 5);
  assert.equal(
    escapeHtml('<img onerror="x">&'),
    "&lt;img onerror=&quot;x&quot;&gt;&amp;",
  );
});
