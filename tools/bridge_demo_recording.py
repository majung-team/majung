#!/usr/bin/env python3
"""Local file handoff for opt-in VehicleDemoRecordingTest; never print invitations."""
import argparse
import json
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument('--adb', required=True)
parser.add_argument('--serial', required=True, help='Isolated recording emulator; never select the user device implicitly')
parser.add_argument('--user', default='10')
parser.add_argument('--output', default='evidence/private/demo-recording')
args = parser.parse_args()
out = Path(args.output)
out.mkdir(parents=True, exist_ok=True, mode=0o700)
out.chmod(0o700)
for name in ['phase.txt', 'invitation.json', 'stop', 'create.go', 'approve.go', 'arrive.go', 'complete.go', 'finish.go']:
    (out / name).unlink(missing_ok=True)

def device(*command):
    return subprocess.run([args.adb, '-s', args.serial, 'shell', 'run-as', 'app.majung.vehicle', '--user', args.user, *command],
                          capture_output=True, text=True, timeout=5)

deadline = time.monotonic() + 480
try:
    while time.monotonic() < deadline and not (out / 'stop').exists():
        phase = device('cat', 'files/demo-recording/phase.txt')
        value = phase.stdout.strip()
        if phase.returncode == 0 and value in {'ready', 'inviting', 'waiting-approval', 'active', 'sdk-visible', 'passenger-ready', 'arrived', 'completed'}:
            (out / 'phase.txt').write_text(value)
            if value == 'inviting' and not (out / 'invitation.json').exists():
                invitation = device('cat', 'files/demo-recording/invitation.json')
                if invitation.returncode == 0:
                    data = json.loads(invitation.stdout)
                    if isinstance(data.get('inviteLink'), str):
                        (out / 'invitation.json').write_text(invitation.stdout)
                        (out / 'invitation.json').chmod(0o600)
        for name in ['create', 'approve', 'arrive', 'complete', 'finish']:
            trigger = out / (name + '.go')
            if trigger.exists():
                result = device('touch', 'files/demo-recording/' + name + '.go')
                if result.returncode == 0:
                    trigger.unlink()
        time.sleep(.2)
finally:
    (out / 'invitation.json').unlink(missing_ok=True)
