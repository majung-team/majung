# 마중

가족·지인 픽업에서 만날 장소와 준비 상태를 맞추는 차량용 앱 프로젝트.

현재 `probe` 모듈은 서비스 앱이 아니라 **NaviHelper 실행 확인용 Android 앱**이다.
위치·예상 시간이 실제로 들어오는지부터 확인한다. 서버·탑승자 웹은 아직 만들지 않았다.

## 구성

- `probe/`: NaviHelper 3.0.5 연결, 위치·목적지·경로 조회, 주행·도착 콜백 기록
- `design/prototype/`: 차량·운전자 휴대전화·탑승자 웹의 조작 가능한 화면 초안. 예시 데이터만 사용
- `docs/`: 실행 조건, 시험 기록
- `tools/`: 로그 수집·요약 도구
- `evidence/private/`: 로컬 실행 근거. 위치가 포함될 수 있어 Git에서 제외

팀 기획·검증 현황: [마중 노션](https://app.notion.com/p/401de2e676c28037a1f0d1dbe6cf1288)

## 화면 흐름 살펴보기

프로젝트 루트에서 실행한 뒤 `http://127.0.0.1:4173/`을 연다.

```sh
python3 -m http.server 4173 --bind 127.0.0.1 --directory design/prototype
```

실제 SDK·서버·지도와 연결되지 않은 초안이다. 상단의 ‘장면 설정’으로 주행·연결·도착 상황을 바꿔볼 수 있다.
휴대전화 영역은 ‘운전자 휴대전화’와 ‘탑승자’ 탭으로 구분했다.
사용 흐름은 `docs/product-flow.md`, 상태·권한·데이터 초안은 `docs/state-and-data.md`를 참고한다.

```sh
node --test design/prototype/model.test.js
```

## 개발 환경

Android Studio와 Android SDK API 34, Build Tools 36.0.0, JDK 25.
프로젝트의 Gradle daemon JVM은 25로 고정하고 앱의 Java 바이트코드 대상은 17로 설정했다.
Gradle 9.6.0 / Android Gradle Plugin 9.4.1. AGP의 내장 Kotlin 지원을 사용한다.
Pleos Connect 2.1.0 ARM64 에뮬레이터를 실행 대상으로 사용한다.

`local.properties`에는 각 개발자의 `sdk.dir`을 설정한다. 이 파일은 커밋하지 않는다.

```sh
./gradlew :probe:assembleDebug
adb install -r probe/build/outputs/apk/debug/probe-debug.apk
adb shell am start -n app.majung.probe/.MainActivity
```

Android Studio에서 이 폴더를 열어도 된다. SDK는 공식 Pleos Maven 저장소에서 받으며 저장소에 SDK 바이너리를 복사해 배포하지 않는다.

## 확인 순서

1. Pleos AVD를 cold boot로 실행한다. 일반 휴대전화 에뮬레이터와 구분한다.
2. 확인용 앱에서 `SDK 연결`을 누르고 성공·실패를 확인한다.
3. `현재 위치·목적지·경로 조회`를 한 번 누른다. 초기 요청도 연결 직후 한 번 실행된다.
4. 차량 내비게이션에서 경로 안내를 시작한다. 에뮬레이터로 식별되는 환경에는 공식 문서의 예제 목적지를 요청하는 버튼도 표시된다.
5. `driving` 이벤트의 `destination.duration`과 위치 수신 간격을 기록한다.
6. 내비게이션과 화면을 나눠 쓰거나 앱을 백그라운드로 보낸 후 이벤트가 계속 오는지 확인한다.
7. 경로 변경·취소·연결 해제를 확인한다. `SDK 연결`은 자동으로 누르지 않는다.

앱이 화면에서 사라져도 Application의 리스너는 유지하되, 화면 갱신용 타이머는 멈춘다.
OS가 프로세스를 종료했을 때까지 수신을 보장하는 구현은 아니다. 이 조건 자체가 검증 대상이다.
조회 요청을 자동으로 반복하지 않으므로 자동 수신 간격과 수동 조회 결과를 구분할 수 있다.

## 기록과 개인정보

이벤트는 기기 내부 `files/navi-events.jsonl`에 저장한다. 2MB를 넘으면 이전 파일 하나를 보관하고 새 파일에 기록한다.
좌표·주소가 포함될 수 있어 실제 사용자 위치로 시험한 로그는 공개하지 않는다. 앱은 별도 서버로 전송하지 않는다.

```sh
mkdir -p evidence/private
PROBE_USER=$(adb shell am get-current-user | tr -d '\r')
adb exec-out run-as app.majung.probe --user "$PROBE_USER" cat files/navi-events.jsonl > evidence/private/navi-events.jsonl
python3 tools/summarize_events.py evidence/private/navi-events.jsonl
```

`connected`, `location`, `driving`은 서로 다른 성공 조건이다. 연결 성공이나 빌드 성공을 위치·예상 시간 수신 성공으로 기록하지 않는다.
에뮬레이터의 실제 SDK 콜백도 실제 도로에서 검증했다는 의미는 아니다.
조회 요청으로 받은 위치 이벤트도 간격 요약에 포함된다. 이 값을 자동 위치 스트림의 갱신 주기라고 해석하지 않는다.

## 공식 자료

- [SDK 의존성](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk)
- [SDK 초기화](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release)
- [이벤트 리스너](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners)
- [에뮬레이터 설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)

실행 결과는 `docs/sdk-verification.md`와 노션의 `03 SDK 검증과 실행 결과`에 남긴다.
