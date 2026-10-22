# NaviHelper 실행 확인 — 2026-10-22

## 환경

- 프로젝트: `/Users/admin/Coding/majung`, `probe` 모듈
- macOS / Apple Silicon M3 Pro, RAM 18GB
- Pleos Connect 2.1.0 ARM64 / Android API 34, AVD RAM 4GB, cold boot
- NaviHelper 3.0.5, Gradle 9.6.0, AGP 9.4.1
- 실행 사용자: AAOS의 현재 사용자 10. 테스트와 로그 추출에도 같은 사용자를 지정
- 실차가 아니라 공식 Pleos 에뮬레이터에서 실행한 결과

## 결과

| 시험 | 결과 | 근거 |
| --- | --- | --- |
| 앱 APK 빌드 | 성공 | `:probe:assembleDebug` |
| Android lint | 오류 0건 | API/도구 버전 관련 경고는 별도 기록 |
| SDK 연결·현재 위치 조회 | 통과 | `connectAndReceiveLocation`, `OK (1 test)` |
| 공식 예제 목적지 요청 후 예상 시간 수신 | 통과 | `receiveEtaForOfficialExampleRoute`, duration=60초 |
| Activity가 stopped인 상태에서 위치 조회 응답 | 통과 | `receiveLocationAfterActivityStops`, 새 location 콜백 확인 |
| 같은 예제 좌표·빈 POI ID로 요청 후 예상 시간 수신 | 통과 | `poiMode=empty`, duration=2280초 |

4번의 개별 instrumentation 실행에서 각각 `OK (1 test)`를 확인했다.
빈 POI ID 시험은 예상 시간이 양수인지 자동 검증하고, 요청 좌표와 DestinationInfo 좌표의 일치는 이벤트 로그를 추가로 대조했다.

## 기획에 반영할 발견

공식 문서 예제 좌표는 `37.48544722, 127.03636666`이다.

- 예제의 `poiId=36khp37hh8gqcm5nqhe5`를 같이 넣으면 응답 목적지가 `37.41289678, 127.09383464`로 달랐다. 예상 시간은 60초, 거리는 50m였다.
- 같은 요청에서 `poiId=""`로 보내면 응답 목적지가 **요청한 좌표와 일치**했다. 예상 시간은 2280초(38분), 거리는 12000m였다.
- 따라서 이 에뮬레이터와 예제 요청에서는 빈 POI ID를 허용했고, 지정한 POI ID가 좌표와 다를 때 장소 해석에 영향을 줬다.
- 이를 모든 차종·지도 공급자·좌표 조합에서 보장되는 규칙으로 일반화하지 않는다. 다른 지도 사업자의 장소 ID도 호환된다고 가정하지 않는다.

웹에서 선택한 좌표를 차량 경로에 전달하는 방향은 이 결과를 바탕으로 다음 단계에서 설계할 수 있다.
화면에는 요청 직후 ‘장소 변경 완료’를 표시하지 않고, 응답 목적지·새 경로 데이터를 확인한 뒤 확정하는 처리가 필요하다.

## 아직 검증하지 않은 범위

- 실제 이동 중 위치·ETA 자동 갱신 주기 및 정확도
- OS의 프로세스 종료·장시간 백그라운드 이후의 수신 유지
- 경유지 추가·삭제·도착 이벤트와 순서 변화
- 주행 중 UI 허용 범위, 카테고리·DO 심사
- 실제 차량, 다양한 차종, 다양한 장소와 지도 사업자

stopped Activity에서 수동 조회 응답을 받은 결과는 OS 종료 이후의 자동 수신을 보장하지 않는다.
로그의 위치 이벤트 간격에는 수동 조회 응답이 포함돼 있어 자동 스트림의 주기로 해석하지 않는다.
SDK의 에뮬레이터 콜백을 받은 사실과 실제 도로에서 ETA 정확도를 검증한 사실도 구분한다.

## 재현

```sh
./gradlew :probe:assembleDebug :probe:assembleDebugAndroidTest
adb install -r probe/build/outputs/apk/debug/probe-debug.apk
adb install -r probe/build/outputs/apk/androidTest/debug/probe-debug-androidTest.apk
PROBE_USER=$(adb shell am get-current-user | tr -d '\r')
adb shell am instrument --user "$PROBE_USER" -w -r \
  -e class app.majung.probe.NaviConnectionTest#connectAndReceiveLocation \
  app.majung.probe.test/androidx.test.runner.AndroidJUnitRunner
```

`receiveEtaForOfficialExampleRoute`는 공개된 예제 목적지로 내비게이션 경로를 바꾼다. 에뮬레이터에서만 실행하도록 제한했다.
같은 시험에 `-e poiMode empty`를 추가하면 빈 POI ID 요청을 확인한다.
백그라운드 조회 시험 이름은 `receiveLocationAfterActivityStops`다.

## 원본 근거

`evidence/private/`에 빌드 로그, 네 번의 instrumentation 결과, `navi-events.jsonl`을 저장했다.
원본 로그는 좌표·주소를 포함할 수 있어 Git에서 제외했다. 이번 시험은 에뮬레이터와 공개 예제 좌표로 실행했다.
로그에는 ISO UTC 시각과 Android monotonic 시각을 함께 남긴다. 에뮬레이터 logcat의 표시 시간대와 혼동하지 않는다.

시스템 이미지 SHA-1은 공식 배포 XML의 `24d28eb2f18eba5e87c861a7eb2bdccebb049419`와 일치했다.
공식 ARM 이미지의 패키지 메타데이터에 맞춰 `system-images/android-34/android-automotive/arm64-v8a`에 설치했다.
