"""Render the two current architecture diagrams with no external renderer dependency."""
from html import escape
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / 'docs/diagrams'

def diagram(filename, width, height, title, subtitle, nodes, edges):
    parts = [f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title desc">
<title id="title">{escape(title)}</title><desc id="desc">{escape(subtitle)}</desc>
<defs><marker id="arrow" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><path d="M0 0 L10 4 L0 8 Z" fill="#526673"/></marker></defs>
<rect width="100%" height="100%" fill="#ffffff"/>
<g font-family="'Noto Sans KR', 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif">
<text x="44" y="48" font-size="25" font-weight="700" fill="#173d35">{escape(title)}</text>
<text x="44" y="79" font-size="15" fill="#546570">{escape(subtitle)}</text>''']
    for path, x, y, label in edges:
        parts.append(f'<path d="{path}" fill="none" stroke="#526673" stroke-width="2" marker-end="url(#arrow)"/>')
        if label:
            parts.append(f'<text x="{x}" y="{y}" text-anchor="middle" font-size="14" fill="#3e535f" stroke="#ffffff" stroke-width="6" paint-order="stroke">{escape(label)}</text>')
    for x,y,w,h,title,lines in nodes:
        parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#f3f8f5" stroke="#afc9be" stroke-width="1.5"/>')
        parts.append(f'<text x="{x+w/2}" y="{y+32}" text-anchor="middle" font-size="19" font-weight="700" fill="#173d35">{escape(title)}</text>')
        for i,line in enumerate(lines):
            parts.append(f'<text x="{x+w/2}" y="{y+60+i*23}" text-anchor="middle" font-size="14" fill="#3e535f">{escape(line)}</text>')
    parts.append('</g></svg>')
    (OUT/filename).write_text('\n'.join(parts))

diagram('overview.svg',1320,570,'마중 · 서비스 구성','차량 SDK의 정보와 탑승자 준비 상태를 서버 API로 전달',[
 (40,200,240,120,'Pleos NaviHelper',['목적지 · 현재 위치','도착까지 남은 시간']),
 (365,200,240,120,'차량 앱',['Android Automotive','픽업 생성 · 승인 · 공유']),
 (700,200,240,120,'백엔드 API',['Spring Boot · HTTP','권한 · 상태 · 순번 검사']),
 (1035,200,245,120,'탑승자 웹',['휴대전화 브라우저','참여 · 조회 · 준비 상태']),
 (700,440,240,90,'DB',['로컬 H2 / 배포 PostgreSQL']),
],[
 ('M280 260 H365',322,241,'SDK'),
 ('M605 235 H700',652,213,'차량 정보'),
 ('M700 290 H605',652,341,'준비 상태'),
 ('M940 235 H1035',987,213,'조회 응답'),
 ('M1035 290 H940',987,341,'참여 · 준비'),
 ('M820 320 V440',880,391,'JPA 저장·조회'),
])

diagram('vehicle.svg',1250,850,'마중 · 차량 앱 내부','VehicleApplication이 reader·controller를 공유하며, 전송은 화면 밖 서비스가 담당',[
 (45,155,305,110,'MainActivity',['권한 요청 · 화면 생명주기','생성 · 참여 · 상태 · 완료 화면']),
 (465,155,305,110,'PickupSharingService',['공유 중 알림 · 서버 상태 조회','SDK 5초 조회 · heartbeat']),
 (885,155,320,110,'DestinationReader',['NaviHelper 연결 · 이벤트','목적지 · 위치 · ETA 수신']),
 (885,360,320,110,'VehicleTelemetry',['수신 후 10초 미만','확정 장소와 목적지 대조']),
 (465,360,305,110,'PickupController',['픽업 상태 · 순번 · 전송 제어','완료·취소·만료 시 서비스 중단']),
 (45,555,305,110,'암호화 저장소',['픽업 ID · 토큰 · 다음 순번','위치·ETA는 파일에 저장 안 함']),
 (45,740,305,80,'Android Keystore',['AES-GCM 암호화 키']),
 (465,555,305,110,'PickupApiClient',['단일 worker에서 HTTP 요청','결과는 메인 스레드로 전달']),
 (885,555,320,110,'백엔드 API',['운전자 권한 · 상태 · 순번 검사','차량 정보 저장·조회']),
],[
 ('M350 210 H465',407,192,'서비스 시작'),
 ('M770 210 H885',827,192,'SDK 조회'),
 ('M1045 265 V360',1100,317,'콜백'),
 ('M885 415 H770',827,396,'최신 자료'),
 ('M617 265 V360',690,317,'조회 · 전송'),
 ('M197 265 V415 H465',328,396,'화면 명령'),
 ('M617 470 V555',693,518,'HTTP 요청'),
 ('M465 440 H390 V610 H350',390,539,'전송 전 저장'),
 ('M197 665 V740',260,706,'키 사용'),
 ('M770 610 H885',827,591,'HTTP'),
])
