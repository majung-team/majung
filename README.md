# 마중 (Majung)

데리러 가는 사람과 기다리는 사람이 만날 장소·남은 시간·준비 상태를 함께 확인하는 서비스다.
차량은 Pleos 앱, 휴대전화는 웹을 사용한다.

**[사용 안내 Wiki](https://github.com/majung-team/majung/wiki)** · **[접속·설치·테스트](docs/team-testing.md)** · **[개발 현황과 PR 근거](docs/development-status.md)** · [문서 목록](docs/README.md)

## 먼저 사용해 보려면

- 휴대전화: [majung.cloud/account](https://majung.cloud/account)에서 카카오 로그인
- 차량: [서명 APK 0.2.11-preview](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.11-preview.20261028)를 공식 Pleos 에뮬레이터에 설치
- 처음 한 번 차량 QR을 운전자 휴대전화로 열고, 차량에서 본인 계정을 확인
- 탑승자는 별도 앱을 설치하지 않는다. 마중 SDK 확인 앱은 개발 진단용이며 일반 이용에 필요 없다.

차량은 내비 경로를 확인한 뒤 픽업을 시작한다. 휴대전화에서 웹 주소만 여는 것으로 차량 위치가 자동 공개되지는 않는다.

## 만나는 두 가지 방법

| 시작하는 사람 | 사용 순서 |
| --- | --- |
| 운전자 | 내비에서 장소 설정 → 마중에서 장소 확인 → 연결된 사람 선택·초대 → 탑승자가 웹에서 수락 |
| 처음 초대하는 운전자 | 장소 확인 → QR 초대 생성 → 운전자 휴대전화로 열어 링크 전달 → 탑승자 참여 |
| 탑승자 | 운전자를 닉네임·마중 ID로 검색 → 장소명·주소 검색 또는 지도 지점 선택 → 마중 요청 → 차량에서 수락 |

여러 요청은 대기열에 받고 픽업은 한 건씩 진행한다. 대기 중인 사람에게 현재 진행 중인 다른 픽업까지의 시간을
자기 도착 예정 시간처럼 표시하지 않는다. 차량 위치는 해당 픽업이 시작된 뒤 권한이 있는 탑승자에게 제공한다.
탑승 완료·취소 뒤에는 위치 공유를 끝낸다.

## 현재 구현과 확인 범위

2026년 10월 28일 기준이다. [실제 PR·커밋 대조](docs/development-status.md)와 [운영 버전](docs/runtime-2026-10-28.md)을 함께 본다.

| 항목 | 현재 상태 |
| --- | --- |
| 계정·차량 | 카카오 로그인, 닉네임·마중 ID 검색, 차량 QR 최초 연결, 사람 연결·해제 |
| 픽업 | QR 초대·직접 초대, 탑승자 요청, 여러 요청 대기, 경로 확인 후 시작·다음 픽업 |
| 위치·ETA | SDK 자료 전송, 웹 SSE 변경 신호와 상태 재조회, 끊김·유효 시간 만료 시 이전 위치·ETA 숨김 |
| 장소 변경 | 탑승자 검색·지도 제안 → 차량 경로 검증 → 장소 버전 확정 |
| 차량 화면 | 정차 중 상세 조작, 시스템 제한 시 읽기 전용 요약, 선택한 경우 음성 안내 |
| 복구·종료 | 암호화 저장, 서버 상태 확인 후 사용자 재개 확인, 자동 종료·7일 보관 후 파기 |
| 운영 | 마중 전용 VM·PostgreSQL·Caddy HTTPS, 서버·웹 각각 Actions 배포 |

단위·시험 서버·공식 에뮬레이터 결과가 있다. 실제 휴대전화 전체 흐름·푸시 배너·LTE 전환·실차 검증은 별도다.
SDK 내비 취소 후 안내가 유지되는 [차량 이슈 #56](https://github.com/majung-team/majung-vehicle-app/issues/56)은 미해결이다.
정식 동의·처리방침과 익명 지표 집계도 모두 끝난 것으로 표시하지 않는다.

## 전체 구조

![서비스 구성](docs/diagrams/overview.svg)

차량 앱과 웹은 같은 HTTPS 서버 API를 사용한다. 웹 SSE는 변경 신호를 전달하며, 실제 상태는 API로 다시 조회한다.
위치·ETA의 원천은 차량 SDK다. 서버가 교통 상황을 분석해 ETA를 새로 계산하지 않는다.
서버 배포는 재시작·실패 시 복구 방식으로 짧은 중단이 가능하다. 블루그린 배포는 아직 적용하지 않았다.

[파일·폴더와 작동 원리](docs/architecture.md)

## 개발 저장소

| 저장소 | 내용 |
| --- | --- |
| [majung-vehicle-app](https://github.com/majung-team/majung-vehicle-app) | Kotlin 차량 앱, SDK·경로·차량 화면 |
| [majung-backend](https://github.com/majung-team/majung-backend) | Spring Boot API, 인증·픽업·저장·SSE·푸시 |
| [majung-web](https://github.com/majung-team/majung-web) | 휴대전화 웹, 지도·검색·공유·알림 |
| 이 저장소 | 전체 사용 안내, 설계, 개발 근거, 시연·제출 자료 |

현재는 송승준이 개발·검증·배포를 이어간다. 초기 성모·승준 분담과 성모가 작성한 코드는
[개발 현황](docs/development-status.md)에 기여 이력으로 남겼다.

## 이전 시연 기록

https://github.com/user-attachments/assets/3090606c-4212-4439-8da4-d6c45c02e819

약 45초의 초기 로컬 시연이다. 현재 차량 UI·자동 참여·사람 검색·대기열 전체를 보여주는 최신 영상은 아니다.
[촬영 환경](docs/pickup-demo.md) · [최신 흐름 촬영 순서](docs/demo-script.md)

[남은 작업](docs/remaining-work.md) · [기여 규칙](CONTRIBUTING.md) · [제출 안내](docs/submission-guide.md)
