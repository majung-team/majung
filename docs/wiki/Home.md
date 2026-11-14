# 마중 개발 기록

마중은 데리러 가는 사람과 기다리는 사람이 같은 픽업 정보를 보는 서비스다. 탑승자는 웹에서 차량 위치와 도착 시간을 확인하고, 운전자는 내비를 보면서 탑승자의 준비 상태와 메시지를 받는다.

내비를 여는 순간 장소 추가가 멈추거나, 준비 완료를 눌렀는데 화면이 예전 상태로 돌아가면 이 흐름이 끊긴다. 아래는 그 과정에서 만난 문제와 해결 방법을 고른 기록이다.

[프로젝트 소개](https://github.com/majung-team/majung) · [시연 영상](https://github.com/majung-team/majung/blob/main/docs/pickup-demo.md) · [서비스 이용 안내](https://github.com/majung-team/majung/wiki/서비스-이용-안내)

## 기술 회고

<table>
<tr>
<td width="50%" valign="top">
<sub>내비게이션</sub>
<p><strong><a href="https://github.com/majung-team/majung/wiki/Pleos-내비가-경유지-좌표-0을-돌려줄-때-경로는-어떻게-지킬까%3F">좌표는 0인데, 도착 시간은 계산돼 있었다</a></strong></p>
<p>좌표는 제대로 보냈는데 내비에서 읽으면 0이라니. 처음 보낸 좌표를 그대로 써도 괜찮을까?</p>
</td>
<td width="50%" valign="top">
<sub>앱 복구</sub>
<p><strong><a href="https://github.com/majung-team/majung/wiki/앱이-꺼지거나-내비에서-돌아와도-픽업은-이어질까%3F">내비를 켰더니 경유지 추가가 멈췄다</a></strong></p>
<p>경유지를 추가하려고 내비를 열었을 뿐인데, 왜 추가하던 작업까지 멈춰 버렸을까?</p>
</td>
</tr>
<tr>
<td width="50%" valign="top">
<sub>알림과 음성</sub>
<p><strong><a href="https://github.com/majung-team/majung/wiki/내비-화면만-보고-있어도-요청과-메시지를-알릴-수-있을까%3F">메시지만 확인하고, 길안내는 계속 볼 수 없을까?</a></strong></p>
<p>‘지금 내려가요’라는 메시지 하나 확인하려고 마중 앱까지 열어야 할까? 운전자는 계속 길안내를 보고 싶은데.</p>
</td>
<td width="50%" valign="top">
<sub>위치 공유</sub>
<p><strong><a href="https://github.com/majung-team/majung/wiki/픽업이-끝나거나-연결을-끊으면-위치-공유는-바로-멈출까%3F">탑승은 끝났다. 위치 공유도 끝났을까?</a></strong></p>
<p>탑승까지 끝났으니 위치 공유도 끝났겠지? 완료 화면 뒤에 남아 있는 초대 링크와 알림도 살펴볼 필요가 있었다.</p>
</td>
</tr>
<tr>
<td width="50%" valign="top">
<sub>실시간 상태</sub>
<p><strong><a href="https://github.com/majung-team/majung/wiki/휴대전화-QR-연결과-웹-iOS-실시간-상태는-왜-어긋났을까%3F">준비 완료를 눌렀는데, 다시 준비 중?</a></strong></p>
<p>분명 ‘준비 완료’를 눌렀는데 화면은 다시 ‘준비 중’. 저장에 실패한 걸까, 다른 이유가 있는 걸까?</p>
</td>
<td width="50%" valign="top">
<sub>배포와 백업</sub>
<p><strong><a href="https://github.com/majung-team/majung/wiki/단일-VM-Blue-Green-배포와-예약-백업은-왜-중간에-멈췄을까%3F">서버는 32초 뒤 떴고, 배포는 30초 만에 실패했다</a></strong></p>
<p>32초면 정상적으로 켜지는 서버를 30초만 기다리고 있었다니. 배포가 실패한 이유는 서버 코드 밖에 있었다.</p>
</td>
</tr>
</table>

---

## 설치·사용 방법과 개발 자료

- [서비스 이용 안내](https://github.com/majung-team/majung/wiki/서비스-이용-안내): 사용 흐름·FAQ·시연 조건·버전별 기록
- [차량 앱 설치](https://github.com/majung-team/majung/wiki/차량-앱-설치) · [웹에서 시작하기](https://github.com/majung-team/majung/wiki/웹에서-시작하기) · [함께 테스트하기](https://github.com/majung-team/majung/wiki/함께-테스트하기)
- [개발 현황과 PR 근거](https://github.com/majung-team/majung/wiki/개발-현황과-PR-근거) · [아키텍처](https://github.com/majung-team/majung/blob/main/docs/architecture.md) · [차량 APK](https://github.com/majung-team/majung-vehicle-app/releases)
