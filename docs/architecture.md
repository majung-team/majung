# 마중 아키텍처

기본 픽업 공유 구조는 유지한다. 2026-10-25 추가한 계정 연결·마중 요청은 아래 구성과 각 개발 저장소의 문서를 함께 본다.

## 서비스 구성

![서비스 구성](diagrams/overview.svg)

차량과 웹은 API를 거쳐 상태를 주고받는다. WebSocket·SSE·Redis는 사용하지 않는다.
위치와 ETA는 차량 SDK에서 받고 서버는 권한·순번·유효 시간을 검사해 저장한다. 서버가 경로나 ETA를 다시 계산하지는 않는다.

기존 초대는 차량 QR → 운전자 휴대전화 공유 → 탑승자 참여·승인 순서다. 새 방식에서는 로그인한 탑승자가 운전자를 검색해 장소와 함께 요청하고 차량에서 수락한다. 운전자 계정은 최초 한 번 차량에 연결하며, 탑승자는 차량 QR이 없어도 요청한다. 별도 탑승자 네이티브 앱은 아직 개발하지 않는다.

공개 웹과 API는 `https://majung.cloud`를 사용한다. Caddy가 정적 웹을 제공하고 `/api/`를 Spring Boot로 전달한다. 웹·서버 main은 각각 Actions로 배포하고, 차량 APK는 따로 빌드·설치한다.

새 요청은 `RideRequest`에 저장한다. 차량이 수락한 순서대로 대기에 넣고, 자기 차례에 기존 `Pickup`으로 연결해 준비 상태·SDK 공유·도착·완료를 사용한다. 차량 한 대에 한 운전자 계정이라는 기준과 여러 탑승자의 요청 수는 별개다.

[서버 요청 구조](https://github.com/majung-team/majung-backend/blob/main/docs/ride-requests.md) · [차량 계정·요청 화면](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/vehicle-account-requests.md) · [팀 테스트 안내](team-testing.md)

## 차량 앱 내부

![차량 앱 내부 구조](diagrams/vehicle.svg)

### 화면을 내렸을 때

`MainActivity`는 화면 갱신을 멈춘다. `PickupSharingService`가 실행 중이면 SDK 연결은 유지한다.
서비스가 서버 상태를 약 3초마다 확인하고, SDK에는 약 5초마다 위치·목적지를 요청한다.
`ACTIVE`·`ARRIVED`에서만 실행하며 탑승 완료·취소·만료를 확인하면 연결과 알림을 정리한다.

`VehicleTelemetry`는 위치와 ETA가 모두 수신 후 10초 미만이고, ETA의 목적지가 확정 장소와 일치할 때만 한 쌍으로 꺼낸다.
컨트롤러는 최소 5초 간격으로 보낸다. 정차해 값이 같아도 전송하지만, 오래된 자료의 시각을 현재 시각으로 바꾸지는 않는다.
이전 요청이 끝나기 전에는 다음 요청을 겹쳐 보내지 않는다.

### 앱 프로세스가 종료됐을 때

1. 앱을 다시 실행하면 암호화된 접근 정보로 서버 상태를 확인한다.
2. 진행 중이면 ‘이전 픽업 계속하기’ 또는 ‘픽업 취소’를 선택한다.
3. 계속하기를 누른 뒤 SDK에 다시 연결하고 새 자료로 전송한다.
4. 이미 종료됐거나 접근을 거절당하면 저장 정보를 삭제한다.
5. 일시적인 통신 오류면 저장 정보를 유지하고 상태 조회를 재시도한다.

저장하는 값은 픽업 ID·운전자 토큰·유효한 초대 토큰·마지막 전송 순번이다. 위치·ETA는 파일에 저장하지 않는다.
AES-GCM으로 암호화해 백업 제외 경로에 쓰고 키는 Android Keystore에서 관리한다. 다음 순번은 HTTP 요청 전에 저장한다.
재실행 복구와 시동 OFF 감지는 별개다. 강제 중지 후 자동 실행하거나 경로 안내를 자동 재시작하지 않는다.

### 주요 파일

경로는 [majung-vehicle-app](https://github.com/majung-team/majung-vehicle-app)의 `vehicle/src/main/java/app/majung/vehicle/` 기준이다.

| 파일 | 역할 |
| --- | --- |
| `VehicleApplication.kt` | reader·controller 생성, 암호화 저장소 연결과 최초 복구 |
| `MainActivity.kt` | 화면 생명주기, 권한 요청, 서비스 시작과 복구 확인 |
| `PickupCreationSection.kt` / `PickupParticipantSection.kt` | 생성·초대·참여 승인 |
| `PickupStatusSection.kt` / `PickupCompletionSection.kt` | 상태 표시·도착·완료·취소 |
| `PickupSharingService.kt` | 화면 밖 SDK 조회·전송·종료 감지 |
| `DestinationReader.kt` | SDK 연결·이벤트·조회와 연결 세대 검사 |
| `VehicleTelemetry.kt` | 위치·ETA 수신 시각과 목적지 일치 검사 |
| `PickupController.kt` | 픽업 상태·권한·순번·전송 중복 제어 |
| `PickupApiClient.kt` | JSON·Bearer 토큰 HTTP 요청 |
| `PickupSessionStore.kt` / `EncryptedPickupSessionStore.kt` | 저장 형식·암호화·원자적 파일 교체·삭제 |

[차량 화면 구조](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/vehicle-screen.md) · [저장·복구](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/pickup-recovery.md) · [화면 밖 공유](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/background-sharing.md)

## 서버와 웹

```text
majung-backend/
  src/main/java/app/majung/server/
    MajungServerApplication.java
    pickup/
      PickupApi.java              HTTP 경로·입력·응답
      PickupService.java          권한·상태·순번·시각 검사
      Pickup.java                 픽업·참여자·최신 차량 정보
      PickupRepository.java       저장·조회·행 잠금
      AccountApi.java / MemberService.java  카카오 계정·검색·사람 연결
      VehicleAccountService.java  차량 QR·운전자 계정 연결
      RideRequestService.java     탑승자 요청·대기·기존 픽업 연결
      TokenService.java           토큰 발급·해시 비교
      PickupRetentionJob.java     보관 기간이 지난 행 정리
  src/main/resources/             로컬·배포 설정과 초기 스키마
  deploy/                         배포 관련 파일

majung-web/
  index.html / style.css          화면·스타일
  app.js                         참여·준비 상태·주기 조회
  model.js                       화면 상태·만료 판단
  api.js                         API 요청
  account.html / account.js      로그인·검색·차량 연결·마중 요청
  account-api.js                 계정 세션·CSRF 요청
  dev-server.mjs                 로컬 정적 파일 제공·/api 프록시
```

Spring Boot JAR에서 웹 파일을 함께 제공하던 통합 버전과 달리, 현재 웹은 독립 저장소에서 실행한다.
로컬에서는 웹 개발 서버가 `/api` 요청을 백엔드로 넘긴다. 기존 초대 픽업의 접근 정보는 탭의 `sessionStorage`에 보관한다. 카카오 계정은 HttpOnly 세션 쿠키를 사용하고 변경 요청에는 CSRF 토큰을 붙인다. 차량 권한이나 카카오 인증정보를 브라우저에 저장하지 않는다.

서버는 픽업별 운전자·초대·탑승자 토큰을 구분하고 토큰 원문 대신 해시를 저장한다.
같은 픽업의 변경은 트랜잭션·행 잠금으로 처리하고 `sequence`로 중복·역순 변경을 막는다.
준비 완료, 차량 도착, 실제 탑승 완료는 각각 다른 상태다. 준비가 끝났다고 자동으로 도착 처리하지 않는다.

현재 서버 기본값은 초대 15분·픽업 2시간·차량 정보 유효 시간 30초다.
서버는 SDK 수신 시각과 서버 수신 시각에서 계산한 만료 중 이른 시점을 적용한다. 만료된 위치·ETA는 응답에서 숨긴다.
미응답 마중 요청은 30분, 수락한 대기는 2시간이 기본이다. 보관 정리가 활성화된 배포에서는 만료·종료 후 설정된 기간이 지난 기록을 지운다. 기본 보관 기간은 24시간이다.

## 로컬 실행과 배포

| 환경 | 현재 구성 |
| --- | --- |
| 로컬 웹 | 별도 Node 개발 서버 → `/api` 프록시 → 로컬 API |
| 로컬 차량 | 공식 Pleos 에뮬레이터 → 호스트 API |
| 로컬 DB | H2 |
| 공개 서버 | 마중 전용 GCP VM·PostgreSQL·Spring Boot |
| 외부 접속 | https://majung.cloud. Caddy가 HTTPS·웹 파일·API 프록시 담당 |
| 자동 배포 | 백엔드·웹 각각 main에서 Actions 실행. 차량은 APK 별도 설치 |

```text
휴대전화 웹·차량 APK ── HTTPS ── Caddy ── /api/ ── Spring Boot ── PostgreSQL
                                 └── 정적 웹 파일
```

DB·카카오 키·관리자 권한은 서버 환경변수로 관리한다. Actions는 GCP의 단기 인증과 제한된 배포 권한을 사용한다. 같은 VM의 웹·서버 배포는 겹치지 않도록 하고 결과가 정상인지 확인한 뒤 반영한다. 기존 플리플리 인스턴스와는 분리돼 있다.

실행·설치 명령과 첫 등록은 [팀 테스트 안내](team-testing.md)에 모았다. 각 제품의 코드·자동 시험은 해당 저장소에서 실행한다.

## 현재 검증과 남은 범위

- 공식 에뮬레이터·로컬 API에서 생성부터 탑승 완료까지 확인했다.
- 화면을 멈춘 뒤 310초 동안 최신 차량 정보 유지, 서버 취소 후 서비스 중단을 확인했다.
- 강제 중지 전·후 복구, 재개 확인 전 미전송, 재개 후 순번 유지와 종료 후 삭제를 확인했다.
- 공개 HTTPS에서 기존 초대 픽업 API와 차량 앱·SDK 연동, 실제 카카오 로그인과 세션 유지를 확인했다.
- 새 요청은 가상 계정 3명과 로컬 API·브라우저, 차량 계측 시험으로 확인했다.
- 실차 주행·시동 OFF/ON·플랫폼 심사·실제 휴대전화 GPS→새 요청→SDK 전체 흐름은 남아 있다.

[분리 전 아키텍처](archive/integrated-architecture.md)는 이전 폴더·실행 구조를 확인하는 기록이다.

그림은 `tools/render_architecture.py`로 다시 생성할 수 있다.
