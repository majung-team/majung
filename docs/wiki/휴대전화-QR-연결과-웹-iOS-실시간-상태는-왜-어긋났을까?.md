차량 앱 · 모바일 웹 · iOS · 상태 동기화 | [SeungJunS0ng](https://github.com/SeungJunS0ng) | 글 정리: 2026년 11월 15일

[마중 Wiki 홈](https://github.com/majung-team/majung/wiki)

휴대전화에서는 차량 연결을 요청했는데 차량 화면은 그대로였다. 탑승자가 ‘준비 완료’를 눌렀는데, 잠시 뒤 다시 ‘준비 중’처럼 보이는 경우도 있었다.

마중은 차량 앱·웹·iOS 앱이 같은 서버의 상태를 각자 조회한다. 같은 데이터를 사용한다고 세 화면이 항상 같은 순서로 갱신되는 것은 아니었다. 요청과 응답이 도착하는 순서, 화면을 여는 시점, 시험용 서버의 응답까지 나눠 봐야 했다.

## 휴대전화에서 QR을 찍은 뒤 무엇을 해야 할지 불분명했다

차량 연결은 휴대전화에서 QR을 읽고 로그인하는 것으로 끝나지 않는다. 차량 쪽에서도 요청한 계정을 확인해야 한다. 그런데 차량 화면의 갱신이 늦고 확인창도 바로 나타나지 않아, 사용자가 직접 새로고침 버튼을 찾아야 했다.

QR 연결을 기다리는 동안에는 계정 조회 간격을 줄였다. 차량이 연결 확인 대기 상태를 받으면 해당 요청의 확인창을 한 번 자동으로 띄우도록 바꿨다. 사용자는 요청한 계정이 맞는지 확인하고 연결을 마칠 수 있다.

계정을 확인하는 단계는 유지하되, 그 단계를 찾으려고 새로고침을 반복하는 일을 줄였다.

## 준비 상태를 바꿨는데 예전 조회가 뒤늦게 도착했다

예를 들어 ‘준비 중’을 조회한 응답이 오는 동안 탑승자가 ‘준비 완료’를 보낼 수 있다. 저장이 끝난 뒤 예전 조회 응답이 도착하면 화면이 과거 상태로 돌아간다.

전송 중에는 중복 입력을 막고, 변경 전에 시작한 조회가 새 상태를 덮어쓰지 못하도록 했다. 화면의 확인 문구도 나눴다. 서버가 저장한 것과 차량이 전달받은 것은 다른 단계이므로, 차량의 수신 확인이 있어야 ‘차량에 전달됐어요’라고 표시한다. 이것이 운전자가 직접 읽었다는 뜻은 아니다.

같은 차량 이름이 여러 대일 때는 구분 코드와 계정 연결 시각을 함께 표시했다. 이름만 보고 다른 차량을 선택하거나 해제하지 않게 하기 위해서다. 새 요청 화면에서 지도가 높이 0으로 접히던 문제도 별도로 수정했다.

## 실시간 알림에는 바뀌었다는 신호만 넣었다

SSE는 서버가 웹으로 변경 소식을 보내는 연결 방식이다. 마중에서는 이 연결에 차량 좌표와 픽업 정보 전체를 싣지 않고, ‘변경이 있으니 다시 조회하라’는 신호만 보낸다.

신호를 받은 웹은 현재 권한으로 서버 API를 다시 조회한다. 오래된 알림에 담긴 데이터를 그대로 화면에 덮어쓰는 일을 줄이고, 로그아웃이나 연결 해제 뒤에도 조회 권한을 다시 검사할 수 있게 한 것이다.

연결이 끊기면 간격을 두고 다시 연결하며, 일정 시간 반응이 없을 때도 끊긴 것으로 판단한다. 다만 신호만 보낸다고 모든 응답 순서 문제가 사라지는 것은 아니다. 준비 상태처럼 사용자 변경과 조회가 겹치는 곳에는 별도의 보호가 필요했다.

## iOS에서는 화면 문제와 시험 문제를 구분했다

초대 링크를 붙여넣을 때 입력칸이 여러 줄로 늘어나고 키보드가 잘 닫히지 않는 문제가 있었다. 한 줄 입력과 이동 키, 닫기 동작을 정리했다. 참여 직후에는 필요한 정보를 먼저 읽고 픽업 화면으로 넘어가도록 바꿔, 정보가 아직 없는 중간 화면이 잠깐 보이는 현상을 줄였다.

차량 연결 시험에서는 다른 문제가 있었다. 앱은 자동으로 연결 상태를 확인하고 있었는데, 시험은 수동 확인 버튼을 누르려 했다. 시험용 서버도 첫 완료 응답 뒤 상태를 지워 버려 두 동작이 서로 엇갈렸다.

자동 확인과 수동 확인 시험을 나누고, 시험용 서버가 완료 상태를 유지하게 수정했다. 유효기간도 고정된 과거 날짜 대신 실행 시점 기준으로 만들었다. 모든 실패를 앱 기능의 오류로 보거나, 반대로 시험 문제라고 넘기지 않고 어느 쪽의 동작이 어긋났는지 확인했다.

이후 입력·키보드 동작과 실시간 현황 표시는 추가로 보완했다. 아래의 구현·시험 기록은 각 작업 당시의 결과이며, 최신 앱 전체의 시험 건수를 뜻하지 않는다.

## 어디까지 확인했나

웹에서는 준비 상태 전송과 늦은 조회 응답, 지도 표시를 시험했다. 서버에서는 실시간 연결 중 로그아웃·권한 철회가 적용되는지 확인했다. iOS에서는 자동·수동 차량 연결과 초대 참여 흐름을 시뮬레이터에서 검사했다.

실제 iPhone 카메라로 차량 QR을 읽는 과정, 카카오 앱으로 전환하는 최초 로그인, 실제 기기의 원격 푸시 수신까지 모두 검증한 것은 아니다. 시뮬레이터와 시험용 서버로 확인한 범위를 실제 기기 검증으로 적지 않았다.

---

<details>
<summary>구현 세부와 당시 시험 기록 보기</summary>

아래는 각 수정 당시의 코드·시험·커밋 근거다. 에뮬레이터와 실제 차량의 확인 범위를 구분해 기록했다.

**작업 영역:** 차량 앱 · 모바일 웹 · iOS · 백엔드 SSE · **기술 주제:** QR 연결 흐름, 웹·iOS UI 상태 어긋남, SSE 재조회 계약 · **작성자:** [SeungJunS0ng](https://github.com/SeungJunS0ng) · **기준일:** 2026년 11월 13일(KST)

[마중 Wiki 홈](https://github.com/majung-team/majung/wiki) → 클라이언트 · 실시간 연동 → QR 연결과 웹·iOS 상태 맞추기

마중은 차량 앱(Android Automotive), 탑승자·운전자 웹, iOS 동반 앱이 하나의 백엔드 세션을 함께 바라본다. 각 클라이언트가 서로 다른 주기, 다른 URL 진입 경로, 다른 키보드·카메라 환경을 타다 보니 기능 자체는 돌아가도 "화면 전환 타이밍이 어긋나거나 상태가 한 박자 늦게·거꾸로 보이는" 현상이 여러 곳에서 나타났다.

---

### 1. 차량 QR 스캔 후 연결 확인이 한 박자 늦던 문제 (`ad3ffd6`)

차량과 운전자 계정을 QR로 연결할 때, 휴대전화에서 QR을 찍고 카카오 로그인을 끝내더라도 차량 화면에서는 사용자가 직접 새로고침이나 `[요청한 운전자 계정 확인]` 버튼을 눌러야 확인 창이 뜨곤 했다. 게다가 계정 폴링이 10틱 주기로 돌고 있어 연결 요청 직후 반응이 느렸다.

`ad3ffd6`에서는 두 군데를 손봤다.

- `MainActivity`의 주기 tick에서 `app.account.qrLink != null`일 때는 계정 상태 조회 주기를 기존 10틱에서 3틱(`ticks % 3 == 0`)으로 단축했다.
- `VehicleAccountSection`이 `AWAITING_VEHICLE_CONFIRMATION` 상태를 처음 감지하면 `challengeId:majungId` 키당 한 번씩 확인 팝업(`openConfirmDialog()`)을 자동으로 띄워, 휴대전화 요청 직후 차량에서 원터치로 `"이 계정으로 연결"`을 마칠 수 있게 맞췄다.

---

### 2. 웹에서 지도 높이·준비 상태·동일 차량명이 어긋나던 문제 (`#88`, `1cbd1c2`)

11월 5일 사용성 점검(웹 PR [#88](https://github.com/majung-team/majung-web/commit/1cbd1c23e2518c8af98c76bb16b8561131cac44c), `docs/usability-2026-11-05.md`)에서는 웹 화면의 어긋남 세 가지를 한꺼번에 고쳤다.

- **새 요청 지도 높이가 0px로 접히던 현상**: 기존에는 진행 중 픽업 지도에만 높이가 지정돼 있어, 지인에게 새로 마중을 요청하는 화면에서는 카카오 지도가 `0px`로 보이지 않았다. 최소 높이를 지정해 `390px` 모바일 화면에서도 약 `304px` 높이로 지도가 바로 열리게 고쳤다.
- **준비 상태 슬라이딩 세그먼트와 늦은 조회 응답 덮어쓰기**: 웹 `model.js`의 `readinessControl` / `beginReadinessSend()`(`.readiness-segmented-control`, `.readiness-slider`)에서 버튼이 '보내는 중…' 상태로 잠겨 중복 전송을 막게 하고, 전송 전에 출발했던 GET 조회 응답이 뒤늦게 도착해 새 상태를 덮어쓰지 않도록 막았다. 또한 서버에 저장된 사실만으로 "운전자가 읽었다"고 추정하지 않고 `readinessReceivedSequence >= readinessSequence`일 때만 "차량에 전달됐어요."로 표시하게 나눴다.
- **같은 차량명이 여러 대일 때 혼동**: 같은 모델명의 차량이 여러 대 연결돼 있으면 구분 코드와 계정 연결 시각(`linkedAt`, 마지막 운행 시각 아님)을 목록과 연결 해제 확인창에 함께 띄웠다.

---

### 3. SSE에 상태 본문을 싣지 않고 `"신호 후 API 재조회"`로 둔 이유 (`stream.js`, `102a9a4`)

실시간 갱신에서 가장 고민한 지점은 SSE 이벤트 자체에 차량 좌표나 픽업 상태 전체 JSON을 실어 보낼 것인가였다. 결론은 **SSE는 "변화가 있으니 다시 읽어라"라는 신호만 보내고, 실제 데이터는 기존 인증된 REST API로 다시 조회한다**는 방향이었다.

> [!TIP]
> 실시간 동기화의 핵심 원칙은 **SSE가 상태 변경 신호(`event: changed` / `data: refresh`)만 보내고, 실제 스냅샷은 인증된 REST API를 다시 조회해 맞추는 구조**다. SSE 프레임 역전이나 캐시 스냅샷 오염 없이 재연결과 권한 차단을 단순하게 다룰 수 있다.

`majung-web/stream.js`와 백엔드 `PickupEventsApi.java`(`102a9a4`)는 다음과 같이 동작한다.

1. `EventSource`는 URL 쿼리에 토큰을 붙이지 않고는 Bearer 헤더를 실을 수 없기 때문에, 웹에서는 `fetch` 스트리밍 기반 `openPickupStream()`을 써서 게스트 토큰을 `Authorization` 헤더에, 회원 요청 SSE(`/api/v1/ride-requests/{id}/events`)는 `same-origin` HttpOnly 세션 쿠키로 보낸다.
2. 서버는 `event: changed` / `data: refresh`(또는 권한 종료 시 `event: access-ended`)만 보낸다. 본문에 좌표·메시지를 담지 않아 SSE 프레임 역전이나 오래된 캐시 스냅샷과 섞일 여지가 없고, 프레임 크기 상한(131,072B)과 25초 watchdog 타이머·`1초~15초` 지수 백오프 재연결을 걸기 쉽다.
3. 백엔드는 스트림 초기 검증과 재조회 시 `verifyActiveSession()`으로 현재 JDBC HTTP 세션이 아직 유효한지 다시 확인한다. 로그아웃되거나 세션이 철회되면 SSE 연결도 끊기고 재조회도 막힌다(`PickupEventsAuthorizationTest`).

---

### 4. iOS QR 스캐너·초대 입력 UX·연결 경합 테스트 정리 (`c4ac2cb`, `9bbcf43`, `#29`)

iOS 동반 앱 쪽에서는 실제 iPhone 사용 흐름과 UI 테스트에서 다섯 가지 어긋남을 발견했다.

- **QR 스캐너 호환성과 URL Fragment 변형 파싱 (`c4ac2cb`)**: 초기 `VisionKit` `DataScannerViewController` 의존으로는 지원 조건이 까다로워 `AVFoundation` 카메라 캡처(`VehicleCameraQRScanner`, `AVCaptureDevice.default(for: .video)`) 기반으로 정리하고, 차량 QR 링크가 `https://majung.cloud/account#vehicle/...`, `/account/#enroll/...`, `#enroll/...`, `account#...` 등 경로/해시 조합이 조금씩 달라도 모두 파싱하도록 `VehiclePairingTarget`을 보강했다(`MajungCoreTests`). 1인 테스트용 로컬 데모 서버(`SoloDemoServer`) 리스너 준비 대기 시간도 `2초`에서 `8초`로 늘렸다.
- **초대 링크 여러 줄 입력과 키보드 닫기 실패 (`9bbcf43`)**: 홈 화면 초대 링크 입력창이 `axis: .vertical`로 열려 여러 줄 붙여넣기나 줄바꿈 시 입력칸이 커지고, Return 키나 바깥 영역 스크롤로 키보드가 잘 닫히지 않았다. 한 줄 입력, 지우기 버튼, `.submitLabel(.go)`, `.scrollDismissesKeyboard(.interactively)`, 키보드 상단 `"키보드 닫기"` 툴바를 추가했고, 로그인 온보딩 재확인 및 QR 재스캔 재시도도 보강했다(`e88cbb8`).
- **차량 연결 UI 테스트의 자동 폴링 vs 수동 확인 경합 (PR [#29](https://github.com/majung-team/majung-ios/pull/29), `fadb31d`)**: 설정 탭에서 백그라운드로 돌던 `2초` 간격 자동 폴링(`pollPendingVehicleUntilDone`)과 수동 `"상태 확인"` 버튼을 검증하는 UI 테스트가 동시에 돌면서 상태 확인 시점이 엇갈렸다. 게다가 UI 테스트용 `LocalE2EServer`도 `GET /api/v1/vehicle-links/requests/{id}`가 `LINKED`를 반환하자마자 `pendingChallenge = nil`로 지워버리고, `expiresAt`을 `"2026-11-11T10:15:00Z"` 고정 문자열로 내려주고 있었다. PR `#29`에서는 자동 폴링 완료 테스트와 수동 확인 테스트(`-UITestManualVehiclePairing` 인자)를 분리하고, 목 서버가 `LINKED` 상태를 유지하며 실행 시점 기준(`Date().addingTimeInterval(600)`) 만료 시각을 만들도록 고쳤다.
- **초대 참여 직후 화면 전환 레이아웃 보정·상시 이용 가이드 전환 (`bea016f`, `1b48ec3`)**: 초대 참여(`PickupViewModel.join()`) 시 초기 스냅샷·차량 정보를 먼저 조회한 뒤 `.pickup` 화면으로 넘기고, `.inline` 타이틀·중앙 `MajungCard` 로딩/재시도 화면으로 통일해 참여 순간 레이아웃 어긋남을 막았다. 또한 재설치·로그아웃마다 첫 화면을 가리던 강제 `OnboardingStore` 게이트를 제거하고 로그인·홈 우측 상단(`?`)과 설정에서 언제든 여는 상시 **마중 이용 가이드**(`MajungGuideView`)로 전환했다.
- **픽업 완료 화면 카드·버튼 톤 통일, 종료 후 도착 알림 정리 및 다이나믹 아일랜드 실시간 현황 표시 (`c46907d`, `9315ef8`, `71491f7`, `2a1fe46`)**: 완료·취소·만료 화면(`finished`)에만 남아 있던 초록 틴트 배경·외곽선과 `.tint(.green)` 버튼을 걷어내고 도착 화면(`arrival`)과 같은 `MajungCard`, `statusHeroHeader`, `placeSummaryBox`, 기본 강조 버튼으로 통일했다. 초대 입력 키보드 툴바를 하단 안전 영역 바로 옮겨 프레임 경고를 없앴고(`9315ef8`), 픽업 종료·알림 비활성화·등록 경합 시 이전 도착 알림을 즉시 제거하며 포그라운드에서는 소리(`[.sound]`)만 사용하도록 정리했다(`71491f7`). 또한 로컬 `ActivityKit`(`pushType: nil`) 기반 다이나믹 아일랜드·잠금화면 실시간 현황(`MajungLiveActivityExtension`)을 추가해 앱을 내려도 예상 도착 시각 카운트다운이 유지되고 터치 시 활성 픽업 화면으로 즉시 복귀하도록 구성했다(`2a1fe46`).

---

### 확인한 것과 확인하지 못한 것

| 클라이언트 | 해결한 동기화 문제 | 확인 · 테스트 근거 |
| --- | --- | --- |
| **차량**<br>`ad3ffd6` | 3틱 QR 폴링<br>계정 확인 팝업 자동화 | `AWAITING_VEHICLE`<br>`_CONFIRMATION` 트리거 |
| **모바일&nbsp;웹**<br>`#88` | 요청 지도 `304px` · 상태 보호<br>`linkedAt` 차량 구분 | `npm test` 115건 통과<br>(`b2970b4`) |
| **백엔드**<br>`102a9a4` | 목록 SSE 신호 +<br>`verifyActiveSession()` 철회 차단 | JUnit `126/127`<br>(`0 failures, 1 skipped`) |
| **iOS**<br>`2a1fe46` | QR·UI 경합 분리 · 입력 경고 제거<br>완료 톤 통일 · 종료 알림 정리 · 다이나믹 아일랜드 | `MajungTests` 37 · `MajungUITests` 7<br>(+ 운영 연동 2 시뮬레이터) |

**확인하지 못한 점**

- 실제 물리 `iPhone` 기기에서의 카메라 차량 QR 스캔, 카카오 앱 전환 OAuth 로그인, 실주행 중 백그라운드 SSE 끊김 및 유료 Apple Developer 프로비저닝을 전제로 한 실기기 APNs 푸시 수신.

---

### 관련 커밋·문서

- [차량 `ad3ffd6`: QR 스캔 후 자동 확인 팝업](https://github.com/majung-team/majung-vehicle-app/commit/ad3ffd6b9e64ed3a1c5b2188334e729005044f42)
- [웹 #88: 요청 지도·준비 상태·차량 구분 개선 (`1cbd1c2`)](https://github.com/majung-team/majung-web/commit/1cbd1c23e2518c8af98c76bb16b8561131cac44c) · [웹 사용성 보완 기록](https://github.com/majung-team/majung-web/blob/main/docs/usability-2026-11-05.md) · [`stream.js`](https://github.com/majung-team/majung-web/blob/main/stream.js)
- [백엔드 SSE·계정 기능 보강 (`102a9a4`)](https://github.com/majung-team/majung-backend/commit/102a9a4e09c9fa07f7be418fa4f3585ad41bee8c)
- [iOS QR 스캐너·프래그먼트 수정 (`c4ac2cb`)](https://github.com/majung-team/majung-ios/commit/c4ac2cb9cce2736323627b427aaf56e6f9344f72) · [초대 입력·키보드 닫기 (`9bbcf43`)](https://github.com/majung-team/majung-ios/commit/9bbcf43586e264c42c551b4b364ce114f14868bf) · [PR #29: 자동 갱신·수동 확인 분리 (`fadb31d`)](https://github.com/majung-team/majung-ios/commit/fadb31d62aff4c67683b0acc61ea889dde5555a9) · [참여 전환 레이아웃 보완 (`bea016f`)](https://github.com/majung-team/majung-ios/commit/bea016fd27cd89a94378f2ff3161d2a27727baaf) · [상시 이용 가이드 전환 (`1b48ec3`)](https://github.com/majung-team/majung-ios/commit/1b48ec3b5cabe9e1544ae316702835cf1a615367) · [완료 화면 카드·버튼 톤 통일 (`c46907d`)](https://github.com/majung-team/majung-ios/commit/c46907d95c6d2d1c61732857c0cec705af30b871)

</details>
