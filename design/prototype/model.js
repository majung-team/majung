export const places = {
  original: { name: "중앙역 공영주차장", detail: "정문 쪽 만남 장소" },
  alternate: { name: "중앙역 서쪽 주차장", detail: "서쪽 출입구 앞" },
};

export function initialState() {
  return {
    phase: "draft",
    paired: false,
    guestName: "",
    readiness: "waiting",
    carReadiness: "waiting",
    motion: "parked",
    connection: "online",
    place: "original",
    proposal: null,
    routing: false,
    routeError: false,
    etaSeconds: 480,
    etaValid: true,
    routeVersion: 1,
  };
}

export const isTerminal = (s) =>
  ["completed", "cancelled", "expired"].includes(s.phase);
export const isShared = (s) => ["active", "arrived"].includes(s.phase);

// Presentation state only. A real server must enforce authentication and permissions.
export function reduce(s, action) {
  if (action.type === "RESET") return initialState();
  if (action.type === "MOTION")
    return ["parked", "driving"].includes(action.value)
      ? { ...s, motion: action.value }
      : s;
  if (isTerminal(s)) return s;
  switch (action.type) {
    case "CREATE":
      return s.phase === "draft" && s.motion === "parked"
        ? { ...s, phase: "inviting" }
        : s;
    case "PAIR":
      return s.phase === "inviting" && s.motion === "parked"
        ? { ...s, paired: true }
        : s;
    case "JOIN": {
      const name = String(action.name || "")
        .trim()
        .slice(0, 12);
      return s.phase === "inviting" && s.paired && name
        ? { ...s, phase: "approval", guestName: name }
        : s;
    }
    case "APPROVE":
      return s.phase === "approval" && s.motion === "parked"
        ? { ...s, phase: "active" }
        : s;
    case "READY":
      if (!isShared(s) || !["preparing", "ready"].includes(action.value))
        return s;
      return {
        ...s,
        readiness: action.value,
        carReadiness: s.connection === "online" ? action.value : s.carReadiness,
      };
    case "CONNECTION":
      if (
        !isShared(s) ||
        !["online", "stale", "offline"].includes(action.value)
      )
        return s;
      return {
        ...s,
        connection: action.value,
        carReadiness: action.value === "online" ? s.readiness : s.carReadiness,
      };
    case "PROPOSE":
      return s.phase === "active" && !s.routing && !s.proposal
        ? { ...s, proposal: "alternate", routeError: false }
        : s;
    case "ACCEPT_PLACE":
      return s.phase === "active" &&
        s.proposal &&
        !s.routing &&
        s.motion === "parked" &&
        s.connection === "online"
        ? { ...s, routing: true, routeError: false, etaValid: false }
        : s;
    case "REJECT_PLACE":
      return s.proposal &&
        !s.routing &&
        s.motion === "parked" &&
        s.connection === "online"
        ? { ...s, proposal: null, routeError: false }
        : s;
    case "ROUTE_RESULT":
      if (!s.routing || !s.proposal) return s;
      return action.ok
        ? {
            ...s,
            place: s.proposal,
            proposal: null,
            routing: false,
            routeError: false,
            etaSeconds: 600,
            etaValid: true,
            routeVersion: s.routeVersion + 1,
          }
        : { ...s, routing: false, routeError: true };
    case "FRESH_ROUTE_INFO":
      return isShared(s) && s.connection === "online" && !s.routing
        ? { ...s, etaValid: true }
        : s;
    case "ARRIVE":
      return s.phase === "active" && !s.routing && s.connection === "online"
        ? {
            ...s,
            phase: "arrived",
            etaSeconds: 0,
            proposal: null,
            routeError: false,
          }
        : s;
    case "COMPLETE":
      return s.phase === "arrived" && s.motion === "parked"
        ? { ...s, phase: "completed", proposal: null, routing: false }
        : s;
    case "CANCEL":
      return s.phase !== "draft" && s.motion === "parked"
        ? { ...s, phase: "cancelled", proposal: null, routing: false }
        : s;
    case "EXPIRE":
      return ["inviting", "approval"].includes(s.phase)
        ? { ...s, phase: "expired" }
        : s;
    default:
      return s;
  }
}

export function guestView(s) {
  const shared = isShared(s);
  return {
    canViewLocation: shared && s.connection === "online" && !s.routing,
    etaMinutes:
      shared && s.connection === "online" && !s.routing && s.etaValid
        ? Math.ceil(s.etaSeconds / 60)
        : null,
    place: shared ? places[s.place] : null,
    readinessPending: shared && s.readiness !== s.carReadiness,
  };
}
