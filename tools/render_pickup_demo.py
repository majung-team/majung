#!/usr/bin/env python3
"""Combine an Android recording and timestamped browser captures; trim only waiting gaps."""
import argparse
import json
from pathlib import Path
import subprocess
from PIL import Image, ImageDraw, ImageFont

parser = argparse.ArgumentParser()
parser.add_argument('--capture-dir', type=Path, required=True)
parser.add_argument('--ffmpeg', required=True)
parser.add_argument('--font', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
p = args.capture_dir.resolve()
data = json.loads((p / 'timeline.json').read_text())
frames = data['frames']
start = float((p / 'device-start.txt').read_text())
events = {e['name']: e['time'] / 1000 - start for e in data['events']}
# Align the first browser capture with the device recorder's host start marker.
concat = ['ffconcat version 1.0', f"file '{p / 'web' / frames[0]['file']}'",
          f"duration {max(0.001, frames[0]['time'] / 1000 - start):.6f}"]
for i, frame in enumerate(frames):
    duration = (frames[i + 1]['time'] - frame['time']) / 1000 if i + 1 < len(frames) else 1
    concat += [f"file '{p / 'web' / frame['file']}'", f'duration {max(.001, duration):.6f}']
concat += [f"file '{p / 'web' / frames[-1]['file']}'"]
(p / 'web.ffconcat').write_text('\n'.join(concat) + '\n')
subprocess.run([args.ffmpeg, '-v', 'error', '-f', 'concat', '-safe', '0', '-i', str(p / 'web.ffconcat'),
                '-fps_mode', 'vfr', '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '20', '-y', str(p / 'web.mp4')], check=True)

def font(size):
    return ImageFont.truetype(str(args.font), size)

base = Image.new('RGB', (1920, 1080), '#f3f4ee')
draw = ImageDraw.Draw(base)
draw.text((40, 25), '마중  |  차량과 탑승자가 같은 약속을 확인하는 과정', font=font(39), fill='#173d34')
draw.text((40, 90), '차량 앱 · 공식 Pleos 에뮬레이터', font=font(26), fill='#315f52')
draw.text((1220, 90), '탑승자 웹 · 로컬 브라우저', font=font(26), fill='#315f52')
draw.text((40, 1053), '2026.10.23  ·  실제 SDK + 로컬 서버  ·  웹: 시간별 화면 캡처  ·  실차·주행 검증 아님  ·  대기 구간 편집 / 무음', font=font(18), fill='#59685e')
base.save(p / 'base.png')
segments = [
    (events['create'] - 2, events['create'] + 4,
     '01 · 차량에서 약속 만들기', '내비게이션의 선택 장소로 생성합니다. 탑승자는 좌표를 입력하지 않습니다.'),
    (events['join-form'] - 2, events['join'] + 3,
     '02 · 초대 링크로 참여 요청', '이름만 입력합니다. 운전자 승인 전에는 위치와 장소가 공개되지 않습니다.'),
    (events['approve'] - 1, events['approve'] + 6,
     '03 · 차량에서 참여자 확인 · 승인', '확인 창을 거쳐 공유를 시작합니다. SDK 정보를 서버로 전달합니다.'),
    (events['sdk-visible'] - 7, events['ready'] + 4.5,
     '04 · 예상 시간 확인 · 준비 완료 전달', 'SDK 위치·남은 시간이 웹에 표시되고, 준비 상태가 차량에 반영됩니다.'),
    (events['arrive'] - .6, events['complete'] - .5,
     '05 · 도착 안내 · 공유 유지', '도착은 운전자가 수동 확인합니다. 정차 시연이라 에뮬레이터 ETA는 줄지 않습니다.'),
    (events['complete'] - .5, events['complete'] + 6,
     '06 · 탑승 확인 · 위치 공유 종료', '완료 후 웹의 위치·예상 시간이 사라지고 차량의 접근 정보도 정리됩니다.'),
]
# Keep both sources at their original speed. Hard cuts remove waiting only.
inputs = ['-i', str(p / 'vehicle.mp4'), '-i', str(p / 'web.mp4'), '-i', str(p / 'base.png')]
for i, (_, _, title, description) in enumerate(segments):
    caption = Image.new('RGB', (1920, 88), '#f3f4ee')
    d = ImageDraw.Draw(caption)
    d.text((40, 0), title, font=font(32), fill='#173d34')
    d.text((40, 44), description, font=font(24), fill='#43594d')
    caption.save(p / f'caption-{i}.png')
    inputs += ['-i', str(p / f'caption-{i}.png')]
filters = [
    '[0:v]crop=1680:1200:860:90,scale=1100:786,setsar=1[car]',
    '[1:v]crop=600:720:340:0,scale=650:780,setsar=1[web]',
    '[2:v][car]overlay=40:165[withcar]',
    '[withcar][web]overlay=1220:165:shortest=1[paired]',
    '[paired]fps=24,split=6' + ''.join(f'[pair{i}]' for i in range(6)),
]
for i, (a, b, _, _) in enumerate(segments):
    assert 0 <= a < b
    filters += [f'[pair{i}]trim=start={a}:end={b},setpts=PTS-STARTPTS[clip{i}]',
                f'[clip{i}][{i + 3}:v]overlay=0:960:format=auto[captioned{i}]']
filters += [''.join(f'[captioned{i}]' for i in range(6)) + 'concat=n=6:v=1:a=0,format=yuv420p[out]']
args.output.parent.mkdir(parents=True, exist_ok=True)
subprocess.run([args.ffmpeg, '-v', 'warning', *inputs, '-filter_complex', ';'.join(filters), '-map', '[out]',
                '-r', '24', '-c:v', 'libx264', '-preset', 'medium', '-crf', '21', '-movflags', '+faststart',
                '-an', '-y', str(args.output)], check=True)
(p / 'edit-plan.json').write_text(json.dumps([{'sourceStart': a, 'sourceEnd': b, 'title': title}
                                             for a, b, title, _ in segments], ensure_ascii=False, indent=2))
print('Edited duration:', round(sum(b - a for a, b, _, _ in segments), 2))
print('Output bytes:', args.output.stat().st_size)
