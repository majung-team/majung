# 마중 개발 기록

마중은 차량 내비게이션의 위치·도착 시간과 탑승자의 준비 상태를 연결하는 픽업 서비스다. 차량 앱과 웹·서버를 만들면서 만난 문제와, 해결 방법을 고르고 시험한 과정을 정리했다.

[프로젝트 소개](https://github.com/majung-team/majung) · [시연 영상](https://github.com/majung-team/majung/blob/main/docs/pickup-demo.md) · [서비스 이용 안내](https://github.com/majung-team/majung/wiki/서비스-이용-안내)

## 기술 회고

### 차량과 내비게이션

- **[경유지 좌표가 0으로 돌아온 이유](https://github.com/majung-team/majung/wiki/Pleos-내비가-경유지-좌표-0을-돌려줄-때-경로는-어떻게-지킬까%3F)**<br>  이름·순서 대신 장소 식별자로 좌표를 복구하기

- **[내비 복귀와 앱 재실행, 같은 복구일까?](https://github.com/majung-team/majung/wiki/앱이-꺼지거나-내비에서-돌아와도-픽업은-이어질까%3F)**<br>  화면 전환과 강제 종료를 나눠 픽업 이어가기

- **[알림을 확인해도 내비는 그대로](https://github.com/majung-team/majung/wiki/내비-화면만-보고-있어도-요청과-메시지를-알릴-수-있을까%3F)**<br>  메시지·요청·장소 제안의 동작과 음성 안내 나누기

### 공유와 실시간 상태

- **[픽업이 끝나면 공유도 끝나야 한다](https://github.com/majung-team/majung/wiki/픽업이-끝나거나-연결을-끊으면-위치-공유는-바로-멈출까%3F)**<br>  종료 후 남는 정보 정리와 도착 시간 이중 차감 방지

- **[준비 완료를 눌렀는데 준비 중으로 돌아갔다](https://github.com/majung-team/majung/wiki/휴대전화-QR-연결과-웹-iOS-실시간-상태는-왜-어긋났을까%3F)**<br>  늦은 응답과 차량·웹·iOS 상태 동기화 다루기

### 배포와 운영

- **[32초 만에 뜬 서버, 30초 만에 실패한 배포](https://github.com/majung-team/majung/wiki/단일-VM-Blue-Green-배포와-예약-백업은-왜-중간에-멈췄을까%3F)**<br>  파일 권한·기동 대기·백업·예약 인증에서 찾은 문제

---

## 설치·사용 방법과 개발 자료

- [서비스 이용 안내](https://github.com/majung-team/majung/wiki/서비스-이용-안내): 사용 흐름·FAQ·시연 조건·버전별 기록
- [차량 앱 설치](https://github.com/majung-team/majung/wiki/차량-앱-설치) · [웹에서 시작하기](https://github.com/majung-team/majung/wiki/웹에서-시작하기) · [함께 테스트하기](https://github.com/majung-team/majung/wiki/함께-테스트하기)
- [개발 현황과 PR 근거](https://github.com/majung-team/majung/wiki/개발-현황과-PR-근거) · [아키텍처](https://github.com/majung-team/majung/blob/main/docs/architecture.md) · [차량 APK](https://github.com/majung-team/majung-vehicle-app/releases)
