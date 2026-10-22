# 차량 SDK 위치·ETA를 탑승자 웹으로 전달

2026-10-23. #5의 장소 생성에 이어 차량 정보 전송을 연결했다. 외부 배포와 실제 휴대전화 시험은 아직 하지 않았다.

## 데이터가 지나가는 경로

1. 차량 앱이 NaviHelper의 `CurrentLocationInfo`에서 현재 위치를 받는다.
2. `DestinationInfo`에서 목적지 좌표와 `remainTime`을 받는다. 이 SDK 버전의 단위는 초다.
3. 목적지가 생성한 픽업 장소와 일치하고 두 응답 모두 최근 10초 안에 수신됐는지 확인한다.
4. 승인된 픽업에서만 `PUT /api/v1/pickups/{id}/vehicle-state`로 전송한다.
5. 탑승자 웹이 서버를 조회해 위치와 ETA를 표시한다.

SDK가 제공한 남은 시간을 사용한다. 임의로 만든 ETA나 웹 화면의 예시 데이터를 대신 넣지 않는다.
좌표는 현재 위치, 목적지 좌표는 이번 약속과 경로가 같은지 확인하는 데 쓴다. 둘을 바꿔 보내지 않는다.

차량 화면이 열린 동안 SDK 정보를 5초마다 조회한다. 전송은 값이 새로 들어왔을 때 최대 3초에 한 번으로 제한한다.
서버·공유 상태는 3초마다 확인한다. 실제 SDK 자동 콜백이 들어오는 경우에도 같은 검사를 거친다.
이 조회 주기를 SDK 자체의 자동 갱신 주기나 GPS 정확도로 해석하지 않는다.

## 오래된 값 처리

- 위치 또는 ETA가 하나라도 없거나 10초가 지나면 묶어서 전송하지 않는다.
- 새 위치를 받아도 예전 ETA의 수신 시간을 새 시각으로 바꾸지 않는다.
- `sourceTime`에는 두 SDK 응답 중 먼저 받은 시각을 보낸다.
- 경로 탐색·목적지 변경·취소·오류·SDK 연결 해제 때 위치와 ETA를 함께 무효화한다.
- 다른 목적지로 경로가 바뀌면 기존 픽업의 ETA로 전송하지 않는다.
- 차량 화면을 떠날 때 SDK 연결과 공유를 멈춘다. 백그라운드 위치 공유를 구현한 버전이 아니다.

SDK의 이 두 데이터에는 GPS 측정 시각이 없다. 현재 `sourceTime`은 **앱이 SDK 응답을 받은 UTC 시각**이며 GPS 취득 시각을 검증했다는 뜻은 아니다.
앱에서는 단조 증가 시간으로 수신 경과를 검사한다. 차량 시스템 시계가 서버보다 앞서면 서버가 전송을 거절하며, 임의의 과거 시각으로 바꾸어 보내지는 않는다.

## 서버에 남은 위치 지우기

SDK 연결 해제 등을 감지하면 다음 요청을 보낸다.

```http
DELETE /api/v1/pickups/{id}/vehicle-state
Authorization: Bearer <driverToken>
Content-Type: application/json

{"sequence": 2}
```

생성·승인된 운전자만 호출할 수 있다. 삭제 요청과 위치 전송은 같은 증가 순서를 쓴다.
서버는 위치·ETA를 지운 뒤에도 마지막 순서를 보관한다. 늦게 도착한 이전 전송이나 이전 삭제 요청으로 최신 상태를 덮지 못하게 하기 위해서다.
같은 순서의 삭제 재요청은 이미 지워진 상태일 때만 허용한다.

웹은 다음 조회 때 위치·ETA가 없는 응답을 받고 화면에서 숨긴다. SDK 이벤트와 동시에 모든 화면에서 사라진다는 뜻은 아니다.
요청 처리 중인 전송이 있다면 끝난 뒤 삭제를 전달한다. 실패 시 재시도 간격은 3초다.
네트워크가 끊겨 삭제 요청 자체를 전달하지 못하면 서버의 기존 30초 만료와 웹의 잔여 유효 시간이 마지막 보호 장치다.

서버의 종료·만료·권한 검사도 그대로 적용한다. 취소된 픽업으로 늦게 도착한 위치를 다시 공유할 수 없다.

## 차량 화면 갱신 수정

주기 조회 때 화면이 깜빡인다는 제보(#9)를 받아 다음 부분을 수정했다.

- 값이 같은 TextView에는 텍스트를 다시 대입하지 않음
- 조회 중 문구를 본문에 넣었다 빼는 대신 한 줄짜리 연결 상태 영역 사용
- 조회와 생성·승인·취소의 처리 상태 분리. 단순 조회 중에 버튼을 반복 비활성화하지 않음
- 변경 요청을 시작한 뒤 도착한 이전 조회 결과는 화면 상태에 반영하지 않음

1초 단위의 수신 만료 확인은 유지한다. 이를 위해 매번 전체 화면을 새로 만드는 것은 아니다.
에뮬레이터에서 6초 동안 실제 조회를 반복하며 본문 높이·취소 버튼 위치·버튼 활성 상태가 유지되는지 검사했다.
프레임 타임이나 GPU 성능을 측정한 시험은 아니므로 모든 버벅임의 원인을 확정하거나 없앴다고 보지는 않는다.

## 시험 결과

| 시험 | 결과 |
| --- | --- |
| 서버 HTTP 시험 | 14개 통과. 무효화 권한·중복·이전 순서 거절 포함 |
| 차량 단위 시험 | 8개 통과. 목적지 3개·위치/ETA 묶음과 만료 5개 |
| 웹 모델 시험 | 기존 초안 8개·웹 5개, 합계 13개 통과 |
| 실제 SDK·로컬 HTTP 통합 | SDK 값과 API 값 일치, 연결 해제 후 숨김, 재연결 후 새 정보 전송, 준비 상태 조회·취소 확인 |
| 차량 화면 | 반복 조회 중 본문 높이·버튼 위치·활성 상태 유지 확인 |
| 탑승자 브라우저 | 초대 참여·승인 후 SDK 위치와 ETA 표시, 준비 완료 저장, SDK 해제 후 이전 정보 비표시 확인 |

공식 Pleos Connect 2.1.0 ARM64 / API 34 / 사용자 10과 로컬 서버에서 시험했다.
내비게이션은 공개 예제 목적지로 설정했다. 아래 화면의 위치와 ETA는 에뮬레이터에서 받은 값이다.
사람이 실제로 이동한 위치나 실차에서 검증한 도착 시간이 아니다.

![SDK에서 받은 위치와 ETA를 탑승자 웹에 표시](media/sdk-web-live.png)

![SDK 연결을 해제한 뒤 이전 위치와 ETA 비표시](media/sdk-web-hidden.png)

## 재현

[차량 앱 실행 안내](vehicle-pickup.md)대로 서버와 에뮬레이터를 준비한 뒤 실행한다.

```sh
./gradlew -p server test bootJar
./gradlew :vehicle:assembleDebug :vehicle:assembleDebugAndroidTest :vehicle:testDebugUnitTest :vehicle:lintDebug
adb install -r vehicle/build/outputs/apk/debug/vehicle-debug.apk
adb install -r vehicle/build/outputs/apk/androidTest/debug/vehicle-debug-androidTest.apk
VEHICLE_USER=$(adb shell am get-current-user | tr -d '\r')
adb shell am instrument --user "$VEHICLE_USER" -w -r \
  -e class app.majung.vehicle.VehiclePickupTest \
  app.majung.vehicle.test/androidx.test.runner.AndroidJUnitRunner
```

선택된 내비게이션 목적지가 먼저 있어야 한다. 시험은 실제 SDK 응답을 사용하며 목적지가 없으면 실패한다.
브라우저 캡처용 `browserEvidence=true`는 별도 수동 확인 모드다. 에뮬레이터 내부에 일회 초대만 임시로 전달하고 시험 종료 시 지운다.
운전자·참여자 토큰은 캡처나 로그에 출력하지 않는다. 이 모드는 브라우저 참여·확인 단계가 없으면 시간 초과한다.

실제 휴대전화·실차, 시스템 시계가 크게 어긋난 환경, 다양한 장소·지도 사업자, 장시간 통신 장애와 OS 종료 후 복구는 후속 검증이다.
차량 화면의 도착·탑승 완료도 연결했다. [완료 처리와 검증](vehicle-completion.md)을 참고한다. 지도 표시는 아직 없다.

SDK 근거: [공식 NaviHelper 자료형 안내](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/driving-info/entry),
로컬 공식 navi-data 3.0.4 소스의 `CurrentLocationInfo`·`DestinationInfo` 정의와 에뮬레이터 수신 결과.
