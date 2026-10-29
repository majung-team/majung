# 픽업 시연 · 2026-10-29

https://github.com/user-attachments/assets/e181fcc1-599f-47e9-ac96-98135dae2f24

약 41초의 무음 영상이다. GitHub에서 위 영상의 재생 버튼을 누르면 볼 수 있다.
[MP4 다운로드](media/pickup-demo-20261029.mp4)

## 확인한 흐름

1. 초대 링크를 열고 가입·이름 입력·운전자 승인 없이 참여
2. SDK에서 받은 도착 예정 시간을 웹에 표시
3. 웹에서 준비 완료 선택 → 차량 앱 수신 → 웹의 전달 상태 갱신
4. 차량에서 수동으로 도착 알림
5. 차량에서 탑승 확인·완료 → 웹 위치·ETA 비표시

## 촬영 조건

- 차량: 공식 Pleos Connect 2.1 / ARM64 / API 34 / NaviHelper 3.0.5, 별도 에뮬레이터 사용자 10.
- 앱 화면은 현재 main의 서비스 화면이다. 녹화 시험 코드는 [vehicle PR #81](https://github.com/majung-team/majung-vehicle-app/pull/81)에 있다. 웹은 `ed450be`, 서버는 `f186361` 기준이다.
- 픽업은 **로컬 API로 미리 만든 시험 데이터**다. 차량 등록·운전자 로그인·차량 화면에서의 픽업 생성은 이번 영상에 포함하지 않았다. 위치 공유 안내 동의도 시험 준비에서 설정했다.
- Android 화면은 연속 녹화했고 웹은 실제 브라우저의 단계별 화면 7장을 수집했다. 390px 화면은 데스크톱 브라우저의 화면 크기 설정이며 실제 휴대전화가 아니다.
- 양쪽 화면을 호스트 시각으로 맞추고 대기 구간을 잘랐다. 재생 속도를 높이거나 SDK 응답을 합성하지 않았다. 웹 화면 사이의 움직임은 녹화되지 않아 정지 화면으로 보인다.
- 정차한 에뮬레이터다. ETA의 교통량 반영·주행 중 감소·자동 도착 판정은 이 영상으로 검증하지 않았다. 도착은 수동 처리했다.
- 실제 휴대전화·실차·공개 HTTPS·지도·장소 제안·푸시·여러 탑승자 대기열 시연은 포함하지 않았다.
- 토큰·초대 링크 원문·QR·개인정보는 영상에 넣지 않았다. 임시 초대 파일은 종료 후 지웠다.

## 다시 촬영하기

[로컬 실행 안내](vehicle-pickup.md)에 따라 격리된 에뮬레이터와 로컬 서버를 준비한다.
이 시험은 로컬 API(`http://10.0.2.2:8080`)로 빌드한 DEBUG 앱에서만 실행된다.
웹 개발 서버는 4173, API는 8080을 사용한다. 위치 권한은 시험 에뮬레이터에서 미리 허용한다.
사용 중인 차량 앱의 연결 정보를 덮어쓰므로 **평소 쓰는 AVD 대신 별도 시험 AVD**를 사용한다.

```sh
adb -s emulator-5556 shell am instrument --user 10 -w \
  -e class app.majung.vehicle.VehicleDemoRecordingTest -e recordDemo true \
  app.majung.vehicle.test/androidx.test.runner.AndroidJUnitRunner
```

다른 터미널에서 연결 도구를 실행한다. 기기 번호와 사용자 번호는 자기 환경에 맞춘다.

```sh
python3 tools/bridge_demo_recording.py \
  --adb "$ANDROID_SDK_ROOT/platform-tools/adb" --serial emulator-5556 --user 10
```

단계는 `evidence/private/demo-recording/phase.txt`에, 시험 초대는 같은 폴더의 `invitation.json`에만 기록된다.
초대 파일은 브라우저에서 여는 데만 사용하고 채팅·로그·캡처에 출력하지 않는다.

- `ready` 뒤 `create.go`: 미리 만든 시험 픽업의 링크를 브라우저에 전달한다. UI의 생성 버튼을 누르는 단계는 아니다.
- 브라우저에서 참여 후 차량의 공유 서비스가 시작되고 실제 SDK ETA가 올라온다.
- 브라우저에서 준비 완료를 누르고 `passenger-ready`를 확인한다.
- `arrive.go`: 실제 차량 UI의 도착 버튼과 확인 창을 누른다.
- `complete.go`: 탑승 완료 버튼과 확인 창을 누른다.
- 종료 화면을 수집한 뒤 `finish.go`, 연결 도구에는 `stop` 파일을 전달한다.

각 신호 파일은 위 로컬 폴더에 빈 파일로 만든다. 사람 조작 대기는 최대 180초다.
연결 도구의 `--serial`은 필수이며, 다른 에뮬레이터에 명령이 전달되지 않도록 확인한다.
Android 녹화는 `adb -s emulator-5556 shell screenrecord`로 별도 진행한다.

편집에는 Python 3.9 이상·Pillow·FFmpeg가 필요하다. 원본 `vehicle.mp4`, `web/`의 PNG,
각 파일의 호스트 밀리초 시각을 담은 `timeline.json`, 녹화 시작 시각(초)의 `device-start.txt`를 준비한다.
현재 편집 도구의 차량 크롭은 2560×1440 원본, 웹은 390×844 기준이다.

```sh
python3 tools/render_pickup_demo.py \
  --capture-dir evidence/private/demo-recording \
  --ffmpeg /path/to/ffmpeg --font /path/to/korean-font \
  --output docs/media/pickup-demo-20261029.mp4
```

## 결과

- `VehicleDemoRecordingTest`: **OK (1 test), 175.659초**.
- 편집본: 1920×1080, H.264, 24fps, 약 40.7초. 전체 디코딩과 각 단계의 대표 프레임 확인.
- [10월 23일 초기 영상](https://github.com/user-attachments/assets/3090606c-4212-4439-8da4-d6c45c02e819)은 이전 이름 입력·승인 방식의 기록이며 현재 사용 순서와 다르다.

실제 휴대전화·공개 서버에서의 전체 흐름은 [함께 테스트하기](team-testing.md) 순서로 별도 확인한다.
