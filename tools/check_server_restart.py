"""Verify file-backed persistence across two real server processes in an isolated temp directory."""

import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request


def main():
    root = Path(__file__).resolve().parents[1]
    jar = root / "server/build/libs/majung-server.jar"
    if not jar.exists():
        raise SystemExit("먼저 ./gradlew -p server bootJar 를 실행해주세요.")
    with socket.socket() as port_socket:
        port_socket.bind(("127.0.0.1", 0))
        port = port_socket.getsockname()[1]
    with tempfile.TemporaryDirectory(prefix="majung-restart-") as directory:
        env = {**os.environ, "PORT": str(port), "SERVER_ADDRESS": "127.0.0.1",
               "DB_URL": "jdbc:h2:file:" + directory + "/pickup;DB_CLOSE_ON_EXIT=FALSE",
               "DB_USER": "sa", "DB_PASSWORD": ""}
        process = None
        with open(Path(directory) / "server.log", "w+") as log:
            def start():
                proc = subprocess.Popen(["java", "-jar", str(jar)], cwd=directory, env=env,
                                        stdout=log, stderr=subprocess.STDOUT)
                for _ in range(120):
                    if proc.poll() is not None:
                        raise RuntimeError("서버 시작 실패: 임시 서버 로그 확인 필요")
                    try:
                        with socket.create_connection(("127.0.0.1", port), timeout=0.2):
                            return proc
                    except OSError:
                        time.sleep(0.25)
                proc.terminate()
                proc.wait(timeout=10)
                raise RuntimeError("서버 시작 시간 초과")

            def call(method, path, token=None, body=None):
                headers = {"Accept": "application/json"}
                if token:
                    headers["Authorization"] = "Bearer " + token
                if body is not None:
                    headers["Content-Type"] = "application/json"
                req = urllib.request.Request("http://127.0.0.1:" + str(port) + path,
                                             data=None if body is None else json.dumps(body).encode(),
                                             headers=headers, method=method)
                with urllib.request.urlopen(req, timeout=10) as response:
                    return json.load(response)

            try:
                process = start()
                created = call("POST", "/api/v1/pickups", body={"place": {
                    "label": "재시작 시험용 장소", "latitude": 37.0, "longitude": 127.0}})
                path = "/api/v1/pickups/" + created["pickupId"]
                guest = call("POST", path + "/participants", body={
                    "inviteToken": created["inviteToken"], "displayName": "시험"})
                call("POST", path + "/participants/" + guest["participantId"] + "/approval", created["driverToken"])
                call("PUT", path + "/readiness", guest["participantToken"], {"readiness": "READY", "sequence": 1})
                process.terminate()
                process.wait(timeout=15)
                process = start()
                after = call("GET", path, created["driverToken"])
                if after["status"] != "ACTIVE" or after["participant"]["readiness"] != "READY":
                    raise RuntimeError("재시작 후 픽업 상태 불일치")
                guest_after = call("GET", path, guest["participantToken"])
                if guest_after["place"] is None:
                    raise RuntimeError("재시작 후 탑승자 승인 상태 불일치")
                call("POST", path + "/cancellation", created["driverToken"])
                try:
                    call("GET", path + "/vehicle-state", guest["participantToken"])
                except urllib.error.HTTPError as error:
                    if error.code != 410:
                        raise
                else:
                    raise RuntimeError("취소 후 위치 조회 차단 실패")
                print("재시작 전후 픽업·준비 상태·운전자/탑승자 권한 유지: 통과")
                print("재시작 후 취소·위치 조회 차단: 통과")
            finally:
                if process is not None and process.poll() is None:
                    process.terminate()
                    process.wait(timeout=15)


if __name__ == "__main__":
    main()
