import test from "node:test";
import assert from "node:assert/strict";
import { initialState, reduce, guestView } from "./model.js";

const perform = (state, ...actions) => actions.reduce(reduce, state);
const active = () =>
  perform(
    initialState(),
    { type: "CREATE" },
    { type: "PAIR" },
    { type: "JOIN", name: "지민" },
    { type: "APPROVE" },
  );

test("opening an invitation does not expose vehicle location before driver approval", () => {
  let s = perform(initialState(), { type: "CREATE" }, { type: "PAIR" });
  assert.equal(guestView(s).canViewLocation, false);
  s = reduce(s, { type: "JOIN", name: "지민" });
  assert.equal(guestView(s).canViewLocation, false);
  s = reduce(s, { type: "APPROVE" });
  assert.equal(guestView(s).canViewLocation, true);
});

test("driver cannot create, approve or change a place while driving", () => {
  let s = reduce(initialState(), { type: "MOTION", value: "driving" });
  assert.equal(reduce(s, { type: "CREATE" }).phase, "draft");
  s = perform(
    active(),
    { type: "MOTION", value: "driving" },
    { type: "PROPOSE" },
  );
  assert.equal(reduce(s, { type: "ACCEPT_PLACE" }).routing, false);
});

test("failed route change preserves the confirmed place and does not bump its version", () => {
  let s = perform(active(), { type: "PROPOSE" }, { type: "ACCEPT_PLACE" });
  assert.equal(guestView(s).etaMinutes, null);
  s = reduce(s, { type: "ROUTE_RESULT", ok: false });
  assert.equal(s.place, "original");
  assert.equal(s.routeVersion, 1);
  assert.equal(s.routeError, true);
  assert.equal(guestView(s).etaMinutes, null);
});

test("a route failure requires fresh information before displaying the old route ETA", () => {
  let s = perform(
    active(),
    { type: "PROPOSE" },
    { type: "ACCEPT_PLACE" },
    { type: "ROUTE_RESULT", ok: false },
  );
  assert.equal(guestView(s).etaMinutes, null);
  s = reduce(s, { type: "FRESH_ROUTE_INFO" });
  assert.equal(guestView(s).etaMinutes, 8);
  assert.equal(s.place, "original");
});

test("new place becomes confirmed only after route success", () => {
  let s = perform(active(), { type: "PROPOSE" }, { type: "ACCEPT_PLACE" });
  assert.equal(s.place, "original");
  s = reduce(s, { type: "ROUTE_RESULT", ok: true });
  assert.equal(s.place, "alternate");
  assert.equal(s.routeVersion, 2);
});

test("offline readiness is pending for the car, then reconciles on reconnect", () => {
  let s = perform(
    active(),
    { type: "CONNECTION", value: "offline" },
    { type: "READY", value: "ready" },
  );
  assert.equal(guestView(s).canViewLocation, false);
  assert.equal(guestView(s).readinessPending, true);
  assert.equal(s.carReadiness, "waiting");
  s = reduce(s, { type: "CONNECTION", value: "online" });
  assert.equal(s.carReadiness, "ready");
  assert.equal(guestView(s).readinessPending, false);
});

test("arrival is not completion; completing removes guest access and blocks late updates", () => {
  let s = reduce(active(), { type: "ARRIVE" });
  assert.equal(s.phase, "arrived");
  assert.equal(guestView(s).canViewLocation, true);
  s = reduce(s, { type: "COMPLETE" });
  assert.equal(guestView(s).canViewLocation, false);
  assert.equal(guestView(s).place, null);
  assert.deepEqual(reduce(s, { type: "READY", value: "ready" }), s);
});

test("expired invitation cannot be used to join or gain location access", () => {
  const s = perform(
    initialState(),
    { type: "CREATE" },
    { type: "PAIR" },
    { type: "EXPIRE" },
  );
  assert.equal(reduce(s, { type: "JOIN", name: "다른 사람" }).phase, "expired");
  assert.equal(guestView(s).canViewLocation, false);
});
