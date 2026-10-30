# 제출 자료 안내

2026년 10월 30일 기준. 참가 정보와 서비스 설명은 **대표자가 수정한 공식 접수양식 원본**을 기준으로 한다. 접수양식 안에 기획 내용을 작성하므로 신청서와 기획서를 별도 문서로 다시 만들지 않는다.

개인정보가 들어 있는 원본은 비공개 `Coding/majung-submission-private` 폴더에서 관리한다. GitHub와 노션에는 연락처·서명·동의서 원본을 올리지 않는다. 원본의 문장과 구성을 문서 자동 생성으로 덮어쓰지 않는다.

## 자료를 확인할 순서

1. [README](../README.md): 서비스 소개, 운전자 초대와 탑승자 요청 흐름.
2. [시연 영상과 촬영 조건](pickup-demo.md): 10월 29일 촬영한 약 41초 기본 흐름.
3. [접속·설치 안내](team-testing.md): 공개 웹 주소와 차량 앱 설치 방법.
4. [개발 현황](development-status.md): 저장소별 PR과 실제 확인 범위.

현재 영상은 링크 참여 → SDK ETA → 준비 완료 → 수동 도착 → 탑승 완료를 보여준다. 예전의 이름 입력·운전자 승인 영상은 현재 대표 영상이 아니다. 시험용 픽업을 로컬 API로 미리 만들었으며, 실제 휴대전화나 실차 영상으로 설명하지 않는다.

현재 제출 APK는 10월 30일 가독성을 개선한 0.2.15-preview다. 기존 10월 29일 영상은 이전 UI로 촬영했으며 새 글자·버튼 크기는 [차량 가독성 변경](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/readability-2026-10-30.md)의 캡처에서 확인한다. 영상과 최신 APK의 화면이 완전히 같다고 설명하지 않는다.

## 제출할 자료

| 자료 | 현재 상태 |
| --- | --- |
| 접수양식과 개인정보 동의서 | 공식 DOCX 한 파일에서 관리. 대표자가 수정한 원본을 사용하고 참가자 전원의 동의·본인 서명·날짜를 제출 전에 확인 |
| 시연 영상 | [10월 29일 영상](pickup-demo.md) 촬영 완료. 추가 영상은 현재 자료를 보완할 때 선택 |
| 코드와 사용 안내 | 소개 저장소와 차량·서버·웹 저장소. 현재 네 저장소 모두 공개이며 비로그인 접근 확인 |
| 차량 release APK | [0.2.15-preview](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.15-preview.20261030) 게시. 제출 파일명은 `마중_마중_release.apk` |
| 숏폼 | 제출하지 않음 |

## 제출 전 확인

- 공식 [지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)과 [FAQ](https://appdevchallenge.auto-hmg.io/faq)를 대조한다. 접수양식은 필수이고 영상·GitHub·APK는 선택 제출이다.
- 대표자가 팀원 전원을 CC에 넣어 공식 접수 이메일로 보낸다. 동의·서명은 각 참가자가 작성한 내용인지 확인한다.
- GitHub 링크와 APK 파일 경로가 열리는지, README에 실행 환경·설치 방법·서버 주소가 있는지 확인한다. Releases 파일명은 `Majung_Majung_release.apk`이며 제출 파일은 `마중_마중_release.apk`로 준비했다. 두 파일의 SHA-256은 같다.
- 영상의 촬영 버전과 APK 버전을 함께 적는다. 공개 웹은 [release.json](https://majung.cloud/release.json), 서버는 [health](https://majung.cloud/api/v1/health)에서 확인한다.
- 추가 실기기 시험은 현재 우선 작업에서 보류한다. 미확인 항목을 검증 완료로 바꾸지는 않는다. 남은 범위는 [남은 작업](remaining-work.md)에 기록한다.

세부 제출 상태는 [점검표](submission-checklist.md)를 따른다. VM 이름·고정 IP·프로젝트 식별값·운영 자격 증명·초대 토큰은 제출 소개 자료에 넣지 않는다. 이 문서 정리로 제출 메일을 발송하거나 저장소 공개 범위를 바꾸지는 않는다.
