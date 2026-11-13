**작업 영역:** 백엔드 인프라 · CI/CD · 운영 복구 · **기술 주제:** Blue-Green 배포 실패·권한 격리·외부 백업 복구 · **작성자:** [SeungJunS0ng](https://github.com/SeungJunS0ng) · **기준일:** 2026년 11월 12일(KST)

[마중 Wiki 홈](https://github.com/majung-team/majung/wiki) → 백엔드 인프라 · 운영 → 단일 VM Blue-Green 배포와 예약 백업 복구

마중 백엔드는 GCP 서울 리전의 단일 운영 VM 위에 `blue(127.0.0.1:8081)`와 `green(127.0.0.1:8082)` 두 슬롯을 두고 Caddy 리버스 프록시를 전환하는 Blue-Green 배포 구조를 사용한다(`deploy/release.py`). 두 슬롯은 모두 하나의 PostgreSQL과 JDBC 공유 세션을 바라보고, 활성 슬롯만 `/etc/majung/active-slot`을 읽어 만료·정리 스케줄러를 실행하며, 매일 09:30 KST에는 GitHub Actions가 최신 DB 백업을 비공개 Cloud Storage로 복사·해시 대조한다.

구조를 처음 올리고 실제 운영을 돌리자 개발 환경 단위 테스트에서는 안 보이던 운영 장애 네 건이 차례대로 터졌다. (VM 호스트명, 내부 IP, GCP 프로젝트 번호 등 비공개 인프라 식별자는 공개 문서에 남기지 않는다.)

| 사건 | 원인 · 복구 조치 | 검증 결과 |
| --- | --- | --- |
| **#66<br>만료 멈춤** | `/etc/majung 0700` → 읽기 실패<br>폴더 `0710` · 비밀 `0600` · `runuser` | 재시작 없이 `EXPIRED` 전환<br>`JobSelectorAccessTest` 27 tests |
| **#75<br>기동 32초** | 동시 기동 32초 vs 대기 `30초`<br>후보 대기 `ATTEMPTS = 75` 상향 | `test_candidate_healthy...`<br>Blue-Green 스위칭 통과 |
| **#56<br>백업 손상** | `gcloud` SSH 안내가 `tar` 출력 혼입<br>백업 전 `ssh-keygen -q` 선실행 | Actions `41158789207` 해시 일치<br>`RestoreVerification` 25.68초 통과 |
| **#62<br>WIF 거절** | WIF 조건이 `schedule` 이벤트 차단<br>`backup.yml@refs/heads/main` 허용 | Actions `41401001422` 성공<br>공개 health `UP` 유지 |

---

## 사건 1. 픽업 자동 만료가 통째로 멈췄다 (`#66`, `6dcb8d9`)

**증상**

11월 2일, 차량 위치 보고가 끊긴 진행 중 픽업이 30분을 훨씬 넘겼는데도 `ACTIVE`로 남아 운영 경보가 꺼지지 않았다. 이상하게도 웹·API 응답, PostgreSQL, 백업은 모두 정상이었다.

**원인**

원인은 권한 분리 설계의 사각지대에 있었다. `server.env`와 `deploy-policy.json` 같은 비밀 설정을 보호하려고 설정 폴더 `/etc/majung` 권한을 root 전용 `0700`으로 묶어 뒀는데, 실제 JVM 프로세스를 실행하는 비권한 서비스 계정 `majung`이 `/etc/majung` 디렉터리를 통과조차 못 해 `/etc/majung/active-slot` 파일을 못 읽고 있었다. 활성 슬롯인지 판단할 수 없으니 `blue`도 `green`도 만료 스케줄러를 건너뛰고 있던 셈이다.

**조치와 검증**

PR [#66](https://github.com/majung-team/majung-backend/commit/6dcb8d9cd377ba6641eec5e5bceac5546440360e)(이슈 [#65](https://github.com/majung-team/majung-backend/issues/65))에서 비밀 격리와 슬롯 읽기를 동시에 만족하도록 배포 검사(`ensure_job_selector_readable()`)를 추가했다.

1. `/etc/majung`과 `/etc/majung/active-slot`이 심볼릭 링크가 아닌 일반 파일·디렉터리인지 확인한다.
2. 비밀 파일(`server.env`, `deploy-policy.json`)이 존재하면 반드시 `0600`(소유자 root 전용 읽기/쓰기)인지 먼저 검사한다.
3. 디렉터리 소유 그룹만 `majung`으로 두고 권한을 `0710`(그룹 통과 권한 `x`만 부여)으로 맞춘다.
4. `runuser -u majung -- test -r /etc/majung/active-slot`으로 실제 서비스 계정이 슬롯 파일만 읽을 수 있는지 확인하고, 실패하면 후보 서버를 띄우지 않고 배포를 중단한다.

운영 서버에서는 수동 DB 수정이나 서버 재시작 없이 디렉터리 권한만 복구했다. 직후 만료 스케줄러가 스스로 돌아 마지막 차량 활동 시각 + 30분에 맞춰 미처리 픽업을 `EXPIRED`로 전환했고, 차량 좌표·ETA·미확정 제안·도착 푸시 구독 잔여 0건, 공개 `/api/v1/health/operations` HTTP 200 `UP` 복귀를 확인했다(`JobSelectorAccessTest`, 배포·운영 Python 회귀 시험 27개 통과).

---

## 사건 2. 후보 서버는 32초 만에 떴는데 배포는 30초 만에 실패했다 (`#75`, `3546902`)

**증상**

코드 검증과 JAR 해시·리비전(`BOOT-INF/classes/majung-release.txt`) 검사까지 다 통과한 뒤, 새 슬롯(`majung@{candidate}.service`)을 시작하는 단계에서 `Candidate did not become healthy` 오류로 배포가 멈췄다.

**원인과 조치**

`deploy/release.py`의 `probe()` 기본 재시도 횟수가 `attempts = 30`(1초 간격, 약 30초)이었는데, 단일 VM에서 PostgreSQL과 기존 활성 슬롯이 함께 도는 상태에서는 새 Spring Boot 슬롯이 기동해 `/api/v1/health`가 200을 내기까지 약 **32초**가 걸렸다. 2초 차이로 정상 기동 중인 후보를 실패로 판정해 버린 것이다.

PR [#75](https://github.com/majung-team/majung-backend/commit/35469023c42a39ce52e4c96bea7549586a9a443b)에서는 후보 서버 로컬 기동 확인에만 `CANDIDATE_START_ATTEMPTS = 75`(75회)를 적용하고, `test_candidate_healthy_after_thirty_attempts_can_become_active`에서 32회 `ConnectionRefusedError` 뒤 정상 200 응답이 오면 활성화까지 성공하는지 단위 테스트로 고정했다.

---

## 사건 3. 첫 외부 백업 아카이브가 업로드 전에 깨졌다 (`#56`, `01c0008`)

**증상**

10월 31일, GitHub Actions에서 최신 PostgreSQL 백업을 SSH로 받아 비공개 Cloud Storage로 옮기는 `offsite-backup.py`를 처음 돌렸더니 아카이브 검증 단계에서 거부됐다.

**원인과 조치**

`gcloud` CLI가 실행 환경에서 최초 SSH 키를 자동 생성할 때 안내 문구를 `stdout`으로 출력했고, 그 문자열이 원격 백업 `tar` 바이너리 스트림 맨 앞에 섞여 들어가 아카이브가 깨진 것이었다.

PR [#56](https://github.com/majung-team/majung-backend/commit/01c0008c693cc275f955a1d0bf9c5e2f7ab36349)에서 백업 스트림을 열기 전에 `~/.ssh/google_compute_engine` 존재 여부를 확인하고, 없으면 `ssh-keygen -q ...`를 `stdout=subprocess.DEVNULL`로 선행 생성해 키 생성 출력과 백업 스트림을 분리했다. 이후 Actions 실행([`41158789207`](https://github.com/majung-team/majung-backend/actions/runs/41158789207))에서 업로드 후 재다운로드 SHA-256 일치 검증과 `RestoreVerification` 복원 시험(9개 핵심 테이블·서버 기동·세션/픽업 HTTP 확인 25.68초, 정리 후 임시 DB 0개)을 통과했다.

---

## 사건 4. 예약 백업 Actions가 GCP 인증에서 연달아 거절됐다 (`#62`, `ad1a185`)

**증상**

수동(`workflow_dispatch`)이나 `push`로 돌릴 때는 잘 되던 `Private database backup` 워크플로가 11월 1일과 2일 매일 09:30 KST 예약 실행(`schedule`)에서만 GCP Workload Identity Federation(WIF) 인증 거부로 실패했다.

**원인과 조치**

초기 WIF 속성 조건이 `push`와 `workflow_dispatch` 이벤트만 허용하고 `schedule` 이벤트를 막아 두었기 때문이었다. PR [#62](https://github.com/majung-team/majung-backend/commit/ad1a185fac800da870193db11be5a652fee89520)(이슈 [#61](https://github.com/majung-team/majung-backend/issues/61))에서는 기존 조직·저장소 ID·`main` 제한을 풀지 않고, 예약 실행은 백엔드 저장소의 `.github/workflows/backup.yml@refs/heads/main` 워크플로 참조에 한해서만 추가 허용했다(웹 등 다른 저장소의 `schedule` 실행은 계속 거부).

수정 뒤 실패했던 실행([`41401001422`](https://github.com/majung-team/majung-backend/actions/runs/41401001422))을 재시도해 WIF 인증·백업 업로드·동일 세대 다운로드 SHA-256 대조까지 통과했고, VM 서버 재시작 없이 공개 운영 상태가 HTTP 200 / `UP`으로 복구됐다.

---

## 현재 구조에서 확인한 것과 남은 한계

**확인한 내용**

- 단일 VM Blue-Green 전환, Caddy 트래픽 전환·실패 시 롤백, `/etc/majung` 최소 권한 격리(`0710` / 비밀 `0600`), 75초 기동 대기(`release.py` · `test_blue_green.py`).
- 비공개 Cloud Storage 왕복 해시 대조 및 `RestoreVerification` 격리 DB 복원·HTTP 세션 검증(10월 31일, 25.68초) 및 11월 2일 예약 백업 WIF 복구.

> [!CAUTION]
> **트래픽 롤백과 DB·복원 검증의 경계**:
> - Blue-Green 배포는 애플리케이션 슬롯 간 트래픽을 되돌릴 수 있지만 DB 스키마 마이그레이션 자체를 자동 롤백하지 않는다(확장 스키마 선배포 후 구버전 종료 뒤 제약·삭제하는 2단계 전제).
> - 외부 복사본 SHA-256 해시 일치는 파일 무결성 검증이며, PostgreSQL 실제 기동·복원 시험(`majung-restore-check.timer`)과 서로 대체되지 않는다.

---

## 관련 커밋·문서

- [운영·백업 복구 기록 (`docs/operations-recovery.md`)](https://github.com/majung-team/majung-backend/blob/main/docs/operations-recovery.md) · [Blue-Green 배포 구조 (`docs/blue-green.md`)](https://github.com/majung-team/majung-backend/blob/main/docs/blue-green.md)
- [PR #66: 활성 슬롯 읽기 권한 복구 (`6dcb8d9`)](https://github.com/majung-team/majung-backend/commit/6dcb8d9cd377ba6641eec5e5bceac5546440360e) · [PR #75: 후보 서버 75회 대기 (`3546902`)](https://github.com/majung-team/majung-backend/commit/35469023c42a39ce52e4c96bea7549586a9a443b)
- [PR #56: 백업 전 SSH 키 생성 출력 분리 (`01c0008`)](https://github.com/majung-team/majung-backend/commit/01c0008c693cc275f955a1d0bf9c5e2f7ab36349) · [PR #62: 예약 백업 WIF 복구 (`ad1a185`)](https://github.com/majung-team/majung-backend/commit/ad1a185fac800da870193db11be5a652fee89520)
- [`deploy/release.py`](https://github.com/majung-team/majung-backend/blob/main/deploy/release.py) · [`deploy/offsite-backup.py`](https://github.com/majung-team/majung-backend/blob/main/deploy/offsite-backup.py)
