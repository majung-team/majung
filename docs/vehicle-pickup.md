# 내비게이션에서 선택한 장소로 픽업 만들기

2026-10-23. #5의 장소 생성 단계다. 위치·ETA 전송은 [차량 정보 연동](vehicle-telemetry.md)에서 이어서 확인했다. `vehicle` 모듈을 새로 만들었고 기존 `probe`는 SDK 진단용으로 유지한다.

## 이번에 연결한 부분

내비게이션에서 목적지를 선택하고 차량 앱에서 그 장소를 확인한다.
‘이 장소로 픽업 만들기’를 누르면 `DestinationInfo`의 이름·위도·경도를 서버 생성 API에 전달한다.
좌표를 직접 입력하는 칸은 없다. 현재 위치를 만날 장소로 대신 쓰지도 않는다.

픽업 생성 후에는 참여자 승인, 준비 상태 조회, 도착·탑승 완료·취소를 차량 앱에서 처리할 수 있다.
픽업 장소는 생성 시점의 장소로 고정한다. 이후 내비게이션 목적지가 바뀌어도 기존 픽업 장소를 몰래 바꾸지 않는다.
장소 변경 API와 사용자 확인 절차는 아직 없다.

현재 차량 앱은 **주차 상태에서 연결을 시험하는 개발 화면**이다.
주행 중 조작 허용, 차량 카테고리·DO 심사, 실차 검증을 마친 앱이 아니다.

## 실행

로컬 서버를 먼저 실행한다.

```sh
./gradlew -p server bootRun
./gradlew :vehicle:assembleDebug :vehicle:assembleDebugAndroidTest
adb install -r vehicle/build/outputs/apk/debug/vehicle-debug.apk
adb install -r vehicle/build/outputs/apk/androidTest/debug/vehicle-debug-androidTest.apk
```

공식 Pleos 에뮬레이터에서 ‘마중 차량 개발’을 연다.

### Android Studio 밖의 별도 창으로 실행

macOS에서는 **Android Studio → Settings → Tools → Emulator**에서
**Launch in the Running Devices tool window**를 해제한다.
실행 중인 에뮬레이터를 종료한 뒤 Device Manager에서 Pleos AVD를 다시 실행하면 별도 창으로 열린다.
화면을 어디에 띄우는지 정하는 설정이며, 앱 빌드 설정은 아니다.
[Android 공식 안내](https://developer.android.com/studio/run/emulator-launch-separate-window)

APK 빌드만으로 에뮬레이터와 앱이 실행되지는 않는다. AVD를 켠 뒤 Android Studio의 실행 모듈을 `vehicle`로 선택하고 Run하거나,
위 명령으로 설치한 뒤 에뮬레이터의 앱 목록에서 ‘마중 차량 개발’을 연다.

### 픽업 한 건 확인

1. 내비게이션에서 목적지를 고른다.
2. 마중에서 ‘내비게이션 연결’을 누르고 장소 이름을 확인한다.
3. 조회 후 30초가 지나거나 화면을 떠났다면 목적지를 다시 확인한다.
4. ‘이 장소로 픽업 만들기’를 누른다.
5. 개발용 초대 링크를 복사해 **이 Mac의 브라우저**에서 연다.
6. 탑승자 이름으로 참여한 뒤 차량 앱에서 참여자를 확인하고 승인한다.
7. 탑승자 웹에서 준비 완료를 선택하면 차량 앱이 서버 조회로 상태를 확인한다.
8. 주차 후 ‘도착 알리기 · 공유 유지’를 누르고 확인한다. 탑승 전에는 위치 공유를 유지한다.
9. 실제 탑승을 확인한 뒤 ‘탑승 확인 · 픽업 완료’를 누르고 확인한다. 완료 후에는 위치 공유가 종료된다.
10. 완료 전 취소할 수도 있다. 종료 뒤 ‘새 픽업 준비’로 다음 약속을 만들 수 있다.

확인 창에서 ‘돌아가기’를 누르면 기존 상태를 유지한다. 자세한 동작과 최신 시험 결과는 [도착·탑승 완료](vehicle-completion.md)에 있다.

개발 APK는 Android 에뮬레이터의 호스트 주소 `10.0.2.2:8080`으로 접속한다.
이 주소는 GCP나 외부 서버의 IP가 아니다. 개발용 초대 링크도 같은 Mac용 `127.0.0.1:8080`을 쓴다.
실제 휴대전화에서는 이 주소로 접속할 수 없다.

HTTP 예외는 debug 빌드의 `10.0.2.2`에만 두었다. release 빌드는 서버 주소를 비워두었고 HTTP 예외도 포함하지 않는다.
인증·HTTPS·운영 저장소를 구성하기 전 외부에 공개하지 않는다.

## 오래된 정보와 실패 처리

- SDK 연결 해제, 목적지 변경·경로 탐색 시작·취소·오류 때 이전 선택 장소 무효화
- SDK 연결 세대가 달라지면 늦게 도착한 이전 연결의 콜백 무시
- 경로 탐색 중에는 생성용 목적지를 받지 않고, 완료 콜백 후 다시 조회
- 목적지 조회 후 30초가 지나면 생성 버튼 비활성화
- 서버 요청은 별도 작업 스레드에서 실행하며, 생성 중 중복 클릭 차단
- 생성 응답이 끊기면 이미 서버에 만들어졌을 가능성이 있어 자동 재생성하지 않음
- 서버에서 생성한 픽업의 장소는 새 SDK 콜백으로 덮어쓰지 않음
- 승인·준비 상태는 서버 응답으로 확인하고 연결 실패 때 이전 조회 결과를 최신 결과로 표시하지 않음
- 종료·접근 거절 때 운전자·초대 토큰 제거. 토큰은 파일·저장된 Activity 상태·로그에 남기지 않음

서버의 생성 요청 멱등성·세션 재발급은 아직 없다. 생성 결과 미확인 상태의 복구와 프로세스 종료 후 픽업 복원은 후속 작업이다.
현재 토큰은 Application 메모리에서만 유지한다. 화면 재생성은 유지하지만 앱 프로세스가 종료되면 복원하지 않는다.

## 확인 결과

| 구분 | 확인한 내용 | 결과 |
| --- | --- | --- |
| 빌드 | debug APK·시험 APK·release APK | 성공. release 빌드는 배포 승인이나 운영 연결을 뜻하지 않음 |
| 단위 시험 | 목적지 값 검증, 30초 만료, 무효화 후 이전 장소 사용 차단 | 3개 통과 |
| 공식 에뮬레이터 + 로컬 HTTP | 실제 목적지 콜백 → 생성 → 참여 → 승인 → 준비 완료 조회 → 취소 | 통과 |
| 값 대조 | 승인 후 탑승자 응답의 이름·좌표와 SDK에서 받은 장소 비교 | 일치 |
| 권한 | 승인 전·취소 후 탑승자 응답에서 장소 비공개 | 통과 |
| Android lint | 오류 0건 | SDK·도구 버전, 국제화 관련 경고는 남음 |

통합 시험은 공식 Pleos Connect 2.1.0 ARM64 / API 34 / 사용자 10에서 실행했다.
내비게이션 경로는 기존 SDK 시험의 공개 예제 좌표·빈 POI ID로 설정했다.
응답을 흉내 낸 콜백이 아니라 실제 NaviHelper 콜백과 실제 로컬 API를 사용했다. 실차나 실제 휴대전화 시험은 아니다.

```sh
./gradlew :vehicle:testDebugUnitTest :vehicle:lintDebug
VEHICLE_USER=$(adb shell am get-current-user | tr -d '\r')
adb shell am instrument --user "$VEHICLE_USER" -w -r \
  -e class app.majung.vehicle.VehiclePickupTest \
  app.majung.vehicle.test/androidx.test.runner.AndroidJUnitRunner
```

통합 시험에는 선택된 내비게이션 목적지와 실행 중인 로컬 서버가 필요하다.
`probe`의 `receiveEtaForOfficialExampleRoute` 시험은 에뮬레이터의 경로를 공개 예제로 바꾼다.
개인 목적지나 실차에서 시험을 실행하지 않는다.

## 아직 남은 연결

- 지도 표시
- 실제 휴대전화·외부 HTTPS 연결 및 시연 영상
- 목적지 변경·다중 기기·계정 인증·프로세스 종료 후 복원

이 문서의 첫 장소 연결 이후 SDK 위치·ETA 전송을 추가했다. 현재 검증 범위와 실행 캡처는 [차량 정보 연동](vehicle-telemetry.md)을 참고한다.

근거: [Pleos 목적지 설정·조회 공식 안내](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/destinations),
로컬 캐시의 NaviHelper 3.0.5·navi-data 3.0.4 공식 소스. 요청 성공과 경로 탐색 완료는 서로 구분한다.
