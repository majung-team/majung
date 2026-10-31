
# 개발 현황과 PR 대조

**기준일: 2026년 10월 31일**

마중 프로젝트의 기능별 구현 현황, 개발 기여 이력, PR·커밋 근거 및 남은 검증 항목을 정리한 문서다.

차량 앱, 백엔드, 모바일 웹, 프로젝트 문서 저장소의 `main` 브랜치와 병합 PR, 실제 작성 커밋을 기준으로 개발 이력을 관리한다.

초기에는 **송승준과 안성모가 역할을 분담하여 개발**했으며, 현재 후속 개발·검증·배포는 **송승준이 담당**하고 있다.

기여도는 최초 역할 분담이나 PR 생성자만으로 판단하지 않고 실제 작성 코드, 커밋 및 변경 이력을 기준으로 구분한다.

> 이 문서의 코드 반영 여부와 시험 결과는 각 기록의 확인 범위에 한정된다. 기능 구현, 운영 배포, 에뮬레이터 검증, 실제 휴대전화 검증, 실차 검증은 각각 구분한다.

---

## 1. 기능별 구현 및 기여 근거

### 1.1 계정·연결·픽업 요청

| 기능 | 개발 기여 | PR·커밋 근거 | 구현 및 확인 범위 |
| --- | --- | --- | --- |
| 참여·연결 해제 | 안성모 | 서버 `305ac5c`, 웹 `3e060f6`, 차량 `86d72f6` | 초대 링크 참여와 계정 연결 요청 승인 흐름 구분 |
| 계정·다중 요청 대기 | 송승준 | [서버 #16](https://github.com/majung-team/majung-backend/pull/16) · [웹 #15](https://github.com/majung-team/majung-web/pull/15) · [차량 #24](https://github.com/majung-team/majung-vehicle-app/pull/24) | 픽업 요청 대기열과 단일 진행 픽업 관리 |
| 연결된 사람 직접 초대 | 송승준 | [서버 #18](https://github.com/majung-team/majung-backend/pull/18) · [차량 #29](https://github.com/majung-team/majung-vehicle-app/pull/29) · [차량 #46](https://github.com/majung-team/majung-vehicle-app/pull/46) | 차량에서 탑승자를 선택해 초대하고 웹에서 수락 |
| QR 최초 연결 | 송승준 | [서버 #36](https://github.com/majung-team/majung-backend/pull/36) · [웹 #49](https://github.com/majung-team/majung-web/pull/49) · [차량 #44](https://github.com/majung-team/majung-vehicle-app/pull/44) | QR을 통한 운전자 계정 연결, 일반 연결 과정에서 관리자 코드 불필요 |
| 차량에서 사람 찾기 | 송승준 | [서버 #38](https://github.com/majung-team/majung-backend/pull/38) · [차량 #50](https://github.com/majung-team/majung-vehicle-app/pull/50) | 닉네임·마중 ID 검색 및 연결 요청 |

### 1.2 지도·장소·경로 관리

| 기능 | 개발 기여 | PR·커밋 근거 | 구현 및 확인 범위 |
| --- | --- | --- | --- |
| 지도·장소 검색 | 안성모 초기 설정, 송승준 통합 | [웹 #22](https://github.com/majung-team/majung-web/pull/22) · [웹 #32](https://github.com/majung-team/majung-web/pull/32) · [웹 #43](https://github.com/majung-team/majung-web/pull/43) | 지도·장소 검색 반영, 실제 휴대전화 GPS·권한 검증 별도 |
| 장소 변경 제안 | 안성모 작성, 송승준 통합 | [서버 #26](https://github.com/majung-team/majung-backend/pull/26) · [웹 #34](https://github.com/majung-team/majung-web/pull/34) | 작성 커밋: 서버 `c3bb485`, 웹 `935255f` |
| 장소 확정·버전 관리 | 송승준 | [서버 #28](https://github.com/majung-team/majung-backend/pull/28) · [차량 #36](https://github.com/majung-team/majung-vehicle-app/pull/36) · [웹 #36](https://github.com/majung-team/majung-web/pull/36) | 차량의 경로 검증 후 장소 확정, 실제 두 기기 전체 흐름 검증 필요 |
| 요청·경로·다음 픽업 | 송승준 | [차량 #57](https://github.com/majung-team/majung-vehicle-app/pull/57) | 시작 상태 확인, 중복 처리 방지 및 복구, SDK 내비 취소 관련 이슈 별도 관리 |
| 경유지·ETA 재검증 | 송승준 | [차량 #79](https://github.com/majung-team/majung-vehicle-app/pull/79) · [차량 #83](https://github.com/majung-team/majung-vehicle-app/pull/83) | 경유지 식별자 대조, 경로·ETA·재연결 시험. SDK 원본 좌표 `0` 응답은 별도 문의 중 |

### 1.3 위치·상태 동기화·알림

| 기능 | 개발 기여 | PR·커밋 근거 | 구현 및 확인 범위 |
| --- | --- | --- | --- |
| 끊김·정보 최신성 | 안성모 초안, 송승준 보완 | 서버 `0e75171`, 웹 `4372565`, [웹 #45](https://github.com/majung-team/majung-web/pull/45) | 오래된 정보 처리, 실제 LTE 전환·재접속 검증 필요 |
| 도착 임박 푸시 | 안성모 코드, 송승준 운영 설정 | 서버 `c07462e`, 웹 `6dbf515` | Chrome 구독·해제 확인, 실제 휴대전화 알림 배너 미검증 |
| SSE·연결 복구 | 송승준 | [서버 #30](https://github.com/majung-team/majung-backend/pull/30) · [서버 #32](https://github.com/majung-team/majung-backend/pull/32) · [웹 #39](https://github.com/majung-team/majung-web/pull/39) · [웹 #41](https://github.com/majung-team/majung-web/pull/41) | 웹 SSE 변경 신호 및 API 재조회. 차량 SDK 콜백과 별도 |
| 준비 상태 수신 확인 | 송승준 | [서버 #23](https://github.com/majung-team/majung-backend/pull/23) · [웹 #28](https://github.com/majung-team/majung-web/pull/28) · [차량 #34](https://github.com/majung-team/majung-vehicle-app/pull/34) | 차량 앱 수신 순번 확인. 운전자가 실제로 읽었다는 의미는 아님 |
| 자동 종료·데이터 파기 | 송승준 | [서버 #21](https://github.com/majung-team/majung-backend/pull/21) | 도착·무활동 기준 30분, 최대 12시간 상한 및 종료 7일 후 파기 정책. 익명 지표 집계는 별도 |

### 1.4 차량 앱·운영 환경

| 기능 | 개발 기여 | PR·커밋 근거 | 구현 및 확인 범위 |
| --- | --- | --- | --- |
| 차량 저장·백그라운드 처리 | 송승준 | [차량 #5](https://github.com/majung-team/majung-vehicle-app/pull/5) · [차량 #7](https://github.com/majung-team/majung-vehicle-app/pull/7) | 암호화 저장, 연결 복구 및 공유 중 서비스 처리. 실차 장시간 검증 필요 |
| 주행 제한·음성 안내 | 송승준 | [차량 #19](https://github.com/majung-team/majung-vehicle-app/pull/19) · [차량 #30](https://github.com/majung-team/majung-vehicle-app/pull/30) · [차량 #32](https://github.com/majung-team/majung-vehicle-app/pull/32) | 에뮬레이터 주행 제한 전환 확인. 실제 차량·스피커·통화 우선순위 검증 필요 |
| 차량 화면·팝업·새로고침 | 송승준 | [차량 #54](https://github.com/majung-team/majung-vehicle-app/pull/54) · [차량 #61](https://github.com/majung-team/majung-vehicle-app/pull/61) · [차량 #63](https://github.com/majung-team/majung-vehicle-app/pull/63) · [차량 #65](https://github.com/majung-team/majung-vehicle-app/pull/65) · [차량 #69](https://github.com/majung-team/majung-vehicle-app/pull/69) · [웹 #61](https://github.com/majung-team/majung-web/pull/61) | `0.2.13-preview` 단계의 차량 UI 개선 및 시험 기록 |
| GCP·자동 배포 | 안성모 초기 역할 분담, 송승준 실제 운영 구현 | [서버 #9](https://github.com/majung-team/majung-backend/pull/9) · [웹 #7](https://github.com/majung-team/majung-web/pull/7) · [서버 #45](https://github.com/majung-team/majung-backend/pull/45) · [웹 #71](https://github.com/majung-team/majung-web/pull/71) | 전용 VM, PostgreSQL, Caddy, GitHub Actions, Blue-Green 배포 및 JDBC 공유 세션 |

---

## 2. 최근 주요 변경 사항

### 2026년 10월 31일 · 복구 및 운영 검증

- 외부 백업: 비공개 Cloud Storage에 매일 복사, 다운로드 해시 대조·성공 시각 기록. 30시간 누락 시 운영 경보 ([서버 #53](https://github.com/majung-team/majung-backend/pull/53) · [#56](https://github.com/majung-team/majung-backend/pull/56) · [#59](https://github.com/majung-team/majung-backend/pull/59))
- 복원 기능 시험: 외부 복사본을 임시 PostgreSQL에 복원하고 세션 인증·계정·픽업 HTTP 조회·로그아웃 후 차단 통과. 약 26초, 정리 후 임시 DB 0개 ([서버 #55](https://github.com/majung-team/majung-backend/pull/55))
- 운영 지표: 내부 API 5xx·응답 시간 히스토그램·DB 풀·OAuth 결과 수집, 공개 관리 경로 404 확인 ([서버 #58](https://github.com/majung-team/majung-backend/pull/58) · [#59](https://github.com/majung-team/majung-backend/pull/59))
- 안내 취소: SDK 직접 취소 2회, 백그라운드 경로 변경·취소 후 위치 숨김 12초·새 안내·재연결 후 공유 복구 시험 통과 ([차량 #89](https://github.com/majung-team/majung-vehicle-app/pull/89)). 과거 실패 원인은 확정하지 않음
- Notion 역할표: 기존 초기 분담과 기여 이력을 유지하고 운영·복구 시험 결과와 APK 0.2.16-preview를 반영

[운영 시험 결과](https://github.com/majung-team/majung-backend/blob/main/docs/operations-recovery.md) · [SDK 재시험 기록](https://github.com/majung-team/majung-vehicle-app/tree/main/evidence/2026-10-31-sdk-cancel)


### 2026년 10월 29일

**차량 앱**

- `v0.2.14-preview` APK 준비
- 최초 위치 공유 안내 및 동의 기능 반영
- 장소 변경 제안 카드 및 경로 검증 개선
- 공식 SDK·로컬 API에서 장소 버전 반영 확인

관련 PR:
- [차량 #76](https://github.com/majung-team/majung-vehicle-app/pull/76)
- [차량 #78](https://github.com/majung-team/majung-vehicle-app/pull/78)
- [차량 #79](https://github.com/majung-team/majung-vehicle-app/pull/79)

**모바일 웹**

- 개인정보 안내 페이지 반영
- 같은 브라우저 탭에서 초대 링크 재접속 시 기존 참여 복구
- 탭 완전 종료 및 다른 브라우저 이용은 별도 검증 대상으로 구분

관련 PR:
- [웹 #67](https://github.com/majung-team/majung-web/pull/67)
- [웹 #69](https://github.com/majung-team/majung-web/pull/69)
- [웹 #71](https://github.com/majung-team/majung-web/pull/71)

**백엔드·배포**

- 내부 Blue-Green 배포 구조 적용
- PostgreSQL 기반 JDBC 공유 세션 도입
- 두 HTTP 서버 간 로그인 및 CSRF 상태 공유 확인
- 배포 전환 및 실패 복구 자동 시험 통과

관련 PR:
- [서버 #44](https://github.com/majung-team/majung-backend/pull/44)
- [서버 #45](https://github.com/majung-team/majung-backend/pull/45)

Blue-Green 구조는 애플리케이션 배포 전환을 위한 것으로, VM과 데이터베이스 자체를 이중화한 구성은 아니다.

**시연 및 문서**

- Notion 07~10 진행 자료와 GitHub 구현 근거 대조
- 약 41초 분량의 기본 픽업 시연 영상 촬영
- 차량 `0.2.14-preview` 및 공개 운영 버전 기록

[기본 픽업 시연 영상](https://github.com/user-attachments/assets/e181fcc1-599f-47e9-ac96-98135dae2f24)

영상은 로컬 API에서 시험용 픽업을 미리 생성한 에뮬레이터 기반 시연이다. 실제 휴대전화 전체 픽업 및 실차 검증 결과와는 구분한다.

### 2026년 10월 30일

**모바일 웹 사용성 개선**

실제 iPhone Chrome에서 다음 항목 확인:

- 카카오 로그인
- 탭 및 화면 이동
- 운전자 검색
- 웹 재접속

입력 화면 확대와 빈 목록 표시 문제를 개선한 뒤 운영 배포를 확인했다.

- [웹 PR #73](https://github.com/majung-team/majung-web/pull/73)
- 자동 검사 75개 통과 기록
- 운영 배포 후 관련 화면 재확인

**차량 SDK 및 경로 검증**

경유지 식별자, 경로, ETA 및 재연결 관련 시험 결과를 정리했다.

- [차량 PR #83](https://github.com/majung-team/majung-vehicle-app/pull/83)

해당 범위의 시험 결과와 SDK 원본 응답 문제는 구분한다.

앱의 경유지 식별자 대조 및 관련 시험은 통과했으나, SDK 원본 좌표가 `0`으로 반환되는 현상은 공식 문의 후 답변을 기다리고 있다.

---

### 2026년 10월 31일

- [차량 #87](https://github.com/majung-team/majung-vehicle-app/pull/87): 기존 경유지의 최근 장소 기반 좌표 복원, 확인되지 않은 경유지 1~2곳 재선택, 순서·최종 목적지 유지. 별도 Pleos 에뮬레이터 SDK·Android 복구 6개 시험 통과, 단위 60개 통과. 원본 `(0,0)` 응답 수정과는 구분한다.
- [서버 #51](https://github.com/majung-team/majung-backend/pull/51): 실제 HTTP 응답을 배포된 웹 계산 코드에 넣는 ETA 호환성 시험 추가. 경과 시간 중복 차감, 30초 STALE, 2분 숨김, 정체로 늘어나거나 줄어든 ETA 반영을 검사한다.
- [서버 #48](https://github.com/majung-team/majung-backend/pull/48)·[#49](https://github.com/majung-team/majung-backend/pull/49): 최신 백업 임시 DB 복원, 테이블 9개 조회·스키마 검증·서버 기동, 주간 복원·매분 운영 점검, 외부 HTTPS 점검 2개와 마중 전용 이메일 정책 적용.
- 일시 503 이후 ACTIVE 상태를 다시 받아도 사용자 확인 전에는 위치 공유를 재개하지 않는 Android 시험 보강.
- 실제 시동 종료·실차 재시동·휴대전화 푸시 수신, 별도 저장소 백업, 장애 이메일 실제 수신, 전체 운영 지표 대시보드는 미확인 또는 미구현 범위다.

상세 범위는 [복구·ETA·권한 검증](https://github.com/majung-team/majung-backend/blob/main/docs/stability-verification.md)과 [경유지 좌표 대응](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/waypoint-coordinates.md)에서 확인한다.

---

## 3. 검증 현황

개발 완료와 검증 완료를 동일하게 취급하지 않는다.

| 검증 항목 | 현재 상태 | 비고 |
| --- | --- | --- |
| 주요 기능 코드 반영 | 반영 | 기능별 PR·커밋 참고 |
| 자동화 테스트 | 일부 수행·통과 | 테스트별 실행 범위 확인 필요 |
| 시험 서버 | 주요 기능 검증 | 공개 운영 상태와 구분 |
| 공식 Pleos 에뮬레이터 | 주요 기능 검증 | SDK·경로·화면 동작 |
| 실제 iPhone Chrome | 일부 확인 | 로그인·탭 이동·검색·재접속 |
| 실제 휴대전화 전체 픽업 | 미완료 | 서로 다른 계정의 전체 이용 흐름 |
| 실제 푸시 알림 배너 | 미완료 | iPhone 홈 화면 웹 앱 등 |
| GPS·위치 권한 | 추가 확인 필요 | 실제 휴대전화 환경 |
| LTE·Wi-Fi 전환 | 미완료 | 끊김·복구·상태 최신성 |
| 실차 주행 제한·TTS | 미완료 | 실제 차량 및 오디오 환경 |
| 실제 차량 전체 픽업 | 미진행 | 별도 검증 필요 |
| 익명 이용 지표 | 미구현 | 후속 개발 항목 |

테스트를 수행한 항목이라도 실제 기기나 차량에서 동일하게 동작한다고 단정하지 않는다.

---

## 4. 남은 작업과 알려진 이슈

### 4.1 실제 휴대전화 전체 흐름

현재 iPhone Chrome에서 일부 화면과 기능을 확인했지만, 다음 과정은 통합 검증이 필요하다.

- 서로 다른 계정 간 픽업 요청 및 수락
- 실제 휴대전화에서 위치·ETA 확인
- 준비 상태의 차량 반영
- 장소 변경 제안 및 확정
- 탑승 완료 후 위치 공유 종료
- 네트워크 전환 및 재접속

### 4.2 알림·GPS

- 실제 휴대전화 푸시 배너 수신
- iPhone 홈 화면 웹 앱 알림 동작
- 위치 권한 및 GPS 확인
- LTE·Wi-Fi 환경 전환

### 4.3 SDK 및 차량 환경

- 내비게이션 도착 이벤트 재현
- 장시간 실행 및 연결 복구
- 안내 취소 후 상태 정리
- SDK 원본 좌표 응답 확인
- 실제 차량의 주행 제한 및 TTS 동작

**알려진 이슈**

[차량 이슈 #56](https://github.com/majung-team/majung-vehicle-app/issues/56)

내비게이션 취소 이후 안내 상태가 유지되는 현상은 추가 확인이 필요하다.

### 4.4 개인정보 및 이용 지표

**반영된 내용**

- 차량 최초 이용 시 위치 공유 안내
- 동의 전 공유 차단
- 동의 철회 흐름
- 공개 개인정보 안내 페이지

관련 근거:
- [차량 #76](https://github.com/majung-team/majung-vehicle-app/pull/76)
- [웹 #67](https://github.com/majung-team/majung-web/pull/67)
- [개인정보 안내](https://majung.cloud/privacy)

**후속 개발**

- 익명 이용 지표 수집
- 픽업 완료율 및 재사용률 분석
- 반복 연락 감소 효과 검증

실제 측정하지 않은 사용자 수, 이용률 및 효과는 성과로 제시하지 않는다.

---

## 5. 운영 및 배포 현황

마중은 GCP 기반 전용 VM에서 백엔드 서비스를 운영하며, PostgreSQL과 Caddy를 사용한다.

서버와 모바일 웹은 GitHub Actions를 통해 배포한다.

### Blue-Green 배포

백엔드는 내부 Blue-Green 방식으로 새로운 애플리케이션 버전을 배포한다.

1. 기존 서버가 요청을 처리하는 동안 새 버전 실행
2. 새 버전의 상태 확인
3. 정상 동작 확인 후 Caddy 연결 대상 전환
4. 이전 버전 종료 및 정리

PostgreSQL 기반 공유 세션으로 배포 전환 시 로그인 상태의 연속성을 지원한다.

SSE 연결은 전환 과정에서 재접속할 수 있으며, 모바일 웹은 최신 상태를 다시 조회한다.

다만 VM·DB 장애까지 대응하는 완전한 고가용성 구성은 아니다.

### 운영 및 배포 기록

- [운영 버전과 배포 전환](runtime-2026-10-29.md)
- [서버 Blue-Green PR #45](https://github.com/majung-team/majung-backend/pull/45)
- [웹 배포 PR #71](https://github.com/majung-team/majung-web/pull/71)
- [차량 APK Releases](https://github.com/majung-team/majung-vehicle-app/releases)

현재 접속 가능한 버전과 문서 작성 시점의 버전은 다를 수 있으므로, 실제 운영 버전은 서버 상태와 웹 `release.json`을 통해 확인한다.

---

## 6. 개발 기여 이력

마중은 송승준과 안성모가 함께 시작한 팀 프로젝트다.

초기 역할 분담은 개발 계획의 일부로 유지하고, 실제 구현 기여는 GitHub 코드 작성 이력과 PR을 기준으로 구분한다.

### 안성모의 주요 기여

- 참여 및 연결 해제 관련 코드
- 끊김·위치 정보 최신성 처리 초안
- 지도 및 장소 검색 초기 설정
- 도착 임박 푸시 기능 코드
- 장소 변경 제안 기능

관련 기능 중 일부는 이후 송승준이 통합·보완했다.

### 송승준의 주요 기여

- 계정 및 차량 QR 최초 연결
- 다중 픽업 요청 대기열
- 연결된 탑승자 직접 초대
- 차량 내 사람 검색
- 장소 변경 확정 및 버전 관리
- SSE 및 연결 복구 처리
- 준비 상태 수신 확인
- 차량 저장·백그라운드 처리
- 주행 제한 및 TTS
- 픽업 종료·파기 정책
- 서버 운영 및 Blue-Green 배포
- 차량·웹 UI 개선 및 후속 검증

현재 후속 개발과 검증·배포 작업은 송승준이 담당한다.

### 기여 기록 관리 기준

- PR 생성자만으로 실제 코드 작성자를 판단하지 않음
- 커밋 작성자와 변경 파일을 함께 확인
- 초기 기여 코드를 후속 통합자의 단독 구현으로 표기하지 않음
- 역할 계획과 실제 구현 이력을 구분
- 과거 기여와 현재 유지보수 담당자를 별도로 기록

안성모가 작성한 제출 문서 관련 커밋도 기존 이력으로 유지한다.

- `e957386`
- `3e0d658`
- `5cc665f`

[초기 역할 및 개발 기록](revised-plan.md)

---

## 7. 문서 확인 기준

프로젝트 문서는 작성 시점과 실제 검증 범위를 함께 확인한다.

| 문서 | 확인할 내용 |
| --- | --- |
| [설치 및 테스트](team-testing.md) | 현재 접속·설치 및 기능 확인 방법 |
| [운영 버전](runtime-2026-10-29.md) | 기록 당시 운영 버전 및 배포 확인 |
| [초기 개발 계획](revised-plan.md) | 당시 역할 분담과 주차별 계획 |
| [남은 작업](remaining-work.md) | 미완료 개발 및 추가 검증 |
| [제출 자료 안내](submission-guide.md) | 프로젝트 제출용 공개 자료 |

날짜가 붙은 시험 보고서는 해당 시점의 결과로 유지한다.

과거 APK 버전이나 커밋 기록은 최신 운영 버전을 의미하지 않는다.

Notion의 초기 역할 분담 및 진행 자료도 기존 기록과 대조하여 관리한다.

[Notion 역할표](https://app.notion.com/p/10-4026da9ae26a80c6bb08c5550c5d02be)

---

## 8. 현재 개발 단계 요약

2026년 10월 30일 기준, 마중은 차량용 애플리케이션과 모바일 웹, 백엔드 서버의 주요 기능을 구현하고 공식 Pleos 에뮬레이터와 시험 서버에서 핵심 동작을 검증한 상태다.

서명 APK와 공개 서버, 개발 저장소 및 기본 기능 시연 영상을 제공하고 있다.

실제 iPhone Chrome에서 일부 기능을 확인했지만, 실제 휴대전화의 전체 픽업 흐름과 푸시 알림, 네트워크 전환 및 실차 검증은 완료되지 않았다.

향후에는 현재 구현된 픽업 흐름의 안정성을 우선 확보하고, 실제 사용자 환경에서 추가 검증을 진행할 계획이다.
