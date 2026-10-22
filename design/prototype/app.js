import {
  initialState,
  reduce,
  places,
  isShared,
  isTerminal,
  guestView,
} from "./model.js";

let state = initialState();
let phoneRole = "driver";
let pendingEnd = null;
const history = [];
const $ = (id) => document.getElementById(id);
const esc = (text) =>
  String(text).replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const button = (label, action, disabled = false, kind = "") =>
  `<button type="button" data-action="${action}" class="${kind}" ${disabled ? "disabled" : ""}>${label}</button>`;
const placeBlock = (key, caption = "약속 장소") =>
  `<div class="place"><small>${caption}</small><strong>${esc(places[key].name)}</strong><small>${esc(places[key].detail)}</small></div>`;
const readinessText = {
  waiting: "아직 응답이 없어요",
  preparing: "준비하고 있어요",
  ready: "준비가 끝났어요",
};
const actionLabels = {
  CREATE: "차량에서 초대 준비",
  PAIR: "운전자 휴대전화 연결",
  JOIN: "탑승자 참여 요청",
  APPROVE: "운전자 확인 후 위치 공유 시작",
  READY: "탑승자 준비 상태 변경",
  PROPOSE: "새 만남 장소 제안",
  ACCEPT_PLACE: "차량에서 경로 변경 요청",
  REJECT_PLACE: "기존 장소 유지",
  ROUTE_RESULT: "내비게이션 결과 반영",
  ARRIVE: "도착 안내 · 공유는 유지",
  COMPLETE: "픽업 완료 · 공유 종료",
  CANCEL: "픽업 취소 · 공유 종료",
  EXPIRE: "초대 만료 · 참여 차단",
  CONNECTION: "차량 연결 상태 변경",
  MOTION: "차량 상태 변경",
};

function dispatch(action) {
  const next = reduce(state, action);
  if (action.type === "RESET") {
    phoneRole = "driver";
    history.length = 0;
  } else if (next !== state && actionLabels[action.type]) {
    history.unshift(actionLabels[action.type]);
    history.splice(7);
  }
  state = next;
  render();
}

function vehicleMarkup() {
  const parked = state.motion === "parked";
  const online = state.connection === "online";
  const guest = esc(state.guestName);
  if (isTerminal(state)) {
    const title = {
      completed: "잘 만났어요.",
      cancelled: "약속을 취소했어요.",
      expired: "초대가 만료됐어요.",
    }[state.phase];
    return `<h3>${title}</h3><p>위치 공유를 종료했어요.<br>이 링크로는 차량 위치를 볼 수 없어요.</p>${button("새 픽업 준비", "reset", !parked, "block")}<p class="mini-note">새 약속은 주차 후 준비해주세요.</p>`;
  }
  if (state.phase === "draft")
    return `<small>주차 중 미리 준비하세요</small><h3>기다리는 사람에게<br>약속을 보내세요.</h3><p>위치와 도착 예정 시간을 공유하고<br>상대방의 준비 상태를 확인해요.</p>${placeBlock(state.place, "내비게이션에 설정한 픽업 장소")}${button("초대 준비하기", "create", !parked, "block")}${!parked ? '<p class="mini-note">초대는 주차 상태에서 만들 수 있어요.</p>' : ""}`;
  if (state.phase === "inviting")
    return `<h3>${state.paired ? "탑승자의 참여를<br>기다리고 있어요." : "휴대전화에서<br>초대를 준비하세요."}</h3><p>${state.paired ? "운전자 휴대전화에서 링크를 복사해<br>기다리는 사람에게 전달하세요." : "운전자 휴대전화에서 마중을 열고<br>아래 연결 번호를 입력하세요."}</p>${!state.paired ? '<div class="code">402 618</div><small>연결 번호는 짧은 시간 동안만 유효해요.</small>' : placeBlock(state.place)}<p class="mini-note">탑승자가 참여해도 확인 전에는<br>차량 위치가 공유되지 않아요.</p>${button("초대 취소", "cancel", !parked, "end-link")}`;
  if (state.phase === "approval")
    return `<small>참여 요청</small><h3>${guest} 님이<br>약속에 참여했어요.</h3><p>초대한 사람이 맞는지 확인해주세요.<br>확인하면 이번 픽업 동안 위치를 공유해요.</p>${placeBlock(state.place)}${button("확인하고 위치 공유", "approve", !parked, "block")}${!parked ? '<p class="mini-note">참여 확인은 주차 후 진행해주세요.</p>' : ""}${button("초대 취소", "cancel", !parked, "end-link")}`;
  let output = `<small>이번 픽업 · ${guest}</small><h3>${state.phase === "arrived" ? "약속 장소에<br>도착했어요." : parked ? "출발할 준비가<br>되셨나요?" : "서로 준비 상태를<br>맞춰보세요."}</h3>`;
  output += `<div class="ready-state"><strong>${state.carReadiness === "ready" ? "✓ " : ""}${readinessText[state.carReadiness]}</strong><span>${guest} 님 ${!online ? "· 마지막으로 확인한 상태" : "· 상태 반영됨"}</span></div>`;
  output += placeBlock(state.place);
  if (!online)
    output +=
      '<p class="mini-note">차량 연결을 확인하고 있어요.<br>상대방의 새 준비 상태가 아직 반영되지 않을 수 있어요.</p>';
  if (state.phase === "arrived") {
    output += `<p>실제로 탑승한 뒤 픽업을 완료해주세요.<br>도착 안내만으로 공유를 종료하지 않아요.</p>${button("탑승 확인 · 픽업 완료", "complete", !parked, "block")}`;
    if (!parked)
      output += '<p class="mini-note">안전하게 주차한 뒤 완료해주세요.</p>';
  }
  if (state.proposal) {
    output += `<div class="proposal"><small>새 장소 제안 · 아직 확정 전</small><strong>${esc(places[state.proposal].name)}</strong><p>${state.routing ? "새 경로를 반영하고 있어요. 완료 전까지 기존 장소를 유지해요." : state.routeError ? "경로를 반영하지 못했어요. 기존 장소가 유지돼요." : parked ? "이 장소로 변경할까요?" : "주차한 뒤 변경 여부를 확인해주세요."}</p>`;
    if (!state.routing)
      output += `<div class="button-pair">${button(state.routeError ? "다시 요청" : "변경 요청", "accept-place", !parked || !online)}${button("기존 장소 유지", "reject-place", !parked || !online, "quiet")}</div>`;
    output += "</div>";
  }
  output += button("픽업 취소", "cancel", !parked, "end-link");
  return output;
}

function driverPhoneMarkup() {
  if (isTerminal(state))
    return `<h3>이번 약속은<br>종료됐어요.</h3><p>다음 픽업은 차량에서 새로 준비해주세요.</p><span class="pill">위치 공유 종료</span>`;
  if (state.phase === "draft")
    return '<h3>초대는 차량에서<br>먼저 준비해주세요.</h3><p>차량의 마중 화면에서 픽업 장소를 확인하고<br>‘초대 준비하기’를 선택하세요.</p><div class="readiness"><strong>내비게이션의 약속 장소를 함께 써요.</strong><p>휴대전화에서 목적지를 다시 입력할 필요가 없도록 연결하는 안이에요.</p></div>';
  if (!state.paired)
    return `<h3>운전자 휴대전화로<br>초대를 연결해요.</h3><p>차량에 표시된 번호를 확인해주세요.<br>연결은 주차 상태에서 진행해주세요.</p><label for="pair-code">차량 연결 번호<input id="pair-code" inputmode="numeric" autocomplete="off" value="402618" maxlength="6"></label><p id="pair-error" class="phone-note" role="alert"></p>${button("연결하고 초대 준비", "pair", state.motion !== "parked", "block")}<p class="phone-note">이 화면의 연결 번호는 검토용 예시예요.</p>`;
  return `<span class="pill">차량과 연결됨</span><h3>기다리는 사람에게<br>링크를 보내세요.</h3>${placeBlock(state.place)}<div class="link-box">https://majung.example/invite/demo</div>${button("초대 링크 복사", "copy-link", false, "block")}<p id="copy-result" class="phone-note" role="status">실제 초대가 전송되지는 않아요.<br>상단의 ‘탑승자’로 바꿔 참여해보세요.</p>${state.phase === "approval" ? '<div class="readiness"><strong>탑승자가 참여했어요.</strong><p>차량에서 초대한 사람이 맞는지 확인해주세요.</p></div>' : isShared(state) ? '<div class="readiness"><strong>위치를 공유하고 있어요.</strong><p>픽업이 끝나면 차량에서 공유를 종료할 수 있어요.</p></div>' : ""}`;
}

function guestPhoneMarkup() {
  if (isTerminal(state)) {
    const title = {
      completed: "잘 만났어요!",
      cancelled: "픽업이 취소됐어요.",
      expired: "초대가 만료됐어요.",
    }[state.phase];
    return `<h3>${title}</h3><p>차량 위치와 도착 예정 시간은<br>더 이상 볼 수 없어요.</p><span class="pill">${state.phase === "expired" ? "새 초대가 필요해요" : "위치 공유 종료"}</span>`;
  }
  if (!state.paired)
    return '<h3>약속 링크를<br>기다리고 있어요.</h3><p>운전자가 차량과 휴대전화를 연결한 뒤<br>초대 링크를 전달하면 참여할 수 있어요.</p><p class="phone-note">지금은 ‘운전자 휴대전화’ 화면에서<br>초대 연결을 먼저 진행해보세요.</p>';
  if (state.phase === "inviting")
    return `<span class="pill">마중 약속 초대</span><h3>함께 만날 약속에<br>참여할까요?</h3><p>참여 요청 후 운전자가 확인하면<br>차량 위치와 도착 예정 시간을 볼 수 있어요.</p><form id="join-form"><label for="guest-name">운전자가 알아볼 이름<input id="guest-name" name="name" maxlength="12" autocomplete="off" value="지민" required></label><button class="block" type="submit">약속에 참여하기</button></form><p class="phone-note">휴대전화 위치나 연락처는 요청하지 않아요.</p>`;
  if (state.phase === "approval")
    return `<span class="pill">참여 요청 완료</span><h3>운전자 확인을<br>기다리고 있어요.</h3><p>초대한 사람이 맞는지 확인되면<br>이 화면에서 도착 정보를 볼 수 있어요.</p><div class="readiness"><strong>아직 위치 공유 전이에요.</strong><p>왼쪽 차량 화면에서 참여를 확인해주세요.</p></div>`;
  const view = guestView(state);
  let output = `<span class="pill">${state.phase === "arrived" ? "도착 안내" : "위치 공유 중"}</span><h3>${state.phase === "arrived" ? "차량이<br>도착했어요." : state.routing ? "새 경로를<br>확인하고 있어요." : state.connection !== "online" ? "위치 업데이트를<br>기다리고 있어요." : "마중 나가고<br>있어요."}</h3>`;
  if (view.canViewLocation) {
    output += `<div class="eta"><small>${state.phase === "arrived" ? "약속 장소에서 만나세요" : view.etaMinutes === null ? "기존 경로의 도착 시간 재확인 중" : "도착까지"}</small><strong>${state.phase === "arrived" ? "도착" : view.etaMinutes === null ? "확인 중" : `약 ${view.etaMinutes}<span>분</span>`}</strong></div><div class="mini-map" aria-label="실제 지도가 아닌 예시 차량 위치"><b>차</b><span>만날 장소</span></div><p class="phone-note">방금 갱신된 위치 장면 · 예시 데이터</p>`;
  } else {
    output += `<p>${state.routing ? "변경한 경로가 확인되면<br>새 도착 예정 시간을 안내할게요." : "예전 위치를 현재 위치처럼 보여주지 않아요.<br>연결이 돌아오면 다시 갱신할게요."}</p><div class="readiness"><strong>${state.routing ? "도착 예정 시간 갱신 중" : state.connection === "stale" ? "마지막 차량 정보: 2분 전인 장면" : "차량 연결 끊김"}</strong></div>`;
  }
  output += placeBlock(state.place, "현재 확정된 약속 장소");
  output += `<div class="readiness"><strong>${readinessText[state.readiness]}</strong><p>${view.readinessPending ? "새 상태는 저장됐어요. 차량 반영을 기다리고 있어요." : state.readiness === "waiting" ? "출발할 준비가 되면 운전자에게 알려주세요." : "운전자 화면에 반영됐어요."}</p></div><div class="button-pair">${button("준비 중", "preparing", false, state.readiness === "preparing" ? "" : "quiet")}${button("준비 완료", "ready", false, state.readiness === "ready" ? "" : "quiet")}</div>`;
  if (state.phase === "active") {
    if (!state.proposal && state.place === "original")
      output += button(
        "만날 장소 변경 제안",
        "propose",
        state.routing,
        "quiet block",
      );
    if (state.proposal)
      output += `<p class="phone-note">제안한 장소: ${esc(places[state.proposal].name)}<br>${state.routing ? "경로 반영 결과를 기다리고 있어요." : state.routeError ? "변경 실패 · 기존 약속 장소를 유지하고 있어요." : "운전자 확인 전이에요. 기존 약속 장소를 유지해주세요."}</p>`;
  }
  return output;
}

function render() {
  $("vehicle").innerHTML = vehicleMarkup();
  $("phone-content").innerHTML =
    phoneRole === "driver" ? driverPhoneMarkup() : guestPhoneMarkup();
  $("phone-content").setAttribute("aria-labelledby", `${phoneRole}-tab`);
  $("driver-tab").setAttribute("aria-selected", String(phoneRole === "driver"));
  $("guest-tab").setAttribute("aria-selected", String(phoneRole === "guest"));
  $("nav-place").textContent = places[state.place].name;
  $("motion-label").textContent =
    state.motion === "parked" ? "P · 주차 중" : "D · 주행 중";
  $("car-connection").textContent = isTerminal(state)
    ? "공유 종료"
    : !isShared(state)
      ? "공유 전"
      : { online: "연결됨", stale: "갱신 지연", offline: "연결 끊김" }[
          state.connection
        ];
  document
    .querySelectorAll("[data-motion]")
    .forEach((b) =>
      b.setAttribute("aria-pressed", String(b.dataset.motion === state.motion)),
    );
  $("connection").value = state.connection;
  $("connection").disabled = !isShared(state);
  $("arrive").disabled =
    state.phase !== "active" || state.routing || state.connection !== "online";
  $("expire").disabled = !["inviting", "approval"].includes(state.phase);
  $("fresh-route").disabled =
    !isShared(state) ||
    state.etaValid ||
    state.routing ||
    state.connection !== "online";
  $("route-controls").hidden = !state.routing;
  const step = ["draft", "inviting"].includes(state.phase)
    ? 1
    : state.phase === "approval"
      ? 2
      : state.phase === "active"
        ? 3
        : 4;
  document
    .querySelectorAll("[data-step]")
    .forEach((el) =>
      el.classList.toggle("active", Number(el.dataset.step) === step),
    );
  $("history").innerHTML = history.length
    ? history.map((item) => `<li>${esc(item)}</li>`).join("")
    : "<li>새 픽업을 준비하고 있어요.</li>";
  const form = $("join-form");
  if (form)
    form.onsubmit = (e) => {
      e.preventDefault();
      dispatch({ type: "JOIN", name: $("guest-name").value });
    };
}

function endSession(type) {
  pendingEnd = type;
  $("end-title").textContent =
    type === "COMPLETE" ? "픽업을 완료할까요?" : "이번 픽업을 취소할까요?";
  $("end-dialog").showModal();
}

document.addEventListener("click", async (event) => {
  const target = event.target.closest("[data-action]");
  if (!target || target.disabled) return;
  const action = target.dataset.action;
  const simple = {
    create: "CREATE",
    reset: "RESET",
    approve: "APPROVE",
    propose: "PROPOSE",
    "accept-place": "ACCEPT_PLACE",
    "reject-place": "REJECT_PLACE",
  };
  if (simple[action]) dispatch({ type: simple[action] });
  else if (action === "pair") {
    if ($("pair-code").value.replace(/\s/g, "") !== "402618")
      $("pair-error").textContent =
        "차량에 보이는 연결 번호를 다시 확인해주세요.";
    else dispatch({ type: "PAIR" });
  } else if (action === "ready" || action === "preparing")
    dispatch({ type: "READY", value: action });
  else if (action === "complete" || action === "cancel")
    endSession(action.toUpperCase());
  else if (action === "copy-link") {
    try {
      await navigator.clipboard.writeText("https://majung.example/invite/demo");
      if ($("copy-result"))
        $("copy-result").textContent =
          "검토용 예시 링크를 복사했어요. 실제 초대 기능은 아직 연결하지 않았어요.";
    } catch {
      if ($("copy-result"))
        $("copy-result").textContent =
          "예시 주소를 직접 복사해주세요. 실제 초대는 전송되지 않아요.";
    }
  }
});

$("reset").onclick = () => dispatch({ type: "RESET" });
$("driver-tab").onclick = () => {
  phoneRole = "driver";
  render();
};
$("guest-tab").onclick = () => {
  phoneRole = "guest";
  render();
};
for (const id of ["driver-tab", "guest-tab"]) {
  $(id).onkeydown = (event) => {
    if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
    event.preventDefault();
    phoneRole =
      event.key === "Home"
        ? "driver"
        : event.key === "End"
          ? "guest"
          : phoneRole === "driver"
            ? "guest"
            : "driver";
    render();
    $(`${phoneRole}-tab`).focus();
  };
}
document
  .querySelectorAll("[data-motion]")
  .forEach(
    (b) =>
      (b.onclick = () => dispatch({ type: "MOTION", value: b.dataset.motion })),
  );
$("connection").onchange = (e) =>
  dispatch({ type: "CONNECTION", value: e.target.value });
$("arrive").onclick = () => dispatch({ type: "ARRIVE" });
$("expire").onclick = () => dispatch({ type: "EXPIRE" });
$("fresh-route").onclick = () => dispatch({ type: "FRESH_ROUTE_INFO" });
$("route-success").onclick = () => dispatch({ type: "ROUTE_RESULT", ok: true });
$("route-failure").onclick = () =>
  dispatch({ type: "ROUTE_RESULT", ok: false });
$("keep-session").onclick = () => {
  pendingEnd = null;
  $("end-dialog").close();
};
$("confirm-end").onclick = () => {
  $("end-dialog").close();
  dispatch({ type: pendingEnd });
  pendingEnd = null;
};
render();
