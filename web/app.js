import { request, ApiError } from "./api.js";
import {
  parseInvitation,
  validSession,
  invitationUrl,
  nextSequence,
  vehicleDeadline,
  visibleVehicle,
  terminalStatuses,
  escapeHtml as e,
} from "./model.js";

const root = document.querySelector("#app");
const storageKey = "majung.pickup.session.v1";
let invitation = parseInvitation(location.hash);
const invalidInvitation = Boolean(
  location.hash.startsWith("#join") && !invitation,
);
// Remove the fragment before loading or requesting anything else.
if (location.hash) history.replaceState(null, "", location.pathname);
let session = null;
let storageAvailable = true;
try {
  const restored = JSON.parse(sessionStorage.getItem(storageKey) || "null");
  if (!invitation && validSession(restored)) session = restored;
} catch {
  storageAvailable = false;
}
let snapshot = null;
let vehicle = null;
let deadline = 0;
let online = false;
let busy = false;
let refreshing = false;
let epoch = 0;
let page = invitation || invalidInvitation ? "join" : "home";
let message = invalidInvitation
  ? "초대 링크를 확인해주세요. 받은 링크를 다시 열어주세요."
  : "";
let notice = "";
let finished = null;
let pendingReadiness = null;
let manualLink = false;
let showedVehicle = false;
let renderedMarkup = "";
const draft = { name: "", label: "", latitude: "", longitude: "" };
const readinessText = {
  WAITING: "아직 준비 중이에요",
  PREPARING: "출발을 준비하고 있어요",
  READY: "준비가 끝났어요",
};

function storeSession() {
  try {
    if (session) sessionStorage.setItem(storageKey, JSON.stringify(session));
    else sessionStorage.removeItem(storageKey);
  } catch {
    storageAvailable = false;
  }
}

function forgetSession() {
  epoch += 1;
  session = null;
  snapshot = null;
  vehicle = null;
  deadline = 0;
  pendingReadiness = null;
  online = false;
  manualLink = false;
  storeSession();
}

function finish(data) {
  finished = { role: session.role, status: data.status };
  forgetSession();
  invitation = null;
  message = "";
  notice = "";
}

const button = (text, action, quiet = false, disabled = false) =>
  `<button type="button" data-action="${action}" class="${quiet ? "quiet" : "primary"}" ${busy || disabled ? "disabled" : ""}>${text}</button>`;

function alerts() {
  return `${message ? `<div class="alert" role="alert">${e(message)}</div>` : ""}
    ${notice ? `<p class="notice" role="status">${e(notice)}</p>` : ""}
    ${!storageAvailable ? '<p class="hint">이 브라우저에서는 세션을 저장할 수 없어요. 새로고침하면 다시 초대가 필요해요.</p>' : ""}`;
}

function home() {
  return `<section class="intro"><p class="eyebrow">만나는 순간까지, 연락은 간단하게.</p>
    <h1>기다림을<br />같은 약속으로.</h1><p>초대 링크를 열면 만날 장소와 도착 정보를 확인하고,<br class="desktop" /> 준비 상태를 운전자에게 전할 수 있어요.</p></section>
    <section class="card"><span class="badge">탑승자 안내</span><h2>초대 링크로 참여해요.</h2>
    <p>운전자에게 받은 링크를 열어주세요.<br />참여 요청을 확인한 뒤 위치 공유가 시작돼요.</p></section>
    <section class="dev-card"><h2>운전자 확인 화면</h2><p>현재는 차량 앱 연결을 준비하는 개발 단계예요.<br />이 브라우저에서 약속을 만들고 탑승자 참여를 확인할 수 있어요.</p>
    ${button("픽업 준비하기", "prepare", true)}</section>`;
}

function createForm() {
  return `<section class="intro small"><p class="eyebrow">운전자 · 개발 확인용</p><h1>만날 장소를<br />정해주세요.</h1><p>개발 확인을 위한 임시 입력 화면이에요.<br />차량 앱에서는 내비게이션에서 선택한 장소로 약속을 만들어요.</p></section>
    <form id="create-form" class="card"><label for="place-label">만날 장소<input id="place-label" name="label" required maxlength="120" value="${e(draft.label)}" placeholder="예: 역 앞 공영주차장" /></label>
    <div class="input-pair"><label for="latitude">위도<input id="latitude" name="latitude" type="number" min="-90" max="90" step="any" required value="${e(draft.latitude)}" placeholder="37.0" /></label>
    <label for="longitude">경도<input id="longitude" name="longitude" type="number" min="-180" max="180" step="any" required value="${e(draft.longitude)}" placeholder="127.0" /></label></div>
    <p class="hint">시연할 때는 실제 위치 대신 예시 좌표를 사용해주세요.</p>
    <button class="primary" ${busy ? "disabled" : ""}>${busy ? "약속을 만들고 있어요…" : "약속 만들기"}</button></form>
    ${button("돌아가기", "home", true)}`;
}

function joinForm() {
  if (!invitation)
    return `<section class="card"><h1>초대가 필요해요.</h1><p>운전자에게 받은 링크를 다시 열어주세요.</p>${button("처음으로", "home", true)}</section>`;
  return `<section class="intro small"><p class="eyebrow">마중 약속 초대</p><h1>함께 만날 약속에<br />참여할까요?</h1><p>운전자가 확인하면 만날 장소와<br />차량의 도착 정보를 볼 수 있어요.</p></section>
    <form id="join-form" class="card"><label for="display-name">운전자가 알아볼 이름<input id="display-name" name="name" required maxlength="12" autocomplete="nickname" value="${e(draft.name)}" placeholder="이름 또는 별명" /></label>
    <p class="hint">휴대전화 위치나 연락처는 요청하지 않아요.</p>
    <button class="primary" ${busy ? "disabled" : ""}>${busy ? "참여를 요청하고 있어요…" : "약속에 참여하기"}</button></form>`;
}

function placeCard() {
  return snapshot?.place
    ? `<div class="place"><span>함께 만날 장소</span><strong>${e(snapshot.place.label)}</strong></div>`
    : "";
}

function connection() {
  return `<div class="connection ${online ? "connected" : "disconnected"}"><span class="dot"></span>${online ? "서버에서 상태를 확인했어요" : "연결을 확인하고 있어요"}
    ${button("다시 확인", "refresh", true)}</div>`;
}

function invitationCard() {
  const invite = session.invite;
  if (!invite)
    return `<p class="hint">이 탭에는 초대 링크가 남아 있지 않아요. 새로 초대해주세요.</p>`;
  const url = invitationUrl(location.origin, session.pickupId, invite.token);
  return `<div class="invite-card"><h2>기다리는 사람을 초대해요.</h2><p>링크로 참여한 뒤 운전자 확인이 필요해요.</p>
    <div class="button-pair">${button("초대 링크 복사", "copy-invite")}
    <a class="button quiet" href="${e(url)}" target="_blank" rel="noopener noreferrer">초대 화면 열기</a></div>
    ${manualLink ? `<label for="invite-url">길게 눌러 링크 복사<input id="invite-url" readonly value="${e(url)}" /></label>` : ""}
    <p class="hint">한 번만 참여할 수 있어요. 이 링크를 가진 사람에게만 전달해주세요.</p></div>`;
}

function driver() {
  const status = snapshot.status;
  const guest = snapshot.participant;
  let content = "";
  if (status === "INVITING")
    content = `<h1>참여를 기다리고 있어요.</h1>${placeCard()}${invitationCard()}`;
  if (status === "WAITING_APPROVAL")
    content = `<span class="badge">참여 요청</span><h1>${e(guest.displayName)} 님이<br />참여했어요.</h1><p>초대한 사람이 맞는지 확인해주세요.<br />확인하면 이번 픽업 동안 위치를 공유해요.</p>${placeCard()}${button("확인하고 공유 시작", "approve", false, !online)}`;
  if (status === "INVITE_EXPIRED")
    content = `<span class="badge">초대 만료</span><h1>다시 초대해주세요.</h1><p>참여·승인 기한이 지났어요.<br />새 초대를 만들면 이전 링크와 참여 요청은 취소돼요.</p>${placeCard()}`;
  if (["ACTIVE", "ARRIVED"].includes(status))
    content = `<span class="badge">${status === "ARRIVED" ? "도착 안내" : "약속 진행 중"}</span>
    <h1>${status === "ARRIVED" ? "함께 만났나요?" : "출발 준비를<br />확인해요."}</h1>
    <div class="readiness"><span>${e(guest.displayName)} 님</span><strong>${e(readinessText[guest.readiness])}</strong><small>${online ? "서버에 저장된 준비 상태예요." : "마지막으로 확인한 준비 상태예요."}</small></div>${placeCard()}
    <p class="hint">이 웹에서 만든 픽업에는 차량 앱이 연결되어 있지 않아요.</p>
    ${button(status === "ARRIVED" ? "탑승 확인 · 픽업 완료" : "도착 알리기", status === "ARRIVED" ? "complete" : "arrive", false, !online)}`;
  return `${connection()}<section class="card driver"><p class="eyebrow">운전자 확인 화면</p>${content}
    ${["INVITING", "WAITING_APPROVAL", "INVITE_EXPIRED"].includes(status) ? button("새 초대 만들기", "rotate", true, !online) : ""}
    ${button("픽업 취소", "cancel", true, !online)}</section>
    <p class="hint center">이 화면은 개발 확인용이에요. 운전 중에는 조작하지 마세요.</p>`;
}

function guest() {
  const status = snapshot.status;
  if (["WAITING_APPROVAL", "INVITE_EXPIRED"].includes(status))
    return `${connection()}<section class="card"><span class="badge">${status === "INVITE_EXPIRED" ? "참여 기한 만료" : "참여 요청 완료"}</span>
    <h1>${status === "INVITE_EXPIRED" ? "새 초대를<br />받아주세요." : "운전자 확인을<br />기다리고 있어요."}</h1><p>${status === "INVITE_EXPIRED" ? "운전자에게 새 링크를 요청해주세요." : "확인되면 이 화면에서<br />약속 장소와 도착 정보를 볼 수 있어요."}</p>
    <div class="empty-state">아직 차량 위치를 공유하지 않아요.</div></section>`;
  const current = visibleVehicle(
    vehicle,
    deadline,
    performance.now(),
    online,
    status,
  );
  const eta = current
    ? `${Math.max(1, Math.ceil(current.durationSeconds / 60))}`
    : null;
  return `${connection()}<section class="card"><span class="badge">${status === "ARRIVED" ? "도착 안내" : "약속 진행 중"}</span>
    <h1>${status === "ARRIVED" ? "차량이 도착했어요." : "만나는 시간을<br />맞춰볼까요?"}</h1>
    ${status === "ARRIVED" ? "<p>확정된 약속 장소에서 만나세요.<br />탑승 확인 후 공유가 종료돼요.</p>" : ""}
    <div class="arrival" id="arrival-info">${
      !online
        ? "<h2>연결을 확인하고 있어요.</h2><p>지난 위치와 도착 시간을 숨겼어요.<br />연결이 돌아오면 다시 확인할게요.</p>"
        : current
          ? `<span>도착까지</span><strong>약 ${eta}<small>분</small></strong><p>차량 정보 수신 시각 ${e(new Date(current.receivedAt).toLocaleTimeString("ko-KR"))}</p><div class="coordinates">차량 위치 · 위도 ${e(current.latitude.toFixed(5))}<br />경도 ${e(current.longitude.toFixed(5))}</div>`
          : `<h2>${vehicle?.freshness === "WAITING" ? "차량 정보를 기다리고 있어요." : "최신 정보를 확인하고 있어요."}</h2><p>${vehicle?.freshness === "WAITING" ? "차량에서 정보를 보내면<br />도착 예정 시간을 확인할 수 있어요." : "지난 위치와 도착 시간은 표시하지 않아요."}</p>`
    }</div>
    ${placeCard()}<div class="readiness"><strong>${e(readinessText[snapshot.participant.readiness])}</strong><small>${pendingReadiness ? "전송 결과를 확인하지 못했어요. 다시 확인한 뒤 재전송해주세요." : "마지막으로 서버에 저장된 상태예요."}</small></div>
    <div class="button-pair">${button("준비 중", "preparing", snapshot.participant.readiness !== "PREPARING", !online)}${button("준비 완료", "ready", snapshot.participant.readiness !== "READY", !online)}</div>
    ${pendingReadiness && online ? button("선택한 상태 다시 보내기", "retry-readiness", true) : ""}
    <p class="hint">저장된 상태가 운전자 화면에 표시되기까지 잠시 걸릴 수 있어요.</p></section>`;
}

function render() {
  showedVehicle = Boolean(
    visibleVehicle(
      vehicle,
      deadline,
      performance.now(),
      online,
      snapshot?.status,
    ),
  );
  let content;
  if (finished)
    content = `<section class="card end"><span class="end-mark">✓</span><span class="badge">위치 공유 종료</span><h1>${{ COMPLETED: "잘 만났어요!", CANCELLED: "약속이 취소됐어요.", EXPIRED: "약속 시간이 지났어요." }[finished.status]}</h1><p>차량 위치와 도착 예정 시간은<br />더 이상 볼 수 없어요.</p>${button("처음으로", "home", true)}</section>`;
  else if (session && !snapshot)
    content = `<section class="card"><h1>${online ? "약속을 확인하고 있어요." : "약속에 연결하고 있어요."}</h1><p>잠시 후 다시 확인해주세요.</p>${button("다시 확인", "refresh", true)}${button("이 탭에서 나가기", "leave", true)}</section>`;
  else if (session) content = session.role === "driver" ? driver() : guest();
  else
    content =
      page === "create" ? createForm() : page === "join" ? joinForm() : home();
  const markup = `${alerts()}${content}`;
  if (markup !== renderedMarkup) {
    const focused = document.activeElement;
    const focusId = root.contains(focused) ? focused.id : null;
    const focusAction = root.contains(focused) ? focused.dataset?.action : null;
    root.innerHTML = markup;
    renderedMarkup = markup;
    const nextFocus = focusId
      ? root.querySelector(`#${CSS.escape(focusId)}`)
      : focusAction
        ? root.querySelector(`[data-action="${CSS.escape(focusAction)}"]`)
        : null;
    nextFocus?.focus({ preventScroll: true });
  }
  document.title = session
    ? `마중 · ${session.role === "driver" ? "운전자 확인" : "나의 약속"}`
    : "마중 · 함께 만날 약속";
}

function handleError(error) {
  if (
    error instanceof ApiError &&
    [401, 403, 404].includes(error.status) &&
    error.code !== "APPROVAL_REQUIRED"
  ) {
    forgetSession();
    invitation = null;
    page = "home";
    message =
      "이 약속에 접근할 수 없어요. 초대가 다시 발급됐거나 세션이 만료됐을 수 있어요.";
    return;
  }
  message =
    error instanceof ApiError
      ? error.message
      : "서버 응답을 확인하지 못했어요. 연결을 확인한 뒤 다시 시도해주세요.";
  if (!(error instanceof ApiError)) {
    online = false;
    vehicle = null;
    deadline = 0;
  }
}

async function refresh() {
  if (!session || refreshing || busy || document.hidden) return;
  refreshing = true;
  const currentSession = session;
  const generation = epoch;
  try {
    const data = await request(`/${currentSession.pickupId}`, {
      token: currentSession.token,
    });
    if (generation !== epoch || currentSession !== session || busy) return;
    snapshot = data;
    online = true;
    message = "";
    if (terminalStatuses.has(data.status)) {
      finish(data);
      return;
    }
    if (["ACTIVE", "ARRIVED"].includes(data.status)) {
      if (session.invite) {
        delete session.invite;
        storeSession();
      }
      if (session.role === "guest") {
        const started = performance.now();
        const state = await request(
          `/${currentSession.pickupId}/vehicle-state`,
          { token: currentSession.token },
        );
        if (generation !== epoch || currentSession !== session || busy) return;
        vehicle = state;
        deadline = vehicleDeadline(state, started, performance.now());
      }
    } else {
      vehicle = null;
      deadline = 0;
    }
  } catch (error) {
    if (generation !== epoch || currentSession !== session || busy) return;
    // Any failed read invalidates the displayed position, even if it is an HTTP error.
    online = false;
    vehicle = null;
    deadline = 0;
    handleError(error);
  } finally {
    refreshing = false;
    render();
  }
}

async function mutate(action) {
  if (busy) return;
  busy = true;
  epoch += 1;
  message = "";
  notice = "";
  render();
  try {
    await action();
  } catch (error) {
    handleError(error);
  } finally {
    busy = false;
    render();
    await refresh();
  }
}

root.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.target;
  if (!form.reportValidity() || busy) return;
  const fields = new FormData(form);
  if (form.id === "create-form") {
    draft.label = fields.get("label").trim();
    draft.latitude = fields.get("latitude");
    draft.longitude = fields.get("longitude");
    if (!draft.label) {
      message = "만날 장소를 입력해주세요.";
      render();
      return;
    }
    await mutate(async () => {
      const created = await request("", {
        method: "POST",
        body: {
          place: {
            label: draft.label,
            latitude: Number(draft.latitude),
            longitude: Number(draft.longitude),
          },
        },
      });
      session = {
        role: "driver",
        pickupId: created.pickupId,
        token: created.driverToken,
        invite: {
          token: created.inviteToken,
          expiresAt: created.inviteExpiresAt,
        },
      };
      storeSession();
    });
  } else if (form.id === "join-form" && invitation) {
    draft.name = fields.get("name").trim();
    if (!draft.name) {
      message = "운전자가 알아볼 이름을 입력해주세요.";
      render();
      return;
    }
    const invite = invitation;
    await mutate(async () => {
      const joined = await request(`/${invite.pickupId}/participants`, {
        method: "POST",
        body: {
          inviteToken: invite.inviteToken,
          displayName: draft.name,
        },
      });
      session = {
        role: "guest",
        pickupId: invite.pickupId,
        token: joined.participantToken,
        participantId: joined.participantId,
      };
      invitation = null;
      storeSession();
    });
  }
});

async function sendReadiness(value) {
  pendingReadiness = value;
  await mutate(async () => {
    const result = await request(`/${session.pickupId}/readiness`, {
      method: "PUT",
      token: session.token,
      body: { readiness: value, sequence: nextSequence(snapshot) },
    });
    snapshot = result;
    pendingReadiness = null;
    notice = "준비 상태를 서버에 저장했어요.";
  });
}

root.addEventListener("click", async (event) => {
  const target = event.target.closest("[data-action]");
  if (!target || busy || target.disabled) return;
  const action = target.dataset.action;
  if (["home", "prepare"].includes(action)) {
    finished = null;
    message = "";
    notice = "";
    invitation = null;
    page = action === "prepare" ? "create" : "home";
    render();
    return;
  }
  if (action === "leave") {
    if (
      !confirm("이 탭의 세션을 지울까요? 다시 참여하려면 새 초대가 필요해요.")
    )
      return;
    forgetSession();
    page = "home";
    render();
    return;
  }
  if (action === "refresh") {
    await refresh();
    return;
  }
  if (action === "copy-invite") {
    const url = invitationUrl(
      location.origin,
      session.pickupId,
      session.invite.token,
    );
    try {
      await navigator.clipboard.writeText(url);
      notice = "초대 링크를 복사했어요.";
    } catch {
      manualLink = true;
      notice = "아래 링크를 직접 복사해주세요.";
    }
    render();
    return;
  }
  if (["ready", "preparing", "retry-readiness"].includes(action)) {
    if (!online) return;
    await sendReadiness(
      action === "retry-readiness"
        ? pendingReadiness
        : action === "ready"
          ? "READY"
          : "PREPARING",
    );
    return;
  }
  if (!session || session.role !== "driver" || !online) return;
  const confirmations = {
    rotate: "새 초대를 만들까요? 이전 링크와 참여 요청은 취소돼요.",
    cancel: "픽업을 취소할까요? 차량 위치 공유도 종료돼요.",
    complete: "탑승을 확인했나요? 픽업을 완료하면 위치 공유가 종료돼요.",
  };
  if (confirmations[action] && !confirm(confirmations[action])) return;
  const paths = {
    approve: `/participants/${snapshot.participant?.participantId}/approval`,
    arrive: "/arrival",
    complete: "/completion",
    cancel: "/cancellation",
    rotate: "/invitations",
  };
  if (!paths[action]) return;
  await mutate(async () => {
    const result = await request(`/${session.pickupId}${paths[action]}`, {
      method: "POST",
      token: session.token,
    });
    if (action === "rotate") {
      session.invite = {
        token: result.inviteToken,
        expiresAt: result.expiresAt,
      };
      storeSession();
      manualLink = false;
      notice = "새 초대를 만들었어요.";
    } else {
      snapshot = result;
      if (terminalStatuses.has(result.status)) finish(result);
    }
  });
});

document.addEventListener("visibilitychange", () => {
  vehicle = null;
  deadline = 0;
  if (!document.hidden) {
    render();
    void refresh();
  }
});
window.addEventListener("offline", () => {
  online = false;
  vehicle = null;
  deadline = 0;
  render();
});
window.addEventListener("online", () => {
  void refresh();
});
setInterval(() => {
  const showing = Boolean(
    visibleVehicle(
      vehicle,
      deadline,
      performance.now(),
      online,
      snapshot?.status,
    ),
  );
  if (showedVehicle && !showing && !document.hidden) render();
  showedVehicle = showing;
}, 250);
setInterval(() => {
  void refresh();
}, 3000);
render();
if (session) void refresh();
