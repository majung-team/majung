export const terminalStatuses = new Set(["COMPLETED", "CANCELLED", "EXPIRED"]);
const idPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const tokenPattern = /^[A-Za-z0-9_-]{43}$/;

export function parseInvitation(hash) {
  const parts = hash.replace(/^#/, "").split("/");
  if (
    parts.length !== 3 ||
    parts[0] !== "join" ||
    !idPattern.test(parts[1]) ||
    !tokenPattern.test(parts[2])
  )
    return null;
  return { pickupId: parts[1], inviteToken: parts[2] };
}

export function validSession(value) {
  return Boolean(
    value &&
      ["driver", "guest"].includes(value.role) &&
      idPattern.test(value.pickupId) &&
      tokenPattern.test(value.token) &&
      (value.role !== "guest" || idPattern.test(value.participantId)),
  );
}

export function invitationUrl(origin, pickupId, token) {
  return `${origin}/#join/${pickupId}/${token}`;
}

export function nextSequence(snapshot) {
  return (snapshot?.participant?.readinessSequence ?? 0) + 1;
}

export function vehicleDeadline(vehicle, requestStarted, responseReceived) {
  // Subtract the entire round trip; do not trust the phone's wall clock.
  const lifetime = Number(vehicle?.validForMillis);
  if (
    vehicle?.freshness !== "FRESH" ||
    !Number.isFinite(lifetime) ||
    lifetime <= 0
  )
    return 0;
  return (
    responseReceived +
    Math.max(0, lifetime - (responseReceived - requestStarted))
  );
}

export function visibleVehicle(vehicle, deadline, now, online, status) {
  if (
    !online ||
    !["ACTIVE", "ARRIVED"].includes(status) ||
    deadline <= now ||
    vehicle?.freshness !== "FRESH"
  )
    return null;
  if (
    !Number.isFinite(vehicle.latitude) ||
    !Number.isFinite(vehicle.longitude) ||
    !Number.isFinite(vehicle.durationSeconds) ||
    vehicle.durationSeconds < 0
  )
    return null;
  return vehicle;
}

export function escapeHtml(value) {
  return String(value ?? "").replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ],
  );
}
