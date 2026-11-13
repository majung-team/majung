**작업 영역:** 차량 앱 · 모바일 웹 · iOS · 백엔드 SSE · **기술 주제:** QR 연결 흐름, 웹·iOS UI 상태 어긋남, SSE 재조회 계약 · **작성자:** [SeungJunS0ng](https://github.com/SeungJunS0ng) · **기준일:** 2026년 11월 12일(KST)

[마중 Wiki 홈](https://github.com/majung-team/majung/wiki) → 클라이언트 · 실시간 연동 → QR 연결과 웹·iOS 상태 맞추기

마중은 차량 앱(Android Automotive), 탑승자·운전자 웹, iOS 동반 앱이 하나의 백엔드 세션을 함께 바라본다. 각 클라이언트가 서로 다른 주기, 다른 URL 진입 경로, 다른 키보드·카메라 환경을 타다 보니 기능 자체는 돌아가도 "화면 전환 타이밍이 어긋나거나 상태가 한 박자 늦게·거꾸로 보이는" 현상이 여러 곳에서 나타났다.

---

## 1. 차량 QR 스캔 후 연결 확인이 한 박자 늦던 문제 (`ad3ffd6`)

차량과 운전자 계정을 QR로 연결할 때, 휴대전화에서 QR을 찍고 카카오 로그인을 끝내더라도 차량 화면에서는 사용자가 직접 새로고침이나 `[요청한 운전자 계정 확인]` 버튼을 눌러야 확인 창이 뜨곤 했다. 게다가 계정 폴링이 10틱 주기로 돌고 있어 연결 요청 직후 반응이 느렸다.

`ad3ffd6`에서는 두 군데를 손봤다.

- `MainActivity`의 주기 tick에서 `app.account.qrLink != null`일 때는 계정 상태 조회 주기를 기존 10틱에서 3틱(`ticks % 3 == 0`)으로 단축했다.
- `VehicleAccountSection`이 `AWAITING_VEHICLE_CONFIRMATION` 상태를 처음 감지하면 `challengeId:majungId` 키당 한 번씩 확인 팝업(`openConfirmDialog()`)을 자동으로 띄워, 휴대전화 요청 직후 차량에서 원터치로 `"이 계정으로 연결"`을 마칠 수 있게 맞췄다.

---

## 2. 웹에서 지도 높이·준비 상태·동일 차량명이 어긋나던 문제 (`#88`, `1cbd1c2`)

11월 5일 사용성 점검(웹 PR [#88](https://github.com/majung-team/majung-web/commit/1cbd1c23e2518c8af98c76bb16b8561131cac44c), `docs/usability-2026-11-05.md`)에서는 웹 화면의 어긋남 세 가지를 한꺼번에 고쳤다.

- **새 요청 지도 높이가 0px로 접히던 현상**: 기존에는 진행 중 픽업 지도에만 높이가 지정돼 있어, 지인에게 새로 마중을 요청하는 화면에서는 카카오 지도가 `0px`로 보이지 않았다. 최소 높이를 지정해 `390px` 모바일 화면에서도 약 `304px` 높이로 지도가 바로 열리게 고쳤다.
- **준비 상태 슬라이딩 세그먼트와 늦은 조회 응답 덮어쓰기**: 웹 `model.js`의 `readinessControl` / `beginReadinessSend()`(`.readiness-segmented-control`, `.readiness-slider`)에서 버튼이 '보내는 중…' 상태로 잠겨 중복 전송을 막게 하고, 전송 전에 출발했던 GET 조회 응답이 뒤늦게 도착해 새 상태를 덮어쓰지 않도록 막았다. 또한 서버에 저장된 사실만으로 "운전자가 읽었다"고 추정하지 않고 `readinessReceivedSequence >= readinessSequence`일 때만 "차량에 전달됐어요."로 표시하게 나눴다.
- **같은 차량명이 여러 대일 때 혼동**: 같은 모델명의 차량이 여러 대 연결돼 있으면 구분 코드와 계정 연결 시각(`linkedAt`, 마지막 운행 시각 아님)을 목록과 연결 해제 확인창에 함께 띄웠다.

---

## 3. SSE에 상태 본문을 싣지 않고 `"신호 후 API 재조회"`로 둔 이유 (`stream.js`, `102a9a4`)

실시간 갱신에서 가장 고민한 지점은 SSE 이벤트 자체에 차량 좌표나 픽업 상태 전체 JSON을 실어 보낼 것인가였다. 결론은 **SSE는 "변화가 있으니 다시 읽어라"라는 신호만 보내고, 실제 데이터는 기존 인증된 REST API로 다시 조회한다**는 방향이었다.

> [!TIP]
> 실시간 동기화의 핵심 원칙은 **SSE가 상태 변경 신호(`event: changed` / `data: refresh`)만 보내고, 실제 스냅샷은 인증된 REST API를 다시 조회해 맞추는 구조**다. SSE 프레임 역전이나 캐시 스냅샷 오염 없이 재연결과 권한 차단을 단순하게 다룰 수 있다.

`majung-web/stream.js`와 백엔드 `PickupEventsApi.java`(`102a9a4`)는 다음과 같이 동작한다.

1. `EventSource`는 URL 쿼리에 토큰을 붙이지 않고는 Bearer 헤더를 실을 수 없기 때문에, 웹에서는 `fetch` 스트리밍 기반 `openPickupStream()`을 써서 게스트 토큰을 `Authorization` 헤더에, 회원 요청 SSE(`/api/v1/ride-requests/{id}/events`)는 `same-origin` HttpOnly 세션 쿠키로 보낸다.
2. 서버는 `event: changed` / `data: refresh`(또는 권한 종료 시 `event: access-ended`)만 보낸다. 본문에 좌표·메시지를 담지 않아 SSE 프레임 역전이나 오래된 캐시 스냅샷과 섞일 여지가 없고, 프레임 크기 상한(131,072B)과 25초 watchdog 타이머·`1초~15초` 지수 백오프 재연결을 걸기 쉽다.
3. 백엔드는 스트림 초기 검증과 재조회 시 `verifyActiveSession()`으로 현재 JDBC HTTP 세션이 아직 유효한지 다시 확인한다. 로그아웃되거나 세션이 철회되면 SSE 연결도 끊기고 재조회도 막힌다(`PickupEventsAuthorizationTest`).

---

## 4. iOS QR 스캐너·초대 입력 UX·연결 경합 테스트 정리 (`c4ac2cb`, `9bbcf43`, `#29`)

iOS 동반 앱 쪽에서는 실제 iPhone 사용 흐름과 UI 테스트에서 네 가지 어긋남을 발견했다.

- **QR 스캐너 호환성과 URL Fragment 변형 파싱 (`c4ac2cb`)**: 초기 `VisionKit` `DataScannerViewController` 의존으로는 지원 조건이 까다로워 `AVFoundation` 카메라 캡처(`VehicleCameraQRScanner`, `AVCaptureDevice.default(for: .video)`) 기반으로 정리하고, 차량 QR 링크가 `https://majung.cloud/account#vehicle/...`, `/account/#enroll/...`, `#enroll/...`, `account#...` 등 경로/해시 조합이 조금씩 달라도 모두 파싱하도록 `VehiclePairingTarget`을 보강했다(`MajungCoreTests`). 1인 테스트용 로컬 데모 서버(`SoloDemoServer`) 리스너 준비 대기 시간도 `2초`에서 `8초`로 늘렸다.
- **초대 링크 여러 줄 입력과 키보드 닫기 실패 (`9bbcf43`)**: 홈 화면 초대 링크 입력창이 `axis: .vertical`로 열려 여러 줄 붙여넣기나 줄바꿈 시 입력칸이 커지고, Return 키나 바깥 영역 스크롤로 키보드가 잘 닫히지 않았다. 한 줄 입력, 지우기 버튼, `.submitLabel(.go)`, `.scrollDismissesKeyboard(.interactively)`, 키보드 상단 `"키보드 닫기"` 툴바를 추가했고, 로그인 온보딩 재확인 및 QR 재스캔 재시도도 보강했다(`e88cbb8`).
- **차량 연결 UI 테스트의 자동 폴링 vs 수동 확인 경합 (PR [#29](https://github.com/majung-team/majung-ios/pull/29), `fadb31d`)**: 설정 탭에서 백그라운드로 돌던 `2초` 간격 자동 폴링(`pollPendingVehicleUntilDone`)과 수동 `"상태 확인"` 버튼을 검증하는 UI 테스트가 동시에 돌면서 상태 확인 시점이 엇갈렸다. 게다가 UI 테스트용 `LocalE2EServer`도 `GET /api/v1/vehicle-links/requests/{id}`가 `LINKED`를 반환하자마자 `pendingChallenge = nil`로 지워버리고, `expiresAt`을 `"2026-11-11T10:15:00Z"` 고정 문자열로 내려주고 있었다. PR `#29`에서는 자동 폴링 완료 테스트와 수동 확인 테스트(`-UITestManualVehiclePairing` 인자)를 분리하고, 목 서버가 `LINKED` 상태를 유지하며 실행 시점 기준(`Date().addingTimeInterval(600)`) 만료 시각을 만들도록 고쳤다.

---

## 확인한 것과 확인하지 못한 것

| 클라이언트 | 해결한 동기화 문제 | 확인 · 테스트 근거 |
| --- | --- | --- |
| **차량**<br>`ad3ffd6` | 3틱 QR 폴링<br>계정 확인 팝업 자동화 | `AWAITING_VEHICLE`<br>`_CONFIRMATION` 트리거 |
| **모바일&nbsp;웹**<br>`#88` | 요청 지도 `304px` · 상태 보호<br>`linkedAt` 차량 구분 | `npm test` 104건 통과<br>(`0188bd2`) |
| **백엔드**<br>`102a9a4` | 목록 SSE 신호 +<br>`verifyActiveSession()` 철회 차단 | JUnit `124/125`<br>(`0 failures, 1 skipped`) |
| **iOS**<br>`c4ac2cb` · `#29` | `AVFoundation` QR 스캐너<br>한 줄 입력 · UI 경합 분리 | `MajungTests` 26 · `MajungUITests` 6<br>(시뮬레이터) |

**확인하지 못한 점**

- 실제 물리 `iPhone` 기기에서의 카메라 차량 QR 스캔, 카카오 앱 전환 OAuth 로그인, 실주행 중 백그라운드 SSE 끊김 및 유료 Apple Developer 프로비저닝을 전제로 한 실기기 APNs 푸시 수신.

---

## 관련 커밋·문서

- [차량 `ad3ffd6`: QR 스캔 후 자동 확인 팝업](https://github.com/majung-team/majung-vehicle-app/commit/ad3ffd6b9e64ed3a1c5b2188334e729005044f42)
- [웹 #88: 요청 지도·준비 상태·차량 구분 개선 (`1cbd1c2`)](https://github.com/majung-team/majung-web/commit/1cbd1c23e2518c8af98c76bb16b8561131cac44c) · [웹 사용성 보완 기록](https://github.com/majung-team/majung-web/blob/main/docs/usability-2026-11-05.md) · [`stream.js`](https://github.com/majung-team/majung-web/blob/main/stream.js)
- [백엔드 SSE·계정 기능 보강 (`102a9a4`)](https://github.com/majung-team/majung-backend/commit/102a9a4e09c9fa07f7be418fa4f3585ad41bee8c)
- [iOS QR 스캐너·프래그먼트 수정 (`c4ac2cb`)](https://github.com/majung-team/majung-ios/commit/c4ac2cb9cce2736323627b427aaf56e6f9344f72) · [초대 입력·키보드 닫기 (`9bbcf43`)](https://github.com/majung-team/majung-ios/commit/9bbcf43586e264c42c551b4b364ce114f14868bf) · [PR #29: 자동 갱신·수동 확인 분리 (`fadb31d`)](https://github.com/majung-team/majung-ios/commit/fadb31d62aff4c67683b0acc61ea889dde5555a9)
