<div align="center">
  <img src="docs/media/majung-icon.png" width="72" alt="마중 아이콘" />
  <h1>마중 (Majung)</h1>
  <p>차량 내비게이션의 픽업 장소·실시간 위치·예상 도착 시간(ETA)과 탑승자의 준비 상태를 함께 공유해, 마중 가는 과정의 불필요한 연락을 줄이는 서비스입니다.</p>
  <p>
    <a href="https://majung.cloud/account">모바일 웹</a> ·
    <a href="https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.33-preview.20261112">차량 APK (0.2.33-preview)</a> ·
    <a href="https://github.com/majung-team/majung/raw/main/docs/media/pickup-demo.mp4">시연 영상 (1분 36초)</a> ·
    <a href="docs/README.md">전체 문서</a>
  </p>
</div>

---

## 1. 시연 및 주요 기능

[![마중 픽업 및 내비 연동 시연 영상 미리보기](docs/media/pickup-demo-poster.png)](https://github.com/majung-team/majung/raw/main/docs/media/pickup-demo.mp4)

> [시연 영상 보기 (약 1분 36초 · TTS 음성 포함)](https://github.com/majung-team/majung/raw/main/docs/media/pickup-demo.mp4) · [촬영 환경 및 장면 안내](docs/pickup-demo.md)  
> *(차량 앱 `0.2.29-preview` + 운영 서버 `majung.cloud` 기준 Pleos 에뮬레이터 시연 영상)*

- **내비게이션 경유지 연동**: 기존 목적지를 유지한 채 픽업 장소를 경유지로 추가하고, 장소 변경 제안 수락 시 경로를 바로 반영합니다.
- **실시간 위치 · ETA 및 준비 상태 공유**: 탑승자는 모바일 웹에서 차량 위치·ETA를 보고 `[준비 중]` / `[탑승 준비 완료]`와 메시지를 보낼 수 있습니다.
- **주행 안전 화면 · 음성 안내(TTS)**: 운전 중 조작을 제한(`CarUxRestrictions`)하고, 내비 화면 위에서도 신규 요청·탑승자 메시지를 알림과 음성으로 안내합니다.
- **QR 간편 연결 · 개인정보 보호**: 차량 QR 스캔만으로 계정을 연결하며, 반경 100m 자동 도착 감지 후 픽업이 끝나면 위치 공유를 즉시 중단합니다.

---

## 2. 실행 방법 및 테스트 환경

- **차량 APK 위치**: [`majung-vehicle-app/apk/마중_마중_release.apk`](https://github.com/majung-team/majung-vehicle-app/blob/main/apk/%EB%A7%88%EC%A4%91_%EB%A7%88%EC%A4%91_release.apk) · [GitHub Releases (`0.2.33-preview`)](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.33-preview.20261112)
- **실행 순서**:
  1. Pleos Connect 에뮬레이터에 `adb install -r 마중_마중_release.apk`로 차량 앱을 설치·실행합니다.
  2. 휴대전화로 차량 QR을 스캔해 [`https://majung.cloud/account`](https://majung.cloud/account)에 운전자 계정을 연결합니다.
  3. 다른 카카오 계정(탑승자) 또는 초대 링크로 참여해 픽업 요청·수락·내비 연동 흐름을 확인합니다.

| 항목 | 기준 |
| --- | --- |
| **Pleos 에뮬레이터 · 화면 규격** | `Pleos Connect v2.1.0` (`Android 14 / API 34`, `arm64-v8a`) · `Pleos_Connect_v2_0` · **`2560 × 1440 px` (`160 dpi`)** |
| **빌드 · SDK · 서버 설정** | Android `minSdk 28` / `targetSdk 34` · JDK 25 · Pleos `NaviHelper 3.0.5` · `TextToSpeech 3.3.1` · 공개 서버(`https://majung.cloud`, 별도 키 설정 불필요) |
| **테스트 준비물 · 검증 범위** | 카카오 계정 2개(운전자·탑승자) 및 위치 권한 허용 · 공식 Pleos 에뮬레이터 검증 완료 *(실차 주행 및 앱마켓 심사는 별도)* |

---

## 3. 저장소 및 상세 문서

| 저장소 / 문서 | 설명 |
| --- | --- |
| [majung-vehicle-app](https://github.com/majung-team/majung-vehicle-app) | AAOS 차량 앱 · Pleos SDK 연동 · [APK(`apk/`)](https://github.com/majung-team/majung-vehicle-app/blob/main/apk/%EB%A7%88%EC%A4%91_%EB%A7%88%EC%A4%91_release.apk) |
| [majung-web](https://github.com/majung-team/majung-web) | 탑승자 · 운전자 모바일 웹 (`majung.cloud`) |
| [majung-backend](https://github.com/majung-team/majung-backend) | Spring Boot API · SSE · Push · 운영 배포 |
| [majung-ios](https://github.com/majung-team/majung-ios) | SwiftUI iOS 모바일 앱 |
| **상세 문서** | [시스템 아키텍처](docs/architecture.md) · [개발 현황](docs/development-status.md) · [시연 안내](docs/pickup-demo.md) · [설치·테스트](docs/team-testing.md) · [개인정보 안내](https://majung.cloud/privacy) |
