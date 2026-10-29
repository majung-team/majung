# 개발 현황과 PR 대조

확인일: 2026-10-29. 네 저장소 main·병합 PR·실제 작성 커밋을 대조했다. Notion 07 상태 표·08 진행 현황과 할 일·09 MVP·10 역할표를 같은 근거로 보완했다.
현재 이후 작업 담당은 **송승준 1인**이다. 초기 분담은 과거 계획으로 남기며,
PR 작성자가 승준이라는 이유만으로 포함된 성모 코드를 승준 단독 기여로 바꾸지 않는다.

## 기능별 반영 근거

아래는 코드 반영 여부다. 각 행의 미확인 범위를 포함해 실기기·실차 완료를 의미하지 않는다.
제품 저장소의 PR 링크를 눌러 변경 파일과 당시 확인 방법을 볼 수 있다.

| 기능 | 원래 담당·실제 기여 | main 근거 | 남은 확인·주의 |
| --- | --- | --- | --- |
| 참여·연결 해제 | 성모 | 서버 305ac5c, 웹 3e060f6, 차량 86d72f6 | 초대 링크 자동 참여와 계정 요청 승인은 서로 다른 흐름 |
| 끊김·최신성 | 성모 초안·승준 보완 | 서버 0e75171, 웹 4372565 / [web #45](https://github.com/majung-team/majung-web/pull/45) | 실제 LTE·재접속 확인 필요 |
| 지도·검색 | 성모 설정·승준 통합 | [web #22](https://github.com/majung-team/majung-web/pull/22) · [web #32](https://github.com/majung-team/majung-web/pull/32) · [web #43](https://github.com/majung-team/majung-web/pull/43) | 휴대전화 권한·GPS 확인 필요 |
| 도착 임박 푸시 | 성모 코드·승준 운영 설정 | 서버 c07462e, 웹 6dbf515 | Chrome 구독·해제 확인; 실제 휴대전화 배너 미확인 |
| 장소 제안 | 성모 작성·승준 통합 | [backend #26](https://github.com/majung-team/majung-backend/pull/26) · [web #34](https://github.com/majung-team/majung-web/pull/34) | 작성 커밋 서버 c3bb485·웹 935255f. 단독 구현자로 바꾸지 않음 |
| 장소 확정·버전 | 승준 | [backend #28](https://github.com/majung-team/majung-backend/pull/28) · [vehicle-app #36](https://github.com/majung-team/majung-vehicle-app/pull/36) · [web #36](https://github.com/majung-team/majung-web/pull/36) | 경로 증거 확인 뒤 확정; 실제 두 기기 흐름 필요 |
| 계정·여러 요청 대기 | 승준 | [backend #16](https://github.com/majung-team/majung-backend/pull/16) · [web #15](https://github.com/majung-team/majung-web/pull/15) · [vehicle-app #24](https://github.com/majung-team/majung-vehicle-app/pull/24) | 진행 중 픽업은 한 건, 이후 요청은 별도 대기 |
| 연결된 사람 직접 초대 | 승준 | [backend #18](https://github.com/majung-team/majung-backend/pull/18) · [vehicle-app #29](https://github.com/majung-team/majung-vehicle-app/pull/29) · [vehicle-app #46](https://github.com/majung-team/majung-vehicle-app/pull/46) | 차량에서 사람 선택 → 웹 수락, QR 전달과 구분 |
| QR 최초 연결 | 승준 | [backend #36](https://github.com/majung-team/majung-backend/pull/36) · [web #49](https://github.com/majung-team/majung-web/pull/49) · [vehicle-app #44](https://github.com/majung-team/majung-vehicle-app/pull/44) | 일반 시작 흐름에 관리자 코드 불필요 |
| 차량에서 사람 찾기 | 승준 | [backend #38](https://github.com/majung-team/majung-backend/pull/38) · [vehicle-app #50](https://github.com/majung-team/majung-vehicle-app/pull/50) | 닉네임·마중 ID 검색·연결 요청; 기존 계정 연결된 차량에서 사용 |
| SSE·끊김 복구 | 승준 | [backend #30](https://github.com/majung-team/majung-backend/pull/30) · [backend #32](https://github.com/majung-team/majung-backend/pull/32) · [web #39](https://github.com/majung-team/majung-web/pull/39) · [web #41](https://github.com/majung-team/majung-web/pull/41) | 웹 변경 신호와 재조회. 차량 SDK 콜백을 대체하지 않음 |
| 준비 상태 수신 확인 | 승준 | [backend #23](https://github.com/majung-team/majung-backend/pull/23) · [web #28](https://github.com/majung-team/majung-web/pull/28) · [vehicle-app #34](https://github.com/majung-team/majung-vehicle-app/pull/34) | 운전자 읽음이 아니라 차량 앱 수신 순번 |
| 차량 저장·백그라운드 | 승준 | [vehicle-app #5](https://github.com/majung-team/majung-vehicle-app/pull/5) · [vehicle-app #7](https://github.com/majung-team/majung-vehicle-app/pull/7) | 암호화 저장·복구·공유 중 서비스. 실차 장시간 시험 미확인 |
| 요청→경로→다음 픽업 | 승준 | [vehicle-app #57](https://github.com/majung-team/majung-vehicle-app/pull/57) | 준비 완료 후 시작 확인, 중복 방지·복구. SDK 취소 #56은 별도 미해결 |
| 주행 제한·음성 | 승준 | [vehicle-app #19](https://github.com/majung-team/majung-vehicle-app/pull/19) · [vehicle-app #30](https://github.com/majung-team/majung-vehicle-app/pull/30) · [vehicle-app #32](https://github.com/majung-team/majung-vehicle-app/pull/32) | 에뮬레이터 제한 전환 확인. 실제 스피커·통화 우선순위·실차 미확인 |
| 자동 종료·파기 | 승준 | [backend #21](https://github.com/majung-team/majung-backend/pull/21) | 도착/무활동 30분·12시간 상한, 종료 7일 뒤 파기. 익명 집계 별도 |
| GCP·자동 배포 | 초기 성모 분담, 실제 운영 승준 | [backend #9](https://github.com/majung-team/majung-backend/pull/9) · [web #7](https://github.com/majung-team/majung-web/pull/7) · [backend #45](https://github.com/majung-team/majung-backend/pull/45) · [web #71](https://github.com/majung-team/majung-web/pull/71) | 전용 VM·PostgreSQL·Caddy. 블루·그린 전환과 JDBC 공유 세션 적용. VM·DB 이중화는 아님 |
| 화면·팝업·새로고침 | 승준 | [vehicle-app #54](https://github.com/majung-team/majung-vehicle-app/pull/54) · [vehicle-app #61](https://github.com/majung-team/majung-vehicle-app/pull/61) · [vehicle-app #63](https://github.com/majung-team/majung-vehicle-app/pull/63) · [vehicle-app #65](https://github.com/majung-team/majung-vehicle-app/pull/65) · [vehicle-app #69](https://github.com/majung-team/majung-vehicle-app/pull/69) · [web #61](https://github.com/majung-team/majung-web/pull/61) | 0.2.13-preview. 시험 서버·에뮬레이터 UI 결과와 실기기 구분 |

## 지금 남은 것

| 구분 | 상태·다음 작업 | 이후 담당 |
| --- | --- | --- |
| 실제 휴대전화 | 서로 다른 계정으로 QR·직접 초대·요청·준비 상태·종료, LTE 끊김·재접속 | 승준 |
| 푸시·GPS | 실제 휴대전화 배너·위치 권한, iPhone 홈 화면 조건 | 승준 |
| 실제 SDK | 도착 이벤트 재현·오래 실행하기, 취소 명령 후 안내 유지 이슈 #56 | 승준 |
| 10.4 동의·처리방침 | 차량 #76 첫 사용 안내·동의 전 공유 차단·철회, 웹 #67 공개 /privacy 반영. 에뮬레이터·모바일 크기 브라우저 확인, 실제 휴대전화는 별도 | 승준 |
| 10.2 익명 지표 | 아직 미구현인 후속 기능. 측정하지 않은 수치나 효과를 만들지 않음 | 승준 |
| 시연·제출 | 최신 화면으로 전체 흐름 재촬영, 참가 정보·제출 버전 확정 | 승준 |
| 블루그린 | 서버 #45 반영·운영 첫 전환 확인. 두 HTTP 서버의 로그인·CSRF 공유, 배포 실패 복구 자동 시험 통과 | 승준 |

성모 작성 제출 문서 커밋 e957386·3e0d658·5cc665f도 이력으로 유지한다.
기획서·제출 자료가 있다는 이유로 기능이나 실기기 시험까지 끝났다고 표시하지 않는다.

## 문서 읽는 기준

- [현재 접속·설치](team-testing.md), [운영 버전](runtime-2026-10-29.md)이 현재 안내다.
- [초기 기능·분담 계획](revised-plan.md)의 주차·분담은 당시 계획이며 현재 할당이 아니다.
- 날짜가 붙은 시험 보고서는 당시 결과로 유지한다. 오래된 APK와 커밋은 최신 배포를 의미하지 않는다.
- [Notion 역할표](https://app.notion.com/p/10-4026da9ae26a80c6bb08c5550c5d02be)는 기존 표를 유지하고 같은 근거로 수정했다.

## 10월 29일 준비 기준

참가 팀명은 마중, 대표자는 송승준, 팀원은 안성모이며 혼합팀이므로 일반 부문으로 준비한다. 개발·검증·배포는 승준이 이어가고 기존 성모의 코드 기여를 보존한다. 연락처·동의서는 저장소 밖 Coding/majung-submission-private에서 관리한다. 사용자 조사·설문·인터뷰는 이번 개발·제출 범위에서 제외한다. 실제 휴대전화 기능 시험은 계속한다.

차량 #76·#78·#79, 웹 #67·#69·#71, 서버 #44·#45가 반영됐다. 차량 0.2.14-preview와 공개 운영 버전을 위 기록에 남겼다. 차량 #79는 공식 SDK·로컬 API에서 제안 경로 검증과 장소 버전 반영을 확인했다. 웹 #69는 같은 탭에서 뒤로가기 후 초대 링크를 다시 열 때 기존 참여를 복구한다. 탭 종료·다른 브라우저는 별도다.

설명회에서는 외부 API·인터넷 사용과 에뮬레이터 개발, AI 활용을 안내했다. 세부 권한·실차 안전성·앱마켓 입점까지 승인된 것은 아니다.
