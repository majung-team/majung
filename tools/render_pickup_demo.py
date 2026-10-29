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
start = float((p / 'device-start.txt').read_text())
frames = [f for f in data['frames'] if f['time'] / 1000 >= start]
assert frames, 'No browser frames from this recording'
events = {e['name']: e['time'] / 1000 - start for e in data['events'] if e['time'] / 1000 >= start}
# Align the first browser capture with the device recorder's host start marker.
concat = ['ffconcat version 1.0', f"file '{p / 'web' / frames[0]['file']}'",
          f"duration {max(0.001, frames[0]['time'] / 1000 - start):.6f}"]
for i, frame in enumerate(frames):
    duration = (frames[i + 1]['time'] - frame['time']) / 1000 if i + 1 < len(frames) else 10
    concat += [f"file '{p / 'web' / frame['file']}'", f'duration {max(.001, duration):.6f}']
concat += [f"file '{p / 'web' / frames[-1]['file']}'"]
(p / 'web.ffconcat').write_text('\n'.join(concat) + '\n')
subprocess.run([args.ffmpeg, '-v', 'error', '-f', 'concat', '-safe', '0', '-i', str(p / 'web.ffconcat'),
                '-fps_mode', 'vfr', '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '20', '-y', str(p / 'web.mp4')], check=True)

def font(size):
    return ImageFont.truetype(str(args.font), size)

base = Image.new('RGB', (1920, 1080), '#f5f7fa')
draw = ImageDraw.Draw(base)
draw.text((40, 22), '마중  |  초대 참여부터 탑승 완료까지', font=font(39), fill='#191f28')
draw.text((40, 88), '차량 앱 · 공식 Pleos 에뮬레이터', font=font(26), fill='#4e5968')
draw.text((1440, 88), '탑승자 웹 · 390px', font=font(26), fill='#4e5968')
draw.text((40, 1052), '2026.10.29 · SDK + 로컬 시험 서버 · 시험용 픽업을 API로 미리 생성 · 웹은 시간별 캡처 · 수동 도착 · 실차/휴대전화 시험 아님 · 대기 편집/무음', font=font(17), fill='#6b7684')
base.save(p / 'base.png')
segments = [
    (events['invite'] - 1, events['join'] + 4,
     '01 · 링크를 열고 약속에 참여', '이름 입력이나 운전자 승인 없이 참여합니다. 초대 링크 원문은 영상에 넣지 않았습니다.'),
    (events['sdk-visible'] - 2, events['ready'] + 5,
     '02 · 차량 도착 시간 확인', 'SDK에서 받은 위치와 남은 시간을 로컬 서버를 거쳐 웹에 표시합니다.'),
    (events['ready-received'] - 2, events['ready-received'] + 5,
     '03 · 준비 완료 전달', '웹에서 바꾼 준비 상태를 차량 앱이 수신합니다. 운전자가 읽었다는 뜻은 아닙니다.'),
    (events['arrive'] - 4, events['arrive'] + 5,
     '04 · 차량에서 도착 알리기', '도착은 시험 중 수동 처리했습니다. 정차한 에뮬레이터이며 자동 도착 판정 시험은 아닙니다.'),
    (events['complete'] - 5, events['complete'] + 7,
     '05 · 탑승 확인 후 공유 종료', '차량에서 완료하면 웹의 위치와 도착 예정 시간도 더 이상 보이지 않습니다.'),
]
# Render each source interval independently so sparse browser frames keep their timestamps.
# Both sources retain original speed; only the listed waiting gaps are removed.
clips = []
args.output.parent.mkdir(parents=True, exist_ok=True)
for i, (a, b, title, description) in enumerate(segments):
    assert 0 <= a < b
    caption = Image.new('RGB', (1920, 88), '#f5f7fa')
    d = ImageDraw.Draw(caption)
    d.text((40, 0), title, font=font(32), fill='#191f28')
    d.text((40, 44), description, font=font(24), fill='#4e5968')
    caption.save(p / f'caption-{i}.png')
    clip = p / f'clip-{i}.mp4'
    filters = [
        '[0:v]crop=1680:1180:860:90,scale=1140:800,setsar=1,setpts=PTS-STARTPTS[car]',
        '[1:v]scale=370:800,setsar=1,setpts=PTS-STARTPTS[web]',
        '[2:v][car]overlay=40:132[withcar]',
        '[withcar][web]overlay=1440:132[paired]',
        '[paired][3:v]overlay=0:945,format=yuv420p[out]',
    ]
    subprocess.run([args.ffmpeg, '-v', 'error', '-ss', str(a), '-i', str(p / 'vehicle.mp4'),
                    '-ss', str(a), '-i', str(p / 'web.mp4'), '-loop', '1', '-i', str(p / 'base.png'),
                    '-loop', '1', '-i', str(p / f'caption-{i}.png'),
                    '-filter_complex', ';'.join(filters), '-map', '[out]', '-t', str(b - a),
                    '-r', '24', '-c:v', 'libx264', '-preset', 'medium', '-crf', '21', '-an', '-y', str(clip)], check=True)
    clips.append(clip)
(p / 'clips.ffconcat').write_text('ffconcat version 1.0\n' + ''.join(f"file '{clip}'\n" for clip in clips))
subprocess.run([args.ffmpeg, '-v', 'error', '-f', 'concat', '-safe', '0', '-i', str(p / 'clips.ffconcat'),
                '-c', 'copy', '-movflags', '+faststart', '-y', str(args.output)], check=True)
(p / 'edit-plan.json').write_text(json.dumps([{'sourceStart': a, 'sourceEnd': b, 'title': title}
                                             for a, b, title, _ in segments], ensure_ascii=False, indent=2))
print('Edited duration:', round(sum(b - a for a, b, _, _ in segments), 2))
print('Output bytes:', args.output.stat().st_size)
