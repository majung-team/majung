<div align="center">

# 마중

**데리러 가는 사람과 기다리는 사람을 연결하는 차량 앱**

“어디쯤이야?” “지금 내려갈까?”

차량에서는 탑승자의 준비 상태를 보고, 탑승자는 휴대전화로 차량 위치와 남은 시간을 확인합니다.

[접속 주소·팀 테스트](docs/team-testing.md) · [시연 영상](#시연-영상) · [구현 범위](#현재-구현-범위) · [전체 구조](#전체-구조) · [개발 저장소](#개발-저장소)

</div>

## 소개

누군가를 데리러 갈 때 “어디야?”, “지금 나갈까?” 같은 연락을 반복하게 됩니다.
마중은 차량 내비게이션의 위치·남은 시간과 탑승자의 준비 상태를 주고받는 앱입니다.
탑승자가 준비를 마치면 차량에서 확인하고, 차에 타면 픽업을 끝내 위치 공유를 종료합니다.

## 사용 흐름

| 순서 | 운전자·차량 | 탑승자·휴대전화 |
| --- | --- | --- |
| 약속 만들기 | 내비게이션에서 만날 장소를 정하고 픽업 생성 | 초대 링크 받기 |
| 초대·연결 | 초대 링크 전달, 참여 확인 | 링크로 참여 |
| 이동 중 | 탑승자가 준비됐는지 차량에서 확인 | 차량 위치·남은 시간 확인, 준비 상태 변경 |
| 도착·탑승 | 도착 알림 후 실제 탑승을 확인하고 완료 | 도착·공유 종료 상태 확인 |

기존 초대 방식과 탑승자가 먼저 요청하는 방식을 함께 사용한다. 처음 보는 탑승자도 운전자를 검색해 장소명·주소 검색, 지도 핀 또는 현재 위치로 고른 장소와 설명을 보낼 수 있다. 운전자는 여러 요청을 받아 수락 순서대로 대기에 넣고 한 건씩 픽업한다.

[픽업 시작 방식과 대기 요청](docs/pickup-request-flow.md) · **[처음 사용하는 개발자를 위한 접속·설치·테스트 안내](docs/team-testing.md)**

## 시연 영상

https://github.com/user-attachments/assets/3090606c-4212-4439-8da4-d6c45c02e819

약 45초, 무음. 생성 → 참여 → 승인 → 준비 완료 → 도착 → 탑승 확인까지 담았습니다.
공식 Pleos 에뮬레이터·로컬 API·웹으로 촬영한 분리 전 영상입니다. 이후 추가한 재실행 복구·화면 밖 공유는 영상에 없습니다.
[촬영 환경](docs/pickup-demo.md) · [MP4 원본 다운로드](https://github.com/majung-team/majung/raw/refs/heads/main/docs/media/pickup-demo.mp4)

<details>
<summary>SDK 연결 중단 전·후 화면 보기</summary>

![정상 수신과 SDK 해제 후 이전 정보 숨김](docs/media/sdk-web-flow.gif)

SDK 연결 중·해제 후 화면을 캡처해 두 장으로 비교한 GIF입니다.

</details>

## 전체 구조

![마중 서비스 구성](docs/diagrams/overview.svg)

차량 앱과 웹은 서버 API로 연결합니다. 차량은 Pleos Android Automotive 앱, 탑승자 화면은 웹입니다.
서버는 SDK의 위치·남은 시간과 픽업 상태를 저장하고, 웹에서 보낸 준비 상태를 차량에 전달합니다.

**[상세 아키텍처](docs/architecture.md)** — 차량 서비스·화면·SDK·암호화 저장, 서버·웹 파일 구조, 로컬 실행과 배포 준비.

## 현재 구현 범위

2026-10-26 기준. main 반영과 실제 기기에서 확인한 범위를 구분한다. [최신 변경과 운영 확인](docs/runtime-2026-10-26.md), [팀 테스트 안내](docs/team-testing.md)를 함께 확인한다.

| 항목 | 상태·확인 범위 |
| --- | --- |
| 공개 서버·웹 | https://majung.cloud. 전용 VM·PostgreSQL·Caddy와 저장소별 GitHub Actions 사용 |
| 기존 초대 픽업 | 첫 참여자는 이름 입력·별도 승인 없이 연결. 차량에서 탑승자 끊기와 새 초대 가능. 사람 연결·마중 요청 수락은 별도 절차 |
| 카카오 로그인·사람 찾기 | 공개 HTTPS 로그인·닉네임 저장·세션 유지 확인. 실제 두 계정 간 새 요청은 별도 확인 필요 |
| 운전자 계정 연결·여러 요청 | 서버 PR #16, 웹 PR #15, 차량 PR #24 main 반영. 로컬 가상 계정·에뮬레이터로 확인 |
| 경유지·경로 재확인 | 좌표 누락 때 앱 식별자 대응과 장소 확인 적용. 새 요청의 실제 휴대전화 GPS→SDK 전체 흐름은 별도 확인 |
| 재실행·화면 밖 공유 | 암호화 저장·복구·공유 재개 확인. 앱 종료 상태의 새 요청 푸시와는 별개 |
| 주행 제한 | P → D → P에 따라 정차 화면과 읽기 전용 픽업·준비·ETA 요약 전환 확인. 실차 미확인 |
| 준비 상태 전달 | 서버 저장과 차량 앱 수신을 구분. 공개 HTTPS·에뮬레이터에서 확인. 새 차량 APK 필요 |
| 도착 알림 | 운영 VAPID 설정과 Chrome 구독·해제 확인. 실제 OS 배너·휴대전화 수신은 미확인 |
| ETA 갱신 | 차량 SDK 약 5초 조회, 초대·진행 중 계정 화면 약 3초 조회. SSE는 설계 검토 단계 |
| 장소 검색·지도·탑승자 앱 | 마중 전용 무료 쿼터·키와 실제 SDK 지도 표시 확인. 장소명·주소 검색, 지도 핀·현재 위치로 요청. 탑승자 네이티브 앱은 후속 작업 |

실차, 실제 휴대전화 GPS·권한, release 서명·차량 플랫폼 심사, 앱 종료 상태의 새 요청 알림은 아직 확인할 항목이다. 기존 45초 시연 영상에 새 계정·대기열까지 담겨 있지는 않다.

## 개발 저장소

| 저장소 | 담는 내용 | 시작 위치 |
| --- | --- | --- |
| **[majung-vehicle-app](https://github.com/majung-team/majung-vehicle-app)** | 차량 화면, Pleos SDK, 위치·ETA 전송, SDK 진단 모듈 | [실행 안내](https://github.com/majung-team/majung-vehicle-app#실행) · [화면 구조](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/vehicle-screen.md) |
| **[majung-web](https://github.com/majung-team/majung-web)** | 운전자 휴대전화 공유 화면·탑승자 웹 | [README](https://github.com/majung-team/majung-web#readme) |
| **[majung-backend](https://github.com/majung-team/majung-backend)** | 픽업 API, 권한·상태 관리, DB·배포 구성 | [README](https://github.com/majung-team/majung-backend#readme) |
| **majung · 이 저장소** | 서비스 소개, 전체 구조, 공통 기획·시연 자료, 분리 전 개발 기록 | [문서 안내](docs/README.md) |

현재 코드는 위 세 개발 저장소에서 수정·실행합니다. 이 저장소의 `vehicle/`, `server/`, `web/` 등은 [분리 전 기록](docs/archive/README.md)으로 남겨뒀습니다.

## 팀

두 명이 차량·웹·서버를 기능별로 나눠 개발합니다. 담당과 작업 순서는 아래 계획 문서에 정리했습니다.

[기능별 계획과 역할](docs/revised-plan.md) · [기여 가이드](CONTRIBUTING.md) · [팀 노션](https://app.notion.com/p/4026da9ae26a805abd45c0ee5885ef2f)

[제출 자료 안내](docs/submission-guide.md)
