
# 개발 현황과 PR 대조

**기준일: 2026년 11월 12일**

마중 프로젝트의 기능별 구현 현황, 개발 기여 이력, PR·커밋 근거 및 남은 검증 항목을 정리한 문서다.

차량 앱, 백엔드, 모바일 웹, 프로젝트 문서 저장소의 `main` 브랜치와 병합 PR, 실제 작성 커밋을 기준으로 개발 이력을 관리한다.

초기에는 **송승준과 안성모가 역할을 분담하여 개발**했으며, 현재 후속 개발·검증·배포는 **송승준이 담당**하고 있다.

기여도는 최초 역할 분담이나 PR 생성자만으로 판단하지 않고 실제 작성 코드, 커밋 및 변경 이력을 기준으로 구분한다.

> 이 문서의 코드 반영 여부와 시험 결과는 각 기록의 확인 범위에 한정된다. 기능 구현, 운영 배포, 에뮬레이터 검증, 실제 휴대전화 검증, 실차 검증은 각각 구분한다.

## 11월 12일 · 클라이언트 기능 안정화 및 제출 자료 최신화

| 대상 | 11월 12일 확인 버전 | 확인 경로 |
| --- | --- | --- |
| 백엔드 | `84d9498` (`main` 반영 · 운영 Blue-Green 배포) | [운영 health](https://majung.cloud/api/v1/health) |
| 탑승자 웹 | `0188bd2` | [운영 release.json](https://majung.cloud/release.json) |
| 차량 앱 | `0.2.33-preview` · 버전 코드 35 · 소스 `692eae5` | [APK 및 체크섬](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.33-preview.20261112) |

- 백엔드(`84d9498`): 카카오 로컬 키워드·도로명/지번 주소 통합 검색(`GET /api/v1/places/search`), 회원 탈퇴(`DELETE /api/v1/accounts/me`), 비로그인 참여 대기 이탈(`POST /api/v1/pickups/{id}/leave`), 요청·초대 목록 실시간 SSE(`GET /api/v1/ride-requests/events`)를 운영 서버(`majung.cloud`)에 반영했다 (`./gradlew test` 총 125건 중 `124 passed, 0 failed, 1 skipped` · `deploy` 29 tests 통과).
- 탑승자 웹(`0188bd2`) · iOS 앱(`4f9a132`): 운영 서버(`https://majung.cloud`) 단일 연결 정리, QR 스캔(`AVFoundation`)·재시도 보완, 차량 연결 UI 경합 분리([iOS #29](https://github.com/majung-team/majung-ios/pull/29)) 및 로그인/설정 화면 `마중 앱 이용 안내 다시 보기`를 반영했다 (`majung-web` `104 passed, 0 failed`, `majung-ios` `MajungTests` 26 · `MajungUITests` 6).
- 차량 앱 `0.2.33-preview` (`692eae5`, 버전 코드 35, SHA-256 `97738e46ffef16a8ac42533b9411d5e24501c478e01eb0b75765604671c983fc`): 휴대전화 QR 스캔 직후 자동 계정 확인 팝업과 3초 백그라운드 조회를 포함한 최신 서명 Release APK를 빌드·게시하고, `Pleos Connect v2.1.0` 에뮬레이터(`Pleos_Connect_v2_0`, `2560 × 1440 px / 160 dpi`)에서 덮어쓰기 설치(`versionCode=35`)를 확인했다(`11/10` 내비 연동 시연 영상 `374169dc...`은 `0.2.29-preview` 기준 촬영본으로 유지, 실차 시동/주행 시험은 미완료).

## 11월 10일 · 내비 연동 시연 영상 촬영 및 제출 문서 최신화

- 운영 서버(`3546902`), 탑승자 웹(`30e28d2`), 차량 앱 `0.2.29-preview`(`6d599f1`, 코드 31)를 연결해 약 1분 36초 분량의 시연 영상([`docs/media/pickup-demo.mp4`](media/pickup-demo.mp4), 제출 폴더 `마중_시연영상.mp4`)을 촬영했다.
- 기본 픽업 흐름(0:00~0:45)에 더해, 45초부터 차량 내비게이션과 탑승자 웹을 나란히 보여주고 탑승자가 ‘지금 내려가요’를 보낼 때 내비 화면 위 알림과 에뮬레이터 안내 음성(TTS)이 나오는 장면을 담았다.
- 1분 2초부터 내비 화면 위 알림에서 ‘수락 및 내비 변경’을 눌러 기존 최종 목적지는 유지하고 제안 장소(`광교중앙역`)를 경유지로 추가한 경로가 열린 뒤, 새 경로 안내 시작과 함께 탑승자 웹의 약속 장소도 `광교중앙역`으로 바뀌는 흐름을 확인했다.
- 초대 QR 영역은 `초대 QR 가림`으로 마스킹했고 전체 96프레임(1fps) 바코드 검사에서 노출된 QR이 0건임을 확인했다. 실제 차량 주행과 휴대전화 푸시 수신은 영상에 포함되지 않음을 영상 마지막 카드와 [시연 문서](pickup-demo.md)에 명시했다.

## 11월 9일 · 첫 초대와 픽업 복구

| 대상 | 11월 9일 확인 버전 | 확인 경로 |
| --- | --- | --- |
| 백엔드 | `3546902` · Green 슬롯 · UP | [운영 health](https://majung.cloud/api/v1/health) · [배포 실행](https://github.com/majung-team/majung-backend/actions/runs/42591855886) |
| 탑승자 웹 | `30e28d2` | [운영 release.json](https://majung.cloud/release.json) · [배포 실행](https://github.com/majung-team/majung-web/actions/runs/42593227100) |
| 차량 앱 | `0.2.29-preview` · 버전 코드 31 · 소스 `6d599f1` | [APK 및 체크섬](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.29-preview.20261109) |

- [웹 #110](https://github.com/majung-team/majung-web/pull/110): 지도 오류 시 빈 카드를 숨기고 약속 장소와 다시 불러오기 버튼을 표시한다.
- Chrome 모바일 크기(390px)와 데스크톱(1280px)에서 초대 재시도, 참여, 같은 탭 재접속, 취소, 이전 링크 차단을 확인했다.

- [서버 #73](https://github.com/majung-team/majung-backend/pull/73): 링크로 참여하기 전에 초대가 유효한지 확인한다. 유효한 경우 장소 이름만 보이고, 확인만으로는 초대가 사용되지 않는다.
- [웹 #108](https://github.com/majung-team/majung-web/pull/108): 초대 만료와 일시적인 연결 실패를 구분한다. 로그인 여부에 관계없이 약속 장소 옆에서 장소를 제안할 수 있게 정리하고, 위치나 도착 시간을 아직 받지 못했을 때 안내를 바꿨다.
- [차량 #132](https://github.com/majung-team/majung-vehicle-app/pull/132): 내비에서 돌아온 뒤 장소 제안 확인을 이어간다. Pleos에서 장소 변경, 앱 강제 종료 후 위치·도착 시간 공유 재개, 두 번째 대기 픽업의 경유지 안내와 완료를 확인했다.
- [차량 앱 0.2.29-preview](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.29-preview.20261109) (코드 31): 기존 인증서로 서명했고 Pleos에서 설치·실행했다. 이번 시험에 실제 iPhone이나 실차 시동 종료·재시동은 포함되지 않았다.
- [서버 #75](https://github.com/majung-team/majung-backend/pull/75): 후보 서버 기동이 약 32초 걸려 기존 배포 확인이 먼저 끝났다. 후보 슬롯의 대기 시간만 늘리고 공개 상태 확인과 복구 조건을 유지했다.

## 11월 6일 픽업 화면 변경

- 탑승 준비를 도착 예정 시간 아래에 배치했다. 장소를 다시 제안할 때는 지도와 입력을 같은 구역에서 사용한다. Chrome 390×844에서 선택 메모를 비운 채 제안을 보내고, 지도 위에서 페이지를 스크롤하고, 다른 탭이 상단부터 열리는 것을 확인했다. [웹 PR #96](https://github.com/majung-team/majung-web/pull/96)
- 차량에서는 연결된 사람이 없을 때 QR 초대를 강조한다. 확인할 요청과 대기 인원을 나눠 표시한다. [차량 PR #123](https://github.com/majung-team/majung-vehicle-app/pull/123)
- 서명 APK 0.2.26-preview (코드 28)를 Pleos 에뮬레이터에 업데이트 설치하고 앱을 실행했다. 실차와 초대 화면의 기기 조작 시험은 남아 있다. [릴리스](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.26-preview.20261106)

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
| 퀵 메시지 및 음성 안내 | 송승준 | [서버 #67](https://github.com/majung-team/majung-backend/pull/67) · [웹 #86](https://github.com/majung-team/majung-web/pull/86) · [차량 #112](https://github.com/majung-team/majung-vehicle-app/pull/112) | 탑승자 3종 퀵 메시지 웹 전송 및 차량 화면 노출·스피커 TTS 자동 낭독 |
| 주행 중 백그라운드 알림·자동 도착 | 송승준 | [차량 #112](https://github.com/majung-team/majung-vehicle-app/pull/112) | 내비 주행 중 신규 요청 헤드업 알림 + TTS 음성 안내, 반경 100m 근접 자동 도착 감지 |
| 다이내믹 캡슐 토스트 & 도착 안내 | 송승준 | 웹 커밋 `ee421ad` · `be2b4cb` | 중복 토스트 원천 차단(싱글톤), Apple 다이내믹 아일랜드 다크 글래스 캡슐, 차량 도착 시 상단 자동 스크롤 & 전용 안내 |
| 인라인 장소 제안·세그먼트 스위치 | 송승준 | 웹 커밋 `a8a4375` · `bfc94bd` · `3e74e2b` | 장소 검색 선택 카드(`.selected-place-card`), 초기 플로팅 독·모달 시트 제거 후 인라인 장소 변경 UI 개편(`bfc94bd`), 물리 슬라이더 기반 준비 상태 2구 세그먼트(`준비 중`/`탑승 준비 완료`) 및 iOS 스타일 도착 알림 토글 적용 |
| 차량 정보 배지 | 송승준 | [서버 #67](https://github.com/majung-team/majung-backend/pull/67) · [웹 #86](https://github.com/majung-team/majung-web/pull/86) | 탑승자 화면 상단 매칭 차량 정보(`🚘 ${vehicleName}`) 배지 노출 |
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
| 내비 연결 루프 방지 & 흐름 UX | 송승준 | 차량 커밋 `f39e51b` | 내비 실패 시 [내비게이션 열기] 상시 노출로 경로 설정 직접 유도, [내비게이션 다시 연결] 확인 중 로딩 상태 표시 및 중복 클릭 방지, 목적지 확인 완료(✓) 및 탑승자 초대 안내(👉) 단계별 UX 확립 |
| GCP·자동 배포 | 안성모 초기 역할 분담, 송승준 실제 운영 구현 | [서버 #9](https://github.com/majung-team/majung-backend/pull/9) · [웹 #7](https://github.com/majung-team/majung-web/pull/7) · [서버 #45](https://github.com/majung-team/majung-backend/pull/45) · [웹 #71](https://github.com/majung-team/majung-web/pull/71) | 전용 VM, PostgreSQL, Caddy, GitHub Actions, Blue-Green 배포 및 JDBC 공유 세션 |

---

## 2. 최근 주요 변경 사항

### 2026년 11월 6일 · 첫 픽업 권한 안내와 도착 시간 표시

- [차량 #121](https://github.com/majung-team/majung-vehicle-app/pull/121): 첫 픽업의 Android 위치 권한 요청과 허용 후 공유 자동 시작. 거절 시 반복 요청 차단·설정 이동, 복구 확인·주행 제한 유지. 단위 시험 69개·lint·빌드 통과.
- [웹 #92](https://github.com/majung-team/majung-web/pull/92): 도착 시간을 지도 밖 위쪽에 크게 표시하고 메시지 성공 문구를 ‘메시지를 보냈어요’로 정리. 준비 상태는 전송·대기·차량 수신을 구분한다. 시험 89개, Chrome 390×844 가로 넘침 없음 확인, 운영 배포 완료.
- 제출 APK 0.2.25-preview / code 27. 사용자 수정 신청서는 변경하지 않고 APK·해시·버전 파일만 교체했다.
- 권한 창은 사용자가 허용해야 한다. 이번 권한 정책 시험과 실제 차량의 최초 사용·시동 시험을 구분한다.


### 2026년 11월 5일 · 중요한 상태 안내와 픽업 완료 조작

- [웹 #90](https://github.com/majung-team/majung-web/pull/90), main `e9457ec`: 입력 중 도착·취소 갱신, 메시지 50자·실패 초안 보존, 오래된 ETA 안내, 지도 실패 시 장소 제안 유지, 알림의 픽업별 이동, 새 초대 표시·차량 다시 찾기 반영. Node 시험 87개 통과.
- [서버 #69](https://github.com/majung-team/majung-backend/pull/69), main `5f96368`: 실제 도착 알림을 3분 전 알림과 구분하고 픽업별 이동 경로를 암호화해 전송. Java 시험 115개·배포 시험 27개 통과. 운영 DB는 백업 후 nullable 컬럼 4개를 먼저 추가했다.
- [차량 #119](https://github.com/majung-team/majung-vehicle-app/pull/119), main `214f2ad`: 도착·탑승 완료 버튼 하단 고정, 받은 요청 수·다음 대기자 요약. 단위 시험·lint·빌드 및 공식 Pleos 2.1 에뮬레이터 `PickupActionDockTest` 1개 통과.
- 제출 APK [0.2.24-preview / code 26](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.24-preview.20261105) 게시. 기존 인증서와 일치하며 제출 폴더의 APK도 교체했다.
- [상세 기록](critical-feedback-2026-11-05.md). 브라우저 캡처는 시험 데이터 화면이다. 실제 휴대전화 푸시·실차 검증 및 기존 기기 시험 종료 대기 이슈 #116 해결을 뜻하지 않는다.


### 2026년 11월 5일 · 픽업 화면 사용성 보완

- [웹 #88](https://github.com/majung-team/majung-web/pull/88), main `1cbd1c2`: 새 요청 지도 높이 복구, 장소·주소 요약, 준비 상태·메시지 조작 통일, 중복 전송 차단·실패 재시도, 동일 이름 차량 구분. 회귀 시험 81건 통과, 운영 배포 확인.
- [차량 #115](https://github.com/majung-team/majung-vehicle-app/pull/115), main `fcd03dc`: 보조 버튼 구분, 요청 상태·행동 표시, 이름과 긴 메시지 분리, 기본 스크롤 적용. 단위 시험·Lint·debug/시험 APK·서명 release 빌드 통과.
- 추가 기기 가독성 시험은 화면 캡처 후 ActivityScenario 종료 시 PAUSED 대기로 실패했다. 전체 통과로 기록하지 않으며 [차량 #116](https://github.com/majung-team/majung-vehicle-app/issues/116)에서 추적한다.
- 제출 APK `0.2.23-preview`, versionCode 25. [Releases](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.23-preview.20261105)의 sourceRevision과 SHA256SUMS로 소스·파일을 대조한다.
- 브라우저 화면 검사는 Chrome 데스크톱과 390×844에서 로컬 시험 API·실제 카카오 지도로 진행했다. 운영 계정 화면의 차량 구분 표시도 확인했다. 실제 휴대전화·트랙패드 체감·실차 시험 완료라는 의미는 아니다.


### 2026년 11월 5일 · 내비 주행 중 헤즈업 알림(HUN) 전면 복구 & 탑승자 웹 슬라이딩 스위치 UX 개편

- **차량 앱 내비게이션 주행 중 플로팅 헤즈업 알림(Heads-Up Notification / HUN) 전면 복구 (`c0cbcdd`)**:
  - **탑승자 퀵 음성 메시지 헤즈업 팝업**: 드라이버가 내비게이션 화면을 보고 있을 때 탑승자가 전송한 음성 메시지가 상태바에만 조용히 쌓이지 않고 화면 상단에 플로팅 헤즈업 카드로 즉각 팝업되어 터치 또는 [확인] 액션으로 즉각 인지 보장.
  - **신규 픽업 요청 및 장소 변경 제안 헤즈업 팝업**: 새로운 픽업 요청 및 장소 변경 시 `setFullScreenIntent(openApp, true)`와 인터랙티브 액션 버튼(`[확인]`, `[요청 확인]`, `[수락 및 내비 변경]`)을 추가하여 안드로이드 오토모티브(AAOS) 및 일반 안드로이드에서 확실한 상단 카드 노출 보장.
  - **알림 채널 및 권한 고도화**: 신규 고우선순위 채널 `pickup-alerts-v2` 도입 (`IMPORTANCE_HIGH`, 진동 패턴 `[0, 300, 150, 300]`, 라이트, DND 우회, 공개 잠금화면 노출), `VIBRATE` 및 `USE_FULL_SCREEN_INTENT` 매니페스트 권한 추가, 알림 발송 직전 기존 ID 캔슬 후 재발송 처리.
  - **정식 서명 Release APK(`v0.2.22-preview / versionCode 24`)**: 신규 태그(`v0.2.22-preview.20261105`) 생성 및 GitHub Releases 바이너리/해시/릴리스 메타데이터 게시 완료 (SHA-256: `31fc9b6f782d285a21f3deb997c4a101435d28cb47b6dc7019badb359ac3a884`).

- **모바일 웹 탑승 준비 슬라이딩 세그먼트 & 도착 알림 iOS 스타일 토글 스위치 개편 (`3e74e2b`)**:
  - **탑승 준비 상태 물리 슬라이더(`readiness-slider`) 도입**: 좌우 버튼 전환 시 DOM 깜빡임 없이 슬라이딩 알약이 스프링 물리 애니메이션(`cubic-bezier(0.2, 0.8, 0.2, 1)`)으로 매끄럽게 좌우로 미끄러지도록 개편. 클릭 즉시 60fps로 반응하는 즉각적 피드백 구현.
  - **도착 3분 전 알림 iOS/토스 스타일 토글 스위치 도입**: 어색한 텍스트 버튼(`[도착 알림 받기]`/`[알림 끄기]`)을 세련된 On/Off 슬라이더 스위치(`.push-toggle-switch`)로 전환. 탭 시 원형 썸이 좌우로 스르륵 미끄러지며 트랙 색상이 자연스럽게 전환.
  - Node 단위 테스트 76개 100% 통과 및 `majung.cloud` 배포 완료 (`3e74e2b`).

- **모바일 웹 인라인 장소 변경 & 프로덕션 모빌리티 UX 고도화 (`bfc94bd`)**:
  - **지도를 가리던 모달 팝업 시트 및 플로팅 독 전면 제거**: 실시간 지도 트래킹 화면 위에 떠 있던 하단 플로팅 독과 지도를 어둡게 가리던 바텀 시트 팝업을 제거하고, 카카오 T / 토스 / 우버 스타일의 직관적인 인라인 단일 흐름 UX로 전면 개편.
  - **장소 제안 1-터치 인라인 전개 (중복 클릭·팝업 닫힘 버그 원천 해결)**: 약속 장소 카드 내 `[장소 변경]` 클릭 시 팝업 창이 닫혀서 다시 눌러야 했던 UX 결함을 완전히 해결. 약속 장소 카드 바로 아래에 검색/지도 핀 선택 패널이 인라인으로 매끄럽게 펼쳐짐.
  - **프로덕션급 미니멀 세그먼트 컨트롤**: AI 목업 느낌의 이모지(`⏳`, `✓`)와 네온 테두리, 긴 설명 문구를 전면 삭제하고, 세련된 뉴트럴 트랙과 블루 액센트의 미니멀 2구 세그먼트 컨트롤(`[ 준비 중 ]` / `[ 탑승 준비 완료 ]`)로 교체.
  - **원터치 퀵 음성 칩 & 직접 입력 TTS 상시 노출**: 팝업 없이 화면에서 즉시 1-터치로 운전자 차량 스피커에 음성을 전송할 수 있는 퀵 칩(`[지금 내려가요]`, `[1번 출구 앞이에요]`, `[곧 도착해요]`) 및 직접 입력 전송 폼을 본문에 상시 배치.
  - Node 단위 테스트 76개 100% 통과 및 `majung.cloud` 배포 완료 (`bfc94bd`).

- **장소 제안 검색 결과 접기 & 선택 카드화 UI**:
  - 검색 결과에서 특정 장소를 선택하면 길게 늘어지던 4개 검색 리스트와 페이징 바가 즉시 사라지고 콤팩트한 **[선택된 장소 카드]**(`📍 의왕역 1호선 · 경기 의왕시 철도박물관로 66 [다른 장소 검색]`)로 축소되어 스크롤 없이 상세 메모 입력 및 제안 전송 가능.
  - 검색창을 돋보기 아이콘 일체형 모바일 검색 바로 개선하고, "검색어는 카카오에 전달돼요...", "‘...’ 검색 결과 · 이름과 주소를 확인해주세요.", "📡 차량 전송 완료" 등 경직되고 AI스러운 문구 전면 정돈.
- **탑승 준비 상태 2구 세그먼트 스위치 컨트롤 (버튼 인지성 강화)**:
  - 기존 텍스트 뱃지 형태의 오해 요소를 제거하고, 대형 2구 세그먼트 토글 스위치(`[⏳ 아직 준비 중 (출발 준비하고 있어요)]` / `[✓ 탑승 준비 완료! (약속 장소에 나왔어요)]`)로 개편하여 누구나 즉시 누를 수 있는 인터랙티브 컨트롤로 완성.
  - 준비 완료 선택 시 에메랄드 그린 전환 및 `✓ 운전자 네비에 '준비 완료' 표시 중` 실시간 상태 피드백 노출.
  - `majung.cloud` 배포 완료 (`a8a4375`, Node 단위 테스트 76개 100% 통과).
- **차량 앱 로딩 띠 복구, 내비 확인 중 버튼 노출 차단(무한 루프 방지) 및 UI 문구 정돈**:
  - **가로 로딩 띠(ProgressBar) 시각 피드백 복구**: 내비게이션 연결 및 경로 확인 중일 때 상단 가로 인디케이터 띠를 항상 노출하여 드라이버에게 실시간 연동 진행 상황을 직관적으로 제공.
  - **내비게이션 재연결 무한 루프 원천 차단**: [내비게이션 열기]를 눌러 목적지를 설정하고 복귀했을 때, 확인 중인 상태(`isVerifying`)에서는 [내비게이션 열기] 버튼을 절대 노출하지 않도록 차단하여 반복 터치 및 무한 루프 방지.
  - **차량 UI 문구 정돈 & AI풍 어색한 문구 전면 제거**: 불필요한 이모지(`👉`)와 설명조 문장("오른쪽에서 탑승자를 초대해주세요", "장소를 받았어요...", "초대 버튼이 열려요")을 전면 삭제하고 간결하고 명확한 1줄 도착 예정 카드(`약 OO분 후 도착 예정`)로 정돈 (`87f3de8`).
  - 최신 정식 서명 Release APK(`v0.2.21-preview / versionCode 23`, SHA-256: `ca56a2a12f078c9e7f94280dbbcddadcc29bbb9915466b794fdf91a463b4afb6`): 신규 태그(`v0.2.21-preview.20261105`) 생성 및 GitHub Releases 바이너리/해시/릴리스 메타데이터 게시, 공식 Pleos 에뮬레이터 검증 완료.

### 2026년 11월 5일 · 내비 경로 불일치 복구·싱글톤 다이내믹 캡슐 토스트 및 차량 도착 UI 고도화

- **모바일 웹 모빌리티 표준 '지도 상시 노출(Hero Map) + 컴팩트 원터치 액션 시트' 레이아웃 개편 (방안 A)**:
  - **Hero Map 상시 노출 & 지도 위 플로팅 ETA 캡슐**: 지도를 상단에 항상 고정 노출하고, 지도 상단 중앙에 실시간 플로팅 캡슐(`🟢 약 3분 후 도착 · 🚘 마중 차량`)을 띄워 언제나 차량 위치와 도착 예정 시간을 시선 분산 없이 확인 가능.
  - **한 줄 요약 바 (장소 & 상태)**: 만날 장소(`📍 서울역 1번 출구`)와 탑승 준비 상태(`✓ 탑승 준비 완료`)를 미니멀한 1줄 카드로 통합.
  - **원터치 액션 데크 (Action Deck)**: 탑승자의 핵심 조작(`[탑승 준비 완료]` + `[⚡ 지금 내려가요]`)과 퀵 칩을 가로 버튼 그룹으로 배치하여, 스크롤 없이 첫 화면에서 즉시 운전자 차량 스피커로 TTS 음성 전달 가능.
  - **접이식 하단 서랍 (Collapsible Drawer)**: 메시지 직접 입력 폼, 장소 제안(`proposalBlock`), 도착 푸시 설정 등 부가 기능을 하단 접이식 드로어(`<details>`)로 수납하여 전체 세로 길이를 50% 이상 압축(스크롤 프리 한 화면 완결형 UX 완성).
  - Node 단위 테스트 76개 전체 통과 및 `majung.cloud` 실시간 운영 배포 완료 (`5a6cfd6`).
- **내비 경로 불일치 복구 및 최종 목적지 재선택**: 이전에 선택한 경유지를 내비에서 찾지 못하거나 경로 불일치 시 안내와 함께 `[최종 목적지에서 만나기]` 버튼을 제공하여 즉시 최종 목적지로 복구 및 초대 재개 지원 ([차량 #113](https://github.com/majung-team/majung-vehicle-app/pull/113)).
- **신규 픽업 요청 시 기존 내비 경로 보존**: 안내 중인 경로가 없을 때(`confirmedNoGuidance`)만 최종 목적지로 설정(`navigateToPickup`)하고, 기존 내비 경로가 있을 때는 요청 장소를 경유지로만 추가(`addPickupWaypoint`)하여 기존 목적지/경로를 안전하게 보존.
- **최신 정식 서명 Release APK(`v0.2.20-preview / versionCode 22`, SHA-256: `63d8c0b02133a3fe02105ee4788a449437ad771600dacfdb7265d40c6c521fd0`)**: 태그 갱신, GitHub Releases 바이너리/해시 업로드 및 공식 Pleos 에뮬레이터 설치·기동 검증 완료.
- **탑승자 모바일 웹 싱글톤 토스트 & Apple 다이내믹 아일랜드 캡슐 개편**: 메시지 전송 시 연속 중복 노출 버그를 수정하고, 새 토스트 발생 시 기존 토스트를 즉시 대체하는 싱글톤 타이머 매니저 도입. 클래스 충돌을 방지하기 위해 `.toast-symbol`로 분리하고, 다크 프로스티드 글래스(`rgba(18, 24, 38, 0.94)`, 블러 20px, 에메랄드 테두리 글로우, 탭하여 닫기) 디자인 적용 (`ee421ad`, `be2b4cb`).
- **차량 도착 시 화면 상단 자동 스크롤 & 도착 전용 안내 강화**: 차량이 약속 장소에 도착(`ARRIVED`)했을 때 모바일 화면이 상단으로 부드럽게 자동 스크롤되며, 전용 도착 알림(`🚘 차량이 약속 장소에 도착했어요!`)과 햅틱 진동 피드백이 발생하고 본문 중복 문구를 깔끔한 단일 안내 카드(`<p class="arrival-notice">`)로 정돈하여 배포 완료 (`be2b4cb`).

### 2026년 11월 4일 · 목적지 부재 시 즉시 목적지 설정·탑승자 피드백 강화 및 0.2.20-preview 배포

- **내비 목적지 부재 시 즉시 목적지 설정**: 기존 목적지가 없는 상태에서 탑승자 마중 요청 수락 시 경유지가 아닌 최종 목적지로 즉시 경로를 탐색하여 바로 안내 시작 (`navigateToPickup`).
- **내비 목적지 없음 화면 안내 및 [내비게이션 열기] 원터치 액션**: 내비게이션 목적지가 없을 때 대기 화면의 무한 로딩 스피너를 제거하고, 명확한 상태 안내와 함께 원터치로 내비게이션 앱을 기동할 수 있는 바로가기 버튼 제공.
- **7대 아키텍처 계층 패키지 분리 및 Kotlin 패키지 선언 100% 일치화**: `ui`, `navi`, `domain`, `data.api`, `data.storage`, `service`, `util` 7대 계층으로 패키지 구조를 분리 정위치화하여 IDE 컴파일 및 아키텍처 안정성 극대화.
- **탑승자 모바일 웹 실시간 피드백 및 플로팅 토스트/햅틱 지원**: 장소 제안 시 즉각적인 전송 상태 피드백(`제안 보내는 중…`), 플로팅 토스트, 제안 수락 시 SSE 실시간 알림 및 모바일 진동 햅틱 피드백 적용.
- **내비 경로 불일치 복구 및 기존 경로 보존**: 이전에 선택한 경유지 좌표 불일치 시 `[최종 목적지에서 만나기]` 버튼을 통한 즉시 복구 지원, 신규 요청 수락 시 기존 내비 경로가 있을 때는 경유지로만 추가(`addPickupWaypoint`)하여 기존 목적지 보존.
- **최신 정식 서명 Release APK(`v0.2.20-preview / versionCode 22`, SHA-256: `63d8c0b0...`)** 빌드, GitHub Releases 게시 및 공식 Pleos 에뮬레이터 설치·기동 검증 완료.

### 2026년 11월 3일 · 목적지 확인 2초 단축·주행 중 헤즈업 원터치 수락·아키텍처 리팩터링 및 0.2.19-preview 배포

- **내비게이션 목적지 검증 및 준비 대기 간격 2초 단축**: `RouteValidation`의 경로 재탐색 대기 간격을 5초에서 2초로 단축하고 `DestinationReader` 폴링 주기를 최적화하여 탑승자 장소 제안 수락 시 목적지 확정 체감 딜레이를 대폭 개선.
- **주행 중 장소제안 헤즈업 원터치 수락 지원**: 주행 중 탑승자가 새로운 장소를 제안했을 때 화면 상단 헤드업 알림에 `[수락 및 내비 변경]` 원터치 액션을 제공하여 내비 지도를 벗어나지 않고 백그라운드에서 즉시 경로를 변경 및 자동 확정.
- **차량 앱 패키지 클린 아키텍처 리팩터링**: `ui`, `domain`, `data`, `navi`, `util`, `service` 계층형 모듈화 분리로 유지보수성 및 확장성 향상.
- **백엔드 멀티 픽업 플랜 API 및 계층 리팩터링**: 9종 다중 픽업 플랜 API 구현 및 표준 Spring Boot 계층 패키지 구조 분리 완료.
- **최신 정식 서명 Release APK(`v0.2.19-preview / versionCode 21`, SHA-256: `e7a944c1...`)** 빌드, GitHub Releases 게시 및 공식 Pleos 에뮬레이터 설치·기동 검증 완료.

### 2026년 11월 2일 · 내비 주행 중 백그라운드 픽업 알림·자동 도착·탑승자 퀵 메시지 및 릴리스 배포

- 내비 주행 중 백그라운드 픽업 요청 감지 & 헤드업 알림 + TTS 음성 안내, 픽업 장소 반경 100m 자동 도착 감지, 탑승자 3종 퀵 메시지("지금 내려가요", "1번 출구 앞이에요", "곧 도착해요") 실시간 노출 및 차량 TTS 자동 낭독 반영 ([차량 #112](https://github.com/majung-team/majung-vehicle-app/pull/112)).
- 탑승자 퀵 메시지 수신 및 스냅샷 전달 API(`POST /api/v1/pickups/{id}/quick-message`, `POST /api/v1/ride-requests/{id}/quick-message`), 매칭 차량 정보(`vehicleName`) 스냅샷 연동 및 블루·그린 운영 서버 무중단 배포 완료 ([서버 #67](https://github.com/majung-team/majung-backend/pull/67)).
- 탑승자 모바일 화면 차량 정보 배지(`🚘 ${vehicleName}`), 3종 퀵 메시지 칩 원터치 전송 및 전송 완료 힌트 UI 추가 후 `https://majung.cloud` 배포 완료 ([웹 #86](https://github.com/majung-team/majung-web/pull/86)).
- 최신 정식 서명 Release APK(`v0.2.18-preview / versionCode 20`, SHA-256: `69dd9c10...`) 빌드 및 GitHub Releases 자산 게시 완료. 공식 Pleos 에뮬레이터 설치 및 기동 검증.

### 2026년 11월 2일 · 지도 표시·예약 백업 복구·핵심 흐름 예외 보강

- 운영 자동 만료 중단 원인 확인·복구: 서버 계정이 활성 슬롯 파일을 읽지 못해 정기 작업을 건너뛰었다. 비밀 파일 0600은 유지하고 서비스 그룹에 설정 폴더 통과 권한만 허용했다. 기존 픽업의 자동 EXPIRED 전환과 위치·제안·푸시 정리, 운영 health UP 복구 확인. 배포 전 읽기 검사·권한 회귀 27개 추가 ([서버 #66](https://github.com/majung-team/majung-backend/pull/66)). 수동 픽업 취소·DB 수정·서비스 재시작 없이 복구했다.

- 차량 #92 복구 수정과 #96·#99·#101·#105·#109 UI·UX 및 제안 장소 경로 확인·자동 확정·백그라운드 알림·전체 사용자 흐름 다듬기가 포함된 [0.2.18-preview / code 20](https://github.com/majung-team/majung-vehicle-app/releases/tag/v0.2.18-preview.20261103) 게시. 소스 `2b7076b`, 이전 인증서 일치·0.2.16 업데이트 설치·Activity 기동·게시 파일 다운로드 해시 대조 확인. 실제 시동 시험은 별도 ([차량 #94](https://github.com/majung-team/majung-vehicle-app/pull/94) · [차량 #96](https://github.com/majung-team/majung-vehicle-app/pull/96) · [차량 #99](https://github.com/majung-team/majung-vehicle-app/pull/99) · [차량 #101](https://github.com/majung-team/majung-vehicle-app/pull/101) · [차량 #105](https://github.com/majung-team/majung-vehicle-app/pull/105) · [차량 #109](https://github.com/majung-team/majung-vehicle-app/pull/109))
- 모바일 웹 차량 연결 대기 안내 카드와 상단 고정 내비게이션 겹침 해소, 픽업 추적 지도·제안 카드·장소 검색 선택 강조 레이아웃 다듬기 반영 및 운영 배포 완료 ([웹 #81](https://github.com/majung-team/majung-web/pull/81) · [웹 #83](https://github.com/majung-team/majung-web/pull/83) · [웹 #85](https://github.com/majung-team/majung-web/pull/85))

- 지도에서 작게 보이던 `차` 글자를 차 아이콘과 `마중 차량` 이름표로 변경. 만날 장소는 초록색 말풍선으로 구분하며 탑승자 현재 GPS 위치로 표시하지 않는다. 오래된 차량 정보는 회색 점선·마지막 위치 문구로 구분한다. Node 테스트 75개, 모바일·PC 크기 SDK 대역 화면 검사 후 웹 배포 완료 ([웹 #77](https://github.com/majung-team/majung-web/pull/77))
- 11월 1일·2일 예약 백업 실패 원인은 GCP WIF가 `schedule` 이벤트를 거절한 것이었다. backend main의 backup.yml 예약 실행만 허용하고 실패한 예약 실행을 재시도해 업로드·다운로드 해시 검증 통과. 운영 점검도 HTTP 200 / UP 복구. 서버 재시작 없음 ([서버 #62](https://github.com/majung-team/majung-backend/pull/62), [예약 재실행](https://github.com/majung-team/majung-backend/actions/runs/41401001422))
- 픽업 도착·종료·만료 시 미확정 장소 제안 정리, 차량 계정 해제·기기 철회·카카오 연결 해제 시 진행 중 픽업 취소·추적 차단·도착 푸시 구독 제거를 보강하고 운영 배포 완료 (`ea55d33`, [서버 #64](https://github.com/majung-team/majung-backend/pull/64))
- 대기 중(`QUEUED`) 요청의 취소 확인 창이 열린 상태에서 픽업이 시작(`ACTIVE`)된 경우 취소 API 호출을 막고 상태 변경 안내를 표시하도록 수정 후 웹 배포 완료 (Node 테스트 76개 통과, [웹 #79](https://github.com/majung-team/majung-web/pull/79))
- 경유지 추가·장소 변경 중 SDK 타임아웃 또는 연결 해제 시 경로 변경 상태를 함께 정리해 재요청 지연을 막고, 앱 재실행 후 차량 좌표 복원 실패 시 서버의 이전 차량 위치를 즉시 삭제하도록 보강 (`PickupRecoveryTest` 에뮬레이터 검증 통과, [차량 #92](https://github.com/majung-team/majung-vehicle-app/pull/92))

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

앱의 경유지 식별자 대조 및 관련 시험은 통과했으나, SDK 원본 좌표가 `0`으로 반환되는 현상은 공식 부분 회신을 받았으나 세부 버전의 수정 완료는 확인되지 않았다.

---

### 2026년 10월 31일

- [차량 #87](https://github.com/majung-team/majung-vehicle-app/pull/87): 기존 경유지의 최근 장소 기반 좌표 복원, 확인되지 않은 경유지 1~2곳 재선택, 순서·최종 목적지 유지. 별도 Pleos 에뮬레이터 SDK·Android 복구 6개 시험 통과, 단위 60개 통과. 원본 `(0,0)` 응답 수정과는 구분한다.
- [서버 #51](https://github.com/majung-team/majung-backend/pull/51): 실제 HTTP 응답을 배포된 웹 계산 코드에 넣는 ETA 호환성 시험 추가. 경과 시간 중복 차감, 30초 STALE, 2분 숨김, 정체로 늘어나거나 줄어든 ETA 반영을 검사한다.
- [서버 #48](https://github.com/majung-team/majung-backend/pull/48)·[#49](https://github.com/majung-team/majung-backend/pull/49): 최신 백업 임시 DB 복원, 테이블 9개 조회·스키마 검증·서버 기동, 주간 복원·매분 운영 점검, 외부 HTTPS 점검 2개와 마중 전용 이메일 정책 적용.
- 일시 503 이후 ACTIVE 상태를 다시 받아도 사용자 확인 전에는 위치 공유를 재개하지 않는 Android 시험 보강.
- 실제 시동 종료·실차 재시동·휴대전화 푸시 수신·경보 이메일 수신은 현장 확인이 남아 있다. 외부 백업과 복원, 내부 API·DB·OAuth 지표 점검은 위 추가 결과에 반영했다. 기간별 대시보드와는 구분한다.

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
| [기술 회고 6편 (Wiki)](https://github.com/majung-team/majung/wiki#7-%EB%AC%B8%EC%A0%9C-%ED%95%B4%EA%B2%B0%EA%B3%BC-%EA%B8%B0%EC%88%A0-%ED%9A%8C%EA%B3%A0) | 경유지 0 우회, 복구, Blue-Green, 동기화 |
| [초기 운영 전환 기록](runtime-2026-10-29.md) | 기록 당시 운영 버전 및 배포 확인 |
| [초기 개발 계획](revised-plan.md) | 당시 역할 분담과 주차별 계획 |
| [남은 작업](remaining-work.md) | 미완료 개발 및 추가 검증 |
| [제출 자료 안내](submission-guide.md) | 프로젝트 제출용 공개 자료 |

날짜가 붙은 시험 보고서는 해당 시점의 결과로 유지한다.

과거 APK 버전이나 커밋 기록은 최신 운영 버전을 의미하지 않는다.

Notion의 초기 역할 분담 및 진행 자료도 기존 기록과 대조하여 관리한다.

[Notion 역할표](https://app.notion.com/p/10-4026da9ae26a80c6bb08c5550c5d02be)

---

## 8. 현재 개발 단계 요약

2026년 11월 12일 기준, 마중은 차량용 애플리케이션(`0.2.33-preview`, 버전 코드 35 · 시연 영상 촬영본 `0.2.29-preview`)과 모바일 웹(`0188bd2`), iOS 앱(`4f9a132`), 백엔드 서버(`84d9498` Blue 슬롯 `UP`)의 주요 기능을 구현하고 공식 Pleos 에뮬레이터와 운영 서버(`majung.cloud`)를 연결한 내비 연동 시연 영상(약 1분 36초)과 서명 APK, 개발 저장소를 제공하고 있다.

실제 iPhone Chrome에서 일부 기능을 확인했지만, 실제 두 대의 휴대전화를 이용한 전체 픽업 흐름과 푸시 알림 배너, 네트워크 전환 및 실차 검증은 완료되지 않았다.

향후에는 현재 구현된 픽업 흐름의 안정성을 우선 확보하고, 실제 사용자 환경에서 추가 검증을 진행할 계획이다.
