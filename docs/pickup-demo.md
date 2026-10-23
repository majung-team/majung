# 픽업 전체 흐름 시연 · 2026-10-23

https://github.com/user-attachments/assets/3090606c-4212-4439-8da4-d6c45c02e819

[MP4 원본 다운로드](https://github.com/majung-team/majung/raw/refs/heads/main/docs/media/pickup-demo.mp4)

차량에서 픽업을 만들고 탑승자가 참여한 뒤, 승인·준비 완료·도착·탑승 완료까지 진행한 약 45초의 무음 편집본이다.
실제 앱의 버튼·확인 창과 실제 SDK·로컬 서버를 사용했다. 예전의 캡처 2장 반복 GIF와는 다른 자료다.

## 영상에서 확인할 것

1. 차량 내비게이션의 선택 목적지로 픽업 생성. 좌표 입력 없음.
2. 초대 링크를 연 탑승자가 이름으로 참여. 승인 전 장소·위치 비공개.
3. 차량에서 참여자를 확인하고 승인.
4. SDK의 위치·남은 시간이 웹에 표시되고 준비 완료가 차량에 반영.
5. 운전자가 도착을 직접 확인. 탑승 전까지 공유 유지.
6. 실제 탑승을 확인하고 완료. 웹의 위치·ETA 비표시와 차량 접근 정보 정리.

## 녹화 범위

- 공식 Pleos Connect 2.1.0 ARM64 / API 34 / 사용자 10 / NaviHelper 3.0.5.
- 차량은 Android screenrecord로 연속 녹화했다. 웹은 실제 탭 화면 179장을 시간별로 수집해 영상화했다. 낮은 수집 빈도와 구간별 간격 때문에 웹 화면은 일부 움직임이 매끄럽지 않을 수 있다.
- 호스트 시각으로 양쪽 화면을 배치하고 대기 구간만 잘랐다. 재생 속도를 높이거나 가짜 SDK 응답·화면을 넣지 않았다. 편집본은 1920×1080 H.264 MP4, 출력 프레임률은 24fps다. 출력 프레임률이 원본 화면 수집 빈도를 뜻하지는 않는다.
- 정차한 에뮬레이터에서 공개 예제 목적지와 ‘시연 탑승자’를 사용했다. 주행과 ETA 감소는 재현하지 않았다. 도착은 수동 확인이므로 그 뒤에도 SDK의 16분 ETA가 유지되는 장면이 있다.
- 실차·실제 휴대전화·GCP·외부 HTTPS 시연이 아니다. 지도·QR·휴대전화 페어링·장소 변경·푸시도 이번 영상에 포함하지 않았다.
- 토큰·초대 링크 원문·개인 위치 로그·다른 앱 화면·음성은 영상에 포함하지 않았다. 임시 초대 파일은 녹화 종료 후 지웠다.

## 다시 촬영할 때

[차량 실행 안내](vehicle-pickup.md)대로 로컬 서버·에뮬레이터·공개 예제 경로를 준비한다.
빌드·설치 후 다음 opt-in 시험을 실행한다. 기본 시험에서는 건너뛰므로 사람의 브라우저 조작을 기다리지 않는다.

```sh
adb shell am instrument --user 10 -w -r \
  -e class app.majung.vehicle.VehicleDemoRecordingTest -e recordDemo true \
  app.majung.vehicle.test/androidx.test.runner.AndroidJUnitRunner
```

다른 터미널에서 로컬 파일 연결 도구를 실행한다. ADB 경로와 사용자 번호는 자신의 환경에 맞춘다.

```sh
python3 tools/bridge_demo_recording.py --adb "$ANDROID_SDK_ROOT/platform-tools/adb" --user 10
```

이 도구는 `evidence/private/demo-recording/phase.txt`로 단계만 전달한다.
초대 링크는 같은 폴더의 임시 `invitation.json`에만 저장하며 터미널·로그·문서에 출력하지 않는다. 탑승자 브라우저에서 이 링크로 참여한다.

- `create.go`: 차량의 생성 버튼 실행 → 초대 발급.
- 브라우저에서 참여 후 `approve.go`: 차량 승인 버튼과 확인 창 실행.
- 브라우저에서 준비 완료를 누른 뒤 `arrive.go`: 차량 도착 확인.
- `complete.go`: 차량 탑승 확인·완료.
- 종료 화면을 촬영한 뒤 `finish.go`: 시험 종료. 이후 `stop` 파일로 연결 도구도 종료한다.

신호 파일은 위 로컬 폴더에 빈 파일로 만든다. 각 대기 단계는 180초 뒤 실패하며, 종료 시 임시 초대를 정리한다.
Android 녹화는 `adb shell screenrecord`로 따로 시작·종료하고 파일이 완전히 저장된 뒤 가져온다.
웹 화면 수집과 초대 참여는 실제 브라우저에서 진행해야 한다. 연결 도구가 웹 UI나 SDK 응답을 대신 생성하지 않는다.

편집 도구는 Python 3.9 이상·Pillow·FFmpeg를 사용한다. 원본 `vehicle.mp4`, 웹 PNG들과 밀리초 시각을 담은 `timeline.json`,
차량 녹화 시작 호스트 시각(초)의 `device-start.txt`가 필요하다. 이번 촬영의 원본은 Git에서 제외한 로컬 폴더에 보관했다.

```sh
python3 tools/render_pickup_demo.py \
  --capture-dir evidence/private/demo-recording \
  --ffmpeg /path/to/ffmpeg \
  --font /path/to/korean-font \
  --output docs/media/pickup-demo.mp4
```

기록 형식은 편집 스크립트의 `frames`·`events` 사용 부분을 참고한다. 화면 크기·문구·앱 배치가 달라지면 크롭 영역과 자막을 확인하고 다시 촬영한다.

## 검증

녹화 세션의 `pickupWithPassengerBrowser`가 실제 UI로 진행 후 `OK (1 test)`로 종료됐다.
차량 단위 시험 8개와 lint도 통과했다. 영상 전체 디코딩과 단계별 대표 프레임을 확인했다.
다음 작업은 외부 배포 준비와 실제 휴대전화 시험이며, 이 영상만으로 #7의 모든 완료 기준을 충족한 것은 아니다.
