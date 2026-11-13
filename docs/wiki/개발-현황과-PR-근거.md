
# 개발 현황과 PR 근거

마중의 주요 기능 구현, 검증 및 배포 현황을 정리한 문서다.

기능별 구현 내역은 GitHub PR과 커밋을 기준으로 확인하며, 실제 코드 반영 여부와 테스트 결과를 구분해 기록한다.

초기에는 송승준과 성모가 역할을 나누어 개발했으며, 현재 후속 개발과 검증·배포는 송승준이 담당하고 있다.

기여 이력은 PR 병합 담당자만이 아닌 실제 코드 작성 내역과 커밋을 기준으로 구분한다.

---

## 1. 개발 및 검증 자료

| 문서 · 기준 | 확인 대상 | 주요 내용 |
| --- | --- | --- |
| **개발 현황** | [기능별 PR·커밋 기록](https://github.com/majung-team/majung/blob/main/docs/development-status.md) | 기능 구현, 작성 커밋 및 남은 확인 사항 |
| **문제 해결 · 회고** | [기술 회고 6편 (Wiki 홈)](https://github.com/majung-team/majung/wiki#7-%EB%AC%B8%EC%A0%9C-%ED%95%B4%EA%B2%B0%EA%B3%BC-%EA%B8%B0%EC%88%A0-%ED%9A%8C%EA%B3%A0) | 경유지 0 우회, 복구, Blue-Green, 동기화 |
| **운영 · 배포** | [초기 10/29 운영 전환 기록](https://github.com/majung-team/majung/blob/main/docs/runtime-2026-10-29.md) | 최초 Blue-Green 전환 및 배포 기준 |
| **향후 작업** | [남은 개발 및 검증](https://github.com/majung-team/majung/blob/main/docs/remaining-work.md) | 미완료 기능 및 실기기 검증 |
| **역할 분담** | [노션 역할표](https://app.notion.com/p/10-4026da9ae26a80c6bb08c5550c5d02be) | 초기 역할 분담 및 개발 계획 |

- 백엔드(`84d9498` Blue 슬롯 `UP`): 카카오 로컬 키워드·도로명/지번 주소 통합 검색 프록시(`GET /api/v1/places/search`), 회원 탈퇴(`DELETE /api/v1/accounts/me`), 게스트 대기 이탈(`POST /api/v1/pickups/{id}/leave`), 요청·초대 목록 실시간 SSE(`GET /api/v1/ride-requests/events`) 반영 (JUnit `124 passed, 0 failed, 1 skipped` + `deploy` `29 tests`)
- 웹(`0188bd2`, `npm test` `104 passed`) · iOS(`b6d9927`, 참여 전환 보완·상시 이용 가이드, `MajungTests` 32 · `MajungUITests` 6) 운영·저장소 반영 및 차량 APK [`0.2.34-preview`](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.34-preview.20261113)(버전 코드 36, 소스 `127c146`, SHA-256 `89ad895c...` · 빠른 메시지 알림 내비 유지 `9983272` 및 QR 자동 연결 확인 팝업·3초 폴링 포함) Pleos 에뮬레이터(`Majung_Pleos_2_1`) 업데이트 확인 (11/10 시연 영상 `374169dc...`은 직전 `0.2.29-preview` 기준 촬영본으로 유지)

### 2026년 11월 9일 ~ 11월 10일

**차량 앱 · 서버 · 모바일 웹**

- 차량 서명 APK [`0.2.29-preview`](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.29-preview.20261109) (버전 코드 31, 소스 `6d599f1`) 배포 및 Pleos 에뮬레이터 설치·실행 확인
- 초대 링크 사전 확인([서버 #73](https://github.com/majung-team/majung-backend/pull/73)), 초대 만료·일시 연결 실패 구분 및 지도 오류 시 재시도 안내([웹 #108](https://github.com/majung-team/majung-web/pull/108) · [#110](https://github.com/majung-team/majung-web/pull/110)), 내비 복귀 후 장소 제안 확인 및 앱 재실행 후 픽업 공유 재개([차량 #132](https://github.com/majung-team/majung-vehicle-app/pull/132)) 반영

**내비 연동 시연 영상 촬영 (2026.11.10)**

- 운영 서버(`3546902`), 탑승자 웹(`30e28d2`), 차량 앱 `0.2.29-preview`를 연결해 약 1분 36초 분량의 내비 연동 시연 영상([`docs/media/pickup-demo.mp4`](https://github.com/majung-team/majung/raw/main/docs/media/pickup-demo.mp4)) 촬영
- 기본 픽업 흐름(0:00~0:45), 내비 화면 위 탑승자 메시지 알림 및 에뮬레이터 안내 음성(TTS, 0:45~0:51), 장소 변경 제안(`광교중앙역`)과 내비 화면 위 ‘수락 및 내비 변경’을 통한 경유지 경로 추가·웹 약속 장소 갱신(0:51~1:19), 구현 기능 및 검증 범위 요약 카드(1:19~1:36) 수록

[시연 촬영 조건 및 상세 안내](https://github.com/majung-team/majung/blob/main/docs/pickup-demo.md)

### 2026년 10월 29일

**차량 앱**

- 차량 앱 `v0.2.14-preview` 반영
- 최초 이용 시 위치 공유 안내 및 동의 기능 반영
- 픽업 장소 변경 제안 카드 개선

**모바일 웹**

- 동일 브라우저 탭에서 초대 링크 재접속 시 기존 참여 복구 처리

**백엔드 및 배포**

- 내부 Blue-Green 배포 구조 적용
- PostgreSQL 기반 로그인 세션 공유
- 배포 전환 시 SSE 재접속 및 상태 재조회 구조 반영

**문서 및 시연**

- 노션 역할표와 GitHub 개발 근거 대조
- 약 41초 분량의 기본 픽업 흐름 시연 영상 촬영
- 초대 링크 참여, SDK ETA, 준비 완료, 수동 도착 및 탑승 완료 흐름 기록

[시연 영상](https://github.com/user-attachments/assets/e181fcc1-599f-47e9-ac96-98135dae2f24)

> 시연 영상은 시험용 픽업을 로컬 API에서 미리 생성한 에뮬레이터 기반 기록이다. 실제 차량 및 실제 휴대전화의 전체 픽업 검증과는 구분한다.

### 2026년 10월 30일

**모바일 웹**

실제 iPhone Chrome 환경에서 다음 항목을 확인했다.

- 카카오 로그인
- 화면 및 탭 이동
- 운전자 검색
- 웹 재접속

사용성 점검을 통해 입력 화면의 확대 문제와 빈 목록 표시를 개선하고 배포했다.

- [웹 PR #73](https://github.com/majung-team/majung-web/pull/73)

**차량 앱 및 SDK**

차량 앱의 경유지 식별자 대조, 경로 및 ETA 확인, 재연결 관련 테스트를 수행했다.

- [차량 PR #83](https://github.com/majung-team/majung-vehicle-app/pull/83)

해당 PR에서 수행한 시험은 통과했으나, SDK 원본 좌표가 `0`으로 반환되는 현상은 공식 문의 후 답변을 기다리고 있다.


### 2026년 10월 31일

- 비공개 Cloud Storage 외부 백업·다운로드 해시 검증과 30시간 누락 경보 운영 적용
- 외부 복사본을 임시 PostgreSQL에 복원해 세션 인증·계정·픽업 HTTP 조회·로그아웃 후 차단 확인(약 26초, 정리 후 임시 DB 0개)
- 내부 API 5xx·응답 시간 히스토그램 p95 상한·DB 풀·OAuth 콜백 결과 점검 적용. 공개 관리 지표 경로는 404
- SDK 안내 취소 2회와 백그라운드 경로 변경·취소 후 위치 숨김·새 안내·재연결 후 공유 복구 시험 통과. 과거 실패 원인은 미확정

[서버 운영 결과](https://github.com/majung-team/majung-backend/blob/main/docs/operations-recovery.md) · [차량 재시험](https://github.com/majung-team/majung-vehicle-app/pull/89)

---

## 3. 현재 구현 및 검증 범위

기능 구현, 테스트 수행, 실제 차량 검증은 서로 다른 단계로 구분한다.

| 대상 영역 | 현재 상태 (2026.11.13 기준) |
| --- | --- |
| **주요 픽업 기능** | 구현 및 운영(`majung.cloud`)·시험 서버 검증 |
| **차량 앱 (`0.2.34-preview`)** | Pleos 공식 에뮬레이터 설치·내비 유지·QR 검증 |
| **모바일 웹 (`0188bd2`)** | `npm test` 104건 · iPhone Chrome 일부 확인 |
| **iOS 앱 (`b6d9927`)** | `MajungTests` 32 · `MajungUITests` 6 시뮬레이터 |
| **백엔드 서버 (`84d9498`)** | 단일 VM Blue-Green 운영(`UP`) · 124/125 통과 |
| **시연 영상 (11/10)** | `0.2.29-preview` 기준 내비 연동 1분 36초 촬영 |
| **실차 · 실기기 E2E** | 실제 휴대전화 2계정·푸시 배너·LTE·실차 미완료 |
| **익명 서비스 지표** | 후속 구현 예정 (미구현) |

코드가 병합되거나 서버에 배포됐다는 사실만으로 모든 사용자 환경에서 정상 동작한다고 판단하지 않는다.

---

## 4. 알려진 이슈 및 남은 작업

### SDK 관련

**내비게이션 안내 취소 후 상태 유지**

과거 안내 상태 유지 기록은 10/31 공식 에뮬레이터 재시험에서 재현되지 않았다. SDK 취소 2회와 백그라운드 공유 복구 시험을 통과해 이슈를 종료했다. 과거 원인은 확정하지 않았고 재발하면 환경과 함께 다시 확인한다.

- [차량 이슈 #56](https://github.com/majung-team/majung-vehicle-app/issues/56)

**SDK 좌표 응답**

SDK에서 원본 좌표가 `0`으로 반환되는 현상은 공식 문의를 진행했으며, 답변을 기다리고 있다.

### 추가 검증

- 실제 휴대전화에서 초대부터 탑승 완료까지 전체 흐름 확인
- 푸시 알림 배너 수신 및 알림 이동
- LTE 및 Wi-Fi 전환 상황
- 브라우저별 동작 차이
- 실제 차량 환경에서 경로·ETA·주행 제한 동작 확인

### 후속 기능

- 익명 서비스 이용 지표 수집 및 분석
- 반복 픽업 편의 기능 검토
- 탑승자 전용 애플리케이션 도입 검토

개인정보 안내, 최초 위치 공유 동의 및 철회 관련 기능은 현재 구현 내역에 포함되어 있다.

[마중 개인정보 안내](https://majung.cloud/privacy)

---

## 5. 개발 기여 및 이력 관리

마중은 팀 프로젝트로 시작했으며, 초기 역할 분담과 코드 작성 이력을 유지하고 있다.

현재 후속 개발, 기능 검증 및 배포 작업은 송승준이 담당하고 있다.

개발 기여는 다음 근거를 통해 확인한다.

- 실제 코드 작성 커밋
- 기능별 Pull Request
- 코드 변경 및 병합 이력
- 초기 역할 분담 기록
- 개발 및 검증 문서

PR 생성자와 실제 코드 작성자가 다를 수 있으므로, 기여 내용을 확인할 때는 커밋 및 변경 내역을 함께 참고한다.

[기능별 개발 및 기여 근거](https://github.com/majung-team/majung/blob/main/docs/development-status.md)

---

## 관련 문서

- [마중 프로젝트 소개](https://github.com/majung-team/majung)
- [마중 사용 안내](https://github.com/majung-team/majung/wiki)
- [시스템 아키텍처](https://github.com/majung-team/majung/blob/main/docs/architecture.md)
- [운영 및 배포 확인](https://github.com/majung-team/majung/blob/main/docs/runtime-2026-10-29.md)
- [남은 작업](https://github.com/majung-team/majung/blob/main/docs/remaining-work.md)
- [제출 자료 안내](https://github.com/majung-team/majung/blob/main/docs/submission-guide.md)
