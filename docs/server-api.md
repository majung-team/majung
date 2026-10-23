# 픽업 서버 API

2026-10-23. 운전자 1명과 탑승자 1명이 픽업 한 건을 생성하고 종료하는 API다.
Java 17 대상 / Spring Boot 4.0.8 / Spring Data JPA / 로컬 H2 파일 DB를 사용한다.
차량 앱·탑승자 웹은 이 서버와 연결했다. `design/prototype`은 별도 예시 화면으로 남겼다.

외부 접속 준비는 [서버 배포 준비](server-deployment.md)를 함께 본다. deploy 프로필에서는 생성 요청에 `X-Majung-Creation-Key`가 필요하고 웹 개발 도구는 숨긴다. 기존 픽업 토큰의 참여·조회·상태 변경은 그대로다.
기본 API 요청 제한은 접속 IP별 1분에 전체 240회·변경 90회다. 초과 시 429 `RATE_LIMITED`와 `Retry-After: 60`을 반환하며 카운터 수용 한계는 503 `SERVER_BUSY`다.
`GET /api/v1/config`는 개발 도구 허용 여부만 반환한다. 키는 공개하지 않는다. 배포 프로필은 종료·만료 후 24시간 지난 행을 주기 삭제한다. 삭제 후 픽업 조회는 404다.

## 실행과 시험

프로젝트 루트에서 실행한다. 기존 Android 빌드와 분리한 Gradle 프로젝트이므로 `-p server`를 붙인다.

```sh
./gradlew -p server bootRun
```

기본 주소는 `http://127.0.0.1:8080`, 파일 DB는 `server/data/majung`이다.
이 상태로는 다른 휴대전화에서 접속할 수 없다. 외부 HTTPS 배포와 실제 기기 접속은 후속 작업이다.

다른 터미널에서 예시 데이터로 전체 흐름을 확인한다. Python 3 표준 라이브러리만 사용하고 토큰을 출력하지 않는다.

```sh
python3 tools/smoke_pickup_server.py
```

HTTP 통합 테스트와 재시작 검증:

```sh
./gradlew -p server test bootJar
python3 tools/check_server_restart.py
```

재시작 검증은 별도의 임시 파일 DB·로컬 포트로 서버를 두 번 실행한다. 개발 DB를 수정하지 않는다.
패키지 실행은 `cd server` 후 `java -jar build/libs/majung-server.jar`로 할 수 있다.
실행 디렉터리를 바꾸면 상대 경로 DB 위치도 바뀌므로 같은 디렉터리에서 실행한다.

## 권한과 토큰

이 버전은 계정 로그인이 아니라 픽업별 무작위 토큰으로 접근을 구분한다.

| 토큰 | 발급 시점 | 가능한 작업 |
| --- | --- | --- |
| `driverToken` | 픽업 생성 시 한 번 | 초대 재발급·참여 승인·차량 정보 전송·도착·종료 |
| `inviteToken` | 생성·초대 재발급 시 한 번 | 탑승자 참여 요청 1회 |
| `participantToken` | 참여 성공 시 한 번 | 해당 픽업 조회·자신의 준비 상태 변경·승인 후 차량 정보 조회 |

운전자·탑승자 요청은 `Authorization: Bearer <토큰>` 헤더를 쓴다.
초대 토큰만 참여 요청 JSON 본문으로 전송한다. 초대 링크는 fragment를 사용하고 진입 즉시 지운다.
토큰을 URL query·경로·Git·분석 도구·로그에 넣지 않는다.
서버 DB에는 토큰 원문 대신 SHA-256 해시를 저장한다. 원문은 발급 응답에서만 전달하며 서버에서 다시 조회할 수 없다.
웹은 해당 탭의 sessionStorage에 보관하고 종료·만료·접근 거절 때 지운다. localStorage에는 저장하지 않는다.

이 토큰은 계정 본인 인증이나 차량 신뢰성 검증이 아니다. 생성 API에는 아직 계정 인증·할당량·남용 방지가 없으므로
기본 바인딩을 로컬로 제한했다. 외부 공개 운영용 서버로 간주하지 않는다.

## 기본 흐름

1. 운전자 측이 픽업을 만든다. 생성 API는 `INVITING` 상태와 운전자·초대 토큰을 반환한다.
2. 탑승자가 초대 토큰으로 참여하면 `WAITING_APPROVAL`로 바뀐다. 같은 초대는 다시 사용할 수 없다.
3. 운전자가 반환된 참여자 ID를 확인·승인하면 `ACTIVE`가 된다.
4. 탑승자가 준비 상태를 보내고 운전자가 픽업 조회로 확인한다. 저장 성공은 차량 화면에 전달됐다는 뜻이 아니다.
5. 운전자 측이 도착을 보내면 `ARRIVED`가 된다. 공유는 유지된다.
6. 탑승을 확인한 뒤 완료하면 `COMPLETED`가 된다. 완료 전 언제든 취소하면 `CANCELLED`가 된다.

완료는 도착 이후에만 가능하다. 완료·취소 이후에는 새 준비 상태·위치·ETA를 반영하지 않는다.
같은 도착·완료·취소 재요청은 기존 결과를 반환하지만, 완료를 취소로 바꾸거나 취소를 다시 활성화하지 않는다.

## 엔드포인트

공통 경로는 `/api/v1/pickups`. JSON 요청에는 `Content-Type: application/json`을 지정한다.

| 메서드·경로 | 권한 | 요청·결과 |
| --- | --- | --- |
| `POST /` | 로컬 생성 | `place` 입력. 201과 `pickupId`, `driverToken`, `inviteToken`, `inviteExpiresAt`, `expiresAt` 반환 |
| `POST /{id}/participants` | 유효한 초대 | `inviteToken`, `displayName` 입력. 201과 `participantId`, `participantToken`, `status` 반환 |
| `POST /{id}/invitations` | 운전자 | 새 초대 토큰·만료 시각 반환. 이전 초대와 승인 대기 참여자 토큰 철회 |
| `POST /{id}/participants/{participantId}/approval` | 운전자 | 참여 승인. 픽업 상태 반환 |
| `GET /{id}` | 해당 운전자·탑승자 | 픽업 진행·준비 상태 조회. 탑승자는 승인 전 장소 좌표를 볼 수 없음 |
| `PUT /{id}/readiness` | 승인된 탑승자 | `readiness`, `sequence` 입력. 저장된 상태 반환 |
| `PUT /{id}/vehicle-state` | 공유 중 운전자 | 좌표·ETA·원본 시각·순번 입력. 수신 결과 반환 |
| `GET /{id}/vehicle-state` | 공유 중 운전자·승인된 탑승자 | 최신 차량 정보 조회 |
| `POST /{id}/arrival` | 운전자 | 도착 처리. 공유 유지 |
| `POST /{id}/completion` | 운전자 | 도착 후 픽업 완료·위치 조회 차단 |
| `POST /{id}/cancellation` | 운전자 | 픽업 취소·위치 조회 차단 |

표의 `POST /`는 접미사 없는 `POST /api/v1/pickups`를 뜻한다.

### 생성 요청

```json
{
  "place": {
    "label": "시연용 만남 장소",
    "latitude": 37.0,
    "longitude": 127.0
  }
}
```

장소명은 공백만으로 작성할 수 없고 최대 120자다. 위·경도 범위를 검사한다.
참여 이름은 공백만으로 작성할 수 없고 최대 12자다. 이름은 본인 인증 수단이 아니다.

### 준비 상태 요청

```json
{"readiness": "READY", "sequence": 1}
```

상태는 `WAITING`, `PREPARING`, `READY`. `sequence`는 해당 참여자의 준비 상태 변경 순번이며 1부터 증가시킨다.
같은 순번·같은 상태는 재전송으로 보고 성공 처리한다. 낮은 순번 또는 같은 순번의 다른 상태는 409로 거절한다.
재접속 시 서버의 `participant.readinessSequence`를 조회한 뒤 다음 순번으로 보낸다.
여러 탭이 같은 참여자 세션을 쓰는 경우 충돌 후 상태를 다시 조회해야 한다.

### 차량 정보 요청

```json
{
  "latitude": 37.01,
  "longitude": 127.01,
  "durationSeconds": 300,
  "sourceTime": "2026-10-23T03:00:00Z",
  "sequence": 1
}
```

좌표와 시각은 예시다. 실제 요청에서는 현재 수신한 원본 데이터의 UTC 시각을 보낸다.
차량 정보 순번은 준비 상태 순번과 별개다. 순번이 증가해도 원본 시각이 이전보다 과거면 거절한다.
동일 순번·동일 내용의 재전송은 수신 시각을 새로 찍지 않는다. 미래 시각 데이터는 400으로 거절하므로 차량과 서버의 시각 동기화가 필요하다.
`durationSeconds`는 0 이상의 초 단위 값이다. 0이 들어왔다는 이유만으로 서버가 도착이나 완료를 자동 처리하지 않는다.

조회 응답의 `freshness`는 `WAITING`, `FRESH`, `STALE`이다.
원본 시각 또는 서버 수신 시각이 기준 이상 오래됐으면 `STALE`로 표시하고 좌표·ETA는 null로 내려준다.
마지막 원본·수신 시각과 순번은 갱신 상태 설명을 위해 남긴다.

## 만료·철회·접근 차단

| 설정 | 현재 개발 기본값 | 의미 |
| --- | --- | --- |
| `majung.invite-ttl` | 15분 | 참여 요청과 운전자 승인 기한 |
| `majung.pickup-ttl` | 2시간 | 픽업의 최대 접근·변경 가능 시간 |
| `majung.vehicle-freshness` | 30초 | 원본·수신 시각 기준 위치·ETA 표시 유효 시간 |

모두 초기 개발값이다. 팀의 최종 정책이나 실제 SDK 갱신 주기를 측정해서 정한 수치가 아니다.
정확히 만료 시각에 도달하면 유효하지 않다. 초대 재발급은 픽업의 전체 만료 시간을 연장하지 않는다.

- 공유 시작 뒤 초대 재발급은 허용하지 않는다. 처음에는 한 명의 탑승자만 지원한다.
- 승인 전 차량 정보 조회는 403, 완료·취소·픽업 만료 후 조회·변경은 410이다.
- 다른 픽업의 토큰으로는 조회·승인·변경할 수 없다. 픽업 ID를 아는 것만으로 권한을 주지 않는다.
- 상태 조회는 종료 안내용으로 유지한다. 완료·취소·픽업 만료 후에는 약속 장소를 null로 반환한다.
- 픽업 만료는 조회 시 `EXPIRED`로 계산하며 상태 변경과 차량 정보 조회를 막는다. DB 정리 배치는 아직 없다.
- 초대 또는 승인 대기 기한이 지나면 상태 조회에서 `INVITE_EXPIRED`를 반환한다. `inviteExpiresAt`과 픽업 전체의 `expiresAt`을 구분한다. 운전자는 남은 픽업 시간 안에서 새 초대를 만들 수 있다.
- 완료·취소 시 저장된 차량 좌표·ETA를 비운다. 장소·표시 이름·토큰 해시 등 픽업 기록은 로컬 기본 실행에서는 DB에 남는다. deploy 프로필은 보관 기간이 지난 행을 별도 정리한다.

참여·승인·상태 변경·종료는 같은 픽업 행의 DB 잠금 안에서 처리한다.
동시에 같은 초대를 사용해도 참여 성공은 한 번뿐이고, 종료와 위치 전송이 겹쳐도 종료 후 위치가 다시 저장되지 않게 한다.

## 오류 응답

```json
{
  "code": "APPROVAL_REQUIRED",
  "message": "운전자 확인 후 이용할 수 있습니다.",
  "requestId": "서버가 생성한 요청 식별자"
}
```

요청 식별자는 `X-Request-Id`에도 내려준다. 전체 응답에 `Cache-Control: no-store`를 설정한다.
오류에 요청 본문·토큰·좌표·스택 트레이스를 포함하지 않는다.
예상하지 못한 실패는 요청 식별자와 예외 종류만 서버 로그에 남긴다.

| HTTP | 주요 코드 |
| --- | --- |
| 400 | `INVALID_REQUEST`, `FUTURE_SOURCE_TIME` |
| 401 | `UNAUTHORIZED` — Bearer 헤더 없음·형식 오류 |
| 403 | `FORBIDDEN`, `APPROVAL_REQUIRED` |
| 404 | `PICKUP_NOT_FOUND`, `PARTICIPANT_NOT_FOUND` |
| 409 | `INVALID_STATE`, `STALE_EVENT`, `ARRIVAL_REQUIRED`, `RETRY_REQUEST` |
| 410 | `PICKUP_ENDED`, `INVITE_UNAVAILABLE`, `INVITE_EXPIRED` |

## 확인 결과와 다음 단계

실제 HTTP 서버와 테스트 DB로 통합 테스트 13개를 실행했다. 기본 흐름, 권한·픽업 경계, 초대 만료·재사용·철회,
동시 참여, 중복·역순 이벤트, 오래된 정보 숨김, 취소·완료 이후 접근 차단과 입력 오류를 확인했다.
위치 전송과 취소가 동시에 실행돼도 종료 후 위치가 다시 남지 않는 것을 확인했다.
별도 파일 DB를 사용하는 패키지 서버 재시작 시험에서도 픽업·준비 상태와 운전자·탑승자 토큰 권한이 유지됐다.

다음은 탑승자 웹과 차량 앱을 이 API에 연결하는 작업이다.
실제 배포가 필요해지면 사용자가 보유한 GCP를 활용한다. 기존 프로젝트·리소스의 재사용 가능 여부를 먼저 확인하고,
사용할 프로젝트·VM·DB·도메인과 배포 구성을 정한다. 현재 GCP 리소스를 생성하거나 배포한 상태는 아니다.
휴대전화 페어링·QR·푸시·WebSocket/SSE·장소 변경·다중 탑승자·계정 로그인은 아직 구현하지 않았다.
첫 상태 수신은 인증된 GET 요청으로 시작할 수 있지만, 폴링 간격과 차량 전달 확인·실시간 통신은 다음 단계에서 정한다.
H2와 Hibernate 자동 스키마 갱신은 로컬 개발용이다. PostgreSQL 배포 프로필·초기 SQL·요청 제한·보관 정리는 준비했으며 실제 DB 연결·마이그레이션 운영·HTTPS·차량 인증은 후속 작업이다.

관련 설계: [상태·권한 초안](state-and-data.md), [화면 흐름](product-flow.md).

## 탑승자 웹에서 쓰는 차량 정보 유효 시간

`GET /api/v1/pickups/{id}/vehicle-state` 응답에는 `validForMillis`가 포함된다.
서버에서 계산한 남은 유효 시간(ms)이며, 정보가 없거나 만료됐다면 0이다. 웹은 왕복 시간을 차감하고 이 시간이 지나면 위치·ETA를 숨긴다.
서버와 함께 제공하는 웹의 사용 방법은 [탑승자 웹 연결](web-integration.md)을 참고한다.

## 차량 정보 무효화

`DELETE /api/v1/pickups/{id}/vehicle-state`에 운전자 토큰과 `{"sequence": 2}`처럼 증가 순서를 보낸다.
승인된 픽업의 차량 위치·ETA를 지우되 마지막 순서는 유지한다. 이전 위치 전송·이전 삭제 요청은 409로 거절하며 종료·만료 뒤에는 410을 반환한다.
삭제 응답은 `freshness=WAITING`, 위치·ETA null, `validForMillis=0`이며 사용된 순서를 반환한다. 같은 순서 재요청은 이미 삭제된 경우에만 허용한다.
앱은 SDK 해제·경로 변경·수신 정보 만료 때 이 요청을 사용한다. 실제 동작·통신 단절 시 한계는 [차량 정보 연동](vehicle-telemetry.md)에 정리했다.
