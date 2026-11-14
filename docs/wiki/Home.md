# 마중 개발 기록

마중은 차량 내비게이션의 위치·도착 시간과 탑승자의 준비 상태를 연결하는 픽업 서비스다. 차량 앱과 웹·서버를 만들면서 만난 문제와, 해결 방법을 고르고 시험한 과정을 정리했다.

[프로젝트 소개](https://github.com/majung-team/majung) · [시연 영상](https://github.com/majung-team/majung/blob/main/docs/pickup-demo.md) · [서비스 이용 안내](https://github.com/majung-team/majung/wiki/서비스-이용-안내)

## 문제 해결 · 기술 회고

### [Pleos 내비가 경유지 좌표 0을 돌려줄 때 경로는 어떻게 지킬까?](https://github.com/majung-team/majung/wiki/Pleos-내비가-경유지-좌표-0을-돌려줄-때-경로는-어떻게-지킬까%3F)

정상 좌표를 보냈는데 경유지만 `(0, 0)`으로 돌아왔다. 이름과 순서로 맞추지 않고, 요청과 응답의 장소 식별자를 대조해 복구한 과정.

### [앱이 꺼지거나 내비에서 돌아와도 픽업은 이어질까?](https://github.com/majung-team/majung/wiki/앱이-꺼지거나-내비에서-돌아와도-픽업은-이어질까%3F)

내비가 앞으로 열리자 진행 중인 요청을 마중 스스로 끊고 있었다. 화면 전환과 강제 종료를 구분하고, 공유를 유지할 때와 다시 확인할 때를 나눈 이유.

### [내비 화면만 보고 있어도 요청과 메시지를 알릴 수 있을까?](https://github.com/majung-team/majung/wiki/내비-화면만-보고-있어도-요청과-메시지를-알릴-수-있을까%3F)

메시지의 ‘확인’을 누르면 보던 길안내가 가려질 수 있었다. 알림마다 다른 버튼 동작, 음성 안내, 주행 중 화면 제한을 정리한 기록.

### [픽업이 끝나거나 연결을 끊으면 위치 공유는 바로 멈출까?](https://github.com/majung-team/majung/wiki/픽업이-끝나거나-연결을-끊으면-위치-공유는-바로-멈출까%3F)

완료 상태만 바꾸면 위치와 알림도 끝날까? 종료 뒤 남는 정보와 늦게 도착한 요청을 정리하고, 서버·웹의 도착 시간 이중 차감을 막은 과정.

### [단일 VM Blue-Green 배포와 예약 백업은 왜 중간에 멈췄을까?](https://github.com/majung-team/majung/wiki/단일-VM-Blue-Green-배포와-예약-백업은-왜-중간에-멈췄을까%3F)

32초 뒤 준비된 서버를 배포는 30초 만에 실패로 판단했다. 파일 권한, 기동 대기, 백업 데이터에 섞인 안내 문구, 예약 실행 인증에서 찾은 문제들.

### [휴대전화 QR 연결과 웹·iOS 실시간 상태는 왜 어긋났을까?](https://github.com/majung-team/majung/wiki/휴대전화-QR-연결과-웹-iOS-실시간-상태는-왜-어긋났을까%3F)

준비 완료를 저장했는데 화면은 준비 중으로 돌아갈 수 있었다. 늦은 응답, 실시간 변경 신호, 자동 확인과 수동 시험이 겹치는 상황을 다룬 기록.

---

## 설치·사용 방법과 개발 자료

- [서비스 이용 안내](https://github.com/majung-team/majung/wiki/서비스-이용-안내): 사용 흐름·FAQ·시연 조건·버전별 기록
- [차량 앱 설치](https://github.com/majung-team/majung/wiki/차량-앱-설치) · [웹에서 시작하기](https://github.com/majung-team/majung/wiki/웹에서-시작하기) · [함께 테스트하기](https://github.com/majung-team/majung/wiki/함께-테스트하기)
- [개발 현황과 PR 근거](https://github.com/majung-team/majung/wiki/개발-현황과-PR-근거) · [아키텍처](https://github.com/majung-team/majung/blob/main/docs/architecture.md) · [차량 APK](https://github.com/majung-team/majung-vehicle-app/releases)
