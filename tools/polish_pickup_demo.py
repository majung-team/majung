#!/usr/bin/env python3
"""Correct captions in the 2026-11-10 demo while preserving scene timing and audio."""
import argparse
import hashlib
from pathlib import Path
import subprocess
import tempfile

from PIL import Image, ImageDraw, ImageFont

SOURCE_SHA256 = "374169dc9ca80523c3eb68530ca34f3b69de85763393c4a7e59accccad1c6329"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--ffmpeg", required=True)
    parser.add_argument("--font", required=True)
    args = parser.parse_args()
    if args.input.resolve() == args.output.resolve():
        parser.error("Use a different output path to retain the original recording")
    if hashlib.sha256(args.input.read_bytes()).hexdigest() != SOURCE_SHA256:
        parser.error("Input must be the unedited 2026-11-10 demo; this avoids double editing")
    if args.output.exists():
        parser.error("Output already exists; choose a new path")

    background, foreground, muted = (243, 243, 243), (34, 34, 34), (90, 90, 90)
    version = "촬영 2026.11.10 · 차량 앱 0.2.29-preview  |  제출 APK 0.2.33-preview"
    with tempfile.TemporaryDirectory(prefix="majung-demo-") as temp:
        root = Path(temp)

        def overlay(name, height, lines):
            canvas = Image.new("RGB", (1920, height), background)
            draw = ImageDraw.Draw(canvas)
            for x, y, size, text, color in lines:
                font = ImageFont.truetype(args.font, size)
                if draw.textbbox((x, y), text, font=font)[2] >= 1872:
                    raise ValueError("Caption exceeds the available width")
                draw.text((x, y), text, font=font, fill=color)
            path = root / name
            canvas.save(path)
            return path

        opening = overlay("opening.png", 80, [
            (176, 8, 24, version, muted),
            (176, 44, 21, "Pleos Connect 에뮬레이터 · 운영 서버 연동", muted),
        ])
        pending = overlay("pending.png", 114, [
            (48, 6, 30, "새 경로를 확인하고 있습니다", foreground),
            (48, 65, 23, "경로 확인이 끝나면 탑승자 화면의 약속 장소도 바뀝니다.", muted),
        ])
        confirmed = overlay("confirmed.png", 114, [
            (48, 6, 30, "새 경로가 반영되고 약속 장소가 변경됐습니다", foreground),
            (48, 65, 23, "차량 내비와 탑승자 웹에서 광교중앙역을 확인합니다.", muted),
        ])
        ending = overlay("ending.png", 70, [(160, 17, 23, version, muted)])
        command = [args.ffmpeg, "-hide_banner", "-i", str(args.input)]
        for path in (opening, pending, confirmed, ending):
            command += ["-loop", "1", "-i", str(path)]
        # The source scenes, frame timing and AAC audio remain in their original order.
        filters = (
            "[0:v][1:v]overlay=0:1000:enable='lt(t,9.5)'[v1];"
            "[v1][2:v]overlay=0:966:enable='gte(t,70.9)*lt(t,75.2)'[v2];"
            "[v2][3:v]overlay=0:966:enable='gte(t,75.2)*lt(t,79.65)'[v3];"
            "[v3][4:v]overlay=0:1010:enable='gte(t,79.65)'[out]"
        )
        command += [
            "-filter_complex", filters, "-map", "[out]", "-map", "0:a:0",
            "-t", "96.157", "-c:v", "libx264", "-preset", "medium", "-crf", "18",
            "-pix_fmt", "yuv420p", "-c:a", "copy", "-map_metadata", "0",
            "-movflags", "+faststart", str(args.output),
        ]
        args.output.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(command, check=True)
    print("SHA-256:", hashlib.sha256(args.output.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
