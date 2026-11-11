# 제출 자료 안내

2026년 11월 10일 기준. 참가 정보와 서비스 설명은 **대표자가 수정한 공식 접수양식 원본**을 기준으로 한다. 접수양식 안에 기획 내용을 작성하므로 신청서와 기획서를 별도 문서로 다시 만들지 않는다.

개인정보가 들어 있는 원본은 비공개 `Coding/majung-submission-private` 폴더에서 관리한다. GitHub와 노션에는 연락처·서명·동의서 원본을 올리지 않는다. 원본의 문장과 구성을 문서 자동 생성으로 덮어쓰지 않는다.

## 자료를 확인할 순서

1. [README](../README.md): 서비스 소개, 운전자 초대와 탑승자 요청 흐름.
2. [시연 영상과 촬영 조건](pickup-demo.md): 11월 10일 촬영한 약 1분 36초 내비 연동 및 픽업 흐름(기본 픽업·내비 화면 위 메시지 알림 및 TTS·장소 제안 수락 및 경유지 경로 변경).
3. [접속·설치 안내](team-testing.md): 공개 웹 주소와 차량 앱 설치 방법.
4. [개발 현황](development-status.md): 저장소별 PR과 실제 확인 범위.

대표 시연 영상(`docs/media/pickup-demo.mp4`, 제출 폴더 `마중_픽업_시연_내비연동판.mp4`)은 차량 초대(초대 QR 가림) → 초대 링크 참여 → SDK 위치·ETA → 준비 완료 → 도착·탑승 완료 및 공유 종료에 더해, 45초부터 차량 내비게이션과 탑승자 웹을 나란히 두고 ‘지금 내려가요’ 메시지 알림·음성(TTS) 안내와 1분 2초부터 ‘수락 및 내비 변경’을 통한 경유지(`광교중앙역`) 경로 추가 및 약속 장소 변경까지 보여준다. 공식 Pleos 에뮬레이터와 운영 서버(`majung.cloud`) 기반 시연이며 실제 차량 주행이나 실제 휴대전화 푸시 수신 영상으로 설명하지 않는다.

최신 서명 APK는 [0.2.29-preview](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.29-preview.20261109) (버전 코드 31)다. 11월 10일 대표 시연 영상도 동일한 `0.2.29-preview`와 운영 서버(`majung.cloud`)를 기준으로 촬영했다. 10월 29일 초기 시연 영상은 당시 화면과 로컬 시험 환경 기록으로 함께 보존한다.

## 제출할 자료

| 자료 | 현재 상태 |
| --- | --- |
| 접수양식과 개인정보 동의서 | 공식 DOCX 한 파일에서 관리. 대표자가 수정한 원본을 사용하고 참가자 전원의 동의·본인 서명·날짜를 제출 전에 확인 |
| 시연 영상 | [11월 10일 내비 연동판 영상](pickup-demo.md) (`docs/media/pickup-demo.mp4`, 제출 파일 `마중_픽업_시연_내비연동판.mp4`, 약 1분 36초) 촬영 및 QR 비노출 점검 완료 |
| 코드와 사용 안내 | 소개 저장소와 차량·서버·웹 저장소. 현재 네 저장소 모두 공개이며 비로그인 접근 확인 |
| 차량 release APK | [0.2.29-preview](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.29-preview.20261109) 게시. 제출용 파일로 내려받을 때는 `마중_마중_release.apk` |
| 숏폼 | 제출하지 않음 |

## 제출 전 확인

- 공식 [지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)과 [FAQ](https://appdevchallenge.auto-hmg.io/faq)를 대조한다. 접수양식은 필수이고 영상·GitHub·APK는 선택 제출이다.
- 대표자가 팀원 전원을 CC에 넣어 공식 접수 이메일로 보낸다. 동의·서명은 각 참가자가 작성한 내용인지 확인한다.
- GitHub 링크와 APK 파일 경로가 열리는지, README에 실행 환경·설치 방법·서버 주소가 있는지 확인한다. Releases 파일명은 `Majung_Majung_release.apk`이며 제출 파일명은 `마중_마중_release.apk`다. 이름을 바꾼 뒤에도 SHA-256이 릴리스의 `SHA256SUMS` 값과 같은지 확인한다.
- 영상의 촬영 버전과 APK 버전을 함께 적는다. 공개 웹은 [release.json](https://majung.cloud/release.json), 서버는 [health](https://majung.cloud/api/v1/health)에서 확인한다.
- 추가 실기기 시험은 현재 우선 작업에서 보류한다. 미확인 항목을 검증 완료로 바꾸지는 않는다. 남은 범위는 [남은 작업](remaining-work.md)에 기록한다.

세부 제출 상태는 [점검표](submission-checklist.md)를 따른다. VM 이름·고정 IP·프로젝트 식별값·운영 자격 증명·초대 토큰은 제출 소개 자료에 넣지 않는다. 이 문서 정리로 제출 메일을 발송하거나 저장소 공개 범위를 바꾸지는 않는다.

## 참고 · 11월 5일 사용성 보완 기록

11월 5일 기준 사용성 보완 변경 내역이며, 최신 제출 APK는 위 안내의 `0.2.29-preview` (버전 코드 31)다.

웹의 새 요청 지도 높이를 복구하고 선택한 장소명·검색 주소를 요청 버튼 위에 표시했다. 준비 상태는 초대 링크와 계정 화면에서 같은 조작을 사용하며, 서버 저장과 차량 수신 확인을 구분한다. 동일한 차량명은 구분 코드·계정 연결 시각으로 구별한다.

- [웹 변경과 화면](https://github.com/majung-team/majung-web/blob/main/docs/usability-2026-11-05.md)
- [차량 화면 변경과 확인 범위](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/usability-2026-11-05.md)
- 당시 서명 APK (11/5): versionCode 25, SHA-256 `00bc243ed9fa6ea4fb6263bb917e622e02bf5bd28ae13c30fb68d76ec293be87`

공모전 신청서·기획서·동의서는 기존 양식을 유지한 로컬 파일을 사용한다. 참가자 연락처와 서명이 있는 원본은 저장소에 올리지 않는다. 트랙패드 관성·실차 결과는 이번 자동 검사로 대체하지 않는다.
