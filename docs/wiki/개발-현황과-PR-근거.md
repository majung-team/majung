
# 개발 현황과 PR 근거

마중의 주요 기능 구현, 검증 및 배포 현황을 정리한 문서다.

기능별 구현 내역은 GitHub PR과 커밋을 기준으로 확인하며, 실제 코드 반영 여부와 테스트 결과를 구분해 기록한다.

초기에는 송승준과 성모가 역할을 나누어 개발했으며, 현재 후속 개발과 검증·배포는 송승준이 담당하고 있다.

기여 이력은 PR 병합 담당자만이 아닌 실제 코드 작성 내역과 커밋을 기준으로 구분한다.

---

## 1. 개발 및 검증 자료

| 구분 | 문서 | 내용 |
| --- | --- | --- |
| 개발 현황 | [기능별 PR·커밋 기록](https://github.com/majung-team/majung/blob/main/docs/development-status.md) | 기능 구현, 작성 커밋 및 남은 확인 사항 |
| 운영 현황 | [운영 버전 및 검증 범위](https://github.com/majung-team/majung/blob/main/docs/runtime-2026-10-29.md) | 서버·웹·차량 앱 운영 상태 |
| 향후 작업 | [남은 개발 및 검증](https://github.com/majung-team/majung/blob/main/docs/remaining-work.md) | 미완료 기능 및 실기기 검증 |
| 역할 분담 | [노션 역할표](https://app.notion.com/p/10-4026da9ae26a80c6bb08c5550c5d02be) | 초기 역할 분담 및 개발 계획 |

실제 구현과 검증 근거는 GitHub PR, 커밋 및 관련 문서에서 확인할 수 있다.

---

## 2. 개발 진행 기록

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

---

## 3. 현재 구현 및 검증 범위

기능 구현, 테스트 수행, 실제 차량 검증은 서로 다른 단계로 구분한다.

| 구분 | 현재 상태 |
| --- | --- |
| 주요 픽업 기능 | 구현 및 시험 서버 기반 검증 |
| 차량 앱 | Pleos 공식 에뮬레이터 기반 검증 |
| 모바일 웹 | 주요 기능 구현 및 일부 실제 iPhone Chrome 검증 |
| 백엔드 서버 | 공개 서버 운영 및 배포 |
| 시연 영상 | 에뮬레이터 기반 기본 흐름 촬영 |
| 실제 휴대전화 전체 픽업 | 추가 검증 필요 |
| 푸시 알림 배너 | 실제 기기 검증 필요 |
| LTE·Wi-Fi 전환 | 추가 검증 필요 |
| 실제 차량 환경 | 검증 미진행 |
| 익명 서비스 지표 | 후속 구현 예정 |

코드가 병합되거나 서버에 배포됐다는 사실만으로 모든 사용자 환경에서 정상 동작한다고 판단하지 않는다.

---

## 4. 알려진 이슈 및 남은 작업

### SDK 관련

**내비게이션 안내 취소 후 상태 유지**

내비게이션 안내를 취소한 뒤에도 일부 안내 상태가 유지되는 현상이 확인됐다.

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
