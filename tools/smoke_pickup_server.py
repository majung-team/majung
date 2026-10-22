"""Exercise the local pickup API using fictional data. Never print bearer tokens."""

import json
import urllib.error
import urllib.request
from datetime import datetime, timezone

BASE_URL = "http://127.0.0.1:8080"


def request(method, path, token=None, body=None, expected=200):
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        headers["Content-Type"] = "application/json"
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        result = json.load(response)
        if response.status != expected:
            raise RuntimeError(f"Expected HTTP {expected}, got {response.status}: {result.get('code', 'unexpected response')}")
        if response.headers.get("Cache-Control") != "no-store":
            raise RuntimeError("Missing no-store header")
    return result


def main():
    pickup = request("POST", "/api/v1/pickups", body={"place": {
        "label": "시연용 만남 장소", "latitude": 37.0, "longitude": 127.0}}, expected=201)
    path = "/api/v1/pickups/" + pickup["pickupId"]
    owner = pickup["driverToken"]
    try:
        print("1/6 픽업 생성: 통과")
        guest = request("POST", path + "/participants", body={
            "inviteToken": pickup["inviteToken"], "displayName": "시연 탑승자"}, expected=201)
        participant = guest["participantToken"]
        request("GET", path + "/vehicle-state", participant, expected=403)
        print("2/6 승인 전 차량 정보 조회 차단: 통과")
        request("POST", path + "/participants/" + guest["participantId"] + "/approval", owner)
        source_time = datetime.now(timezone.utc).isoformat()
        request("PUT", path + "/vehicle-state", owner, {
            "latitude": 37.01, "longitude": 127.01, "durationSeconds": 300,
            "sourceTime": source_time, "sequence": 1})
        vehicle = request("GET", path + "/vehicle-state", participant)
        if vehicle["freshness"] != "FRESH" or vehicle["durationSeconds"] != 300:
            raise RuntimeError("Approved vehicle state was not available")
        print("3/6 승인 후 예시 차량 정보 조회: 통과")
        request("PUT", path + "/readiness", participant, {"readiness": "READY", "sequence": 1})
        state = request("GET", path, owner)
        if state["participant"]["readiness"] != "READY":
            raise RuntimeError("Readiness did not persist")
        print("4/6 준비 완료 저장·운전자 조회: 통과")
        request("POST", path + "/arrival", owner)
        request("GET", path + "/vehicle-state", participant)
        print("5/6 도착 후 공유 유지: 통과")
        request("POST", path + "/completion", owner)
        request("GET", path + "/vehicle-state", participant, expected=410)
        print("6/6 완료 후 차량 정보 조회 차단: 통과")
    finally:
        # Idempotent cancellation is allowed; a completed pickup returns 410 instead.
        try:
            request("POST", path + "/cancellation", owner)
        except (RuntimeError, OSError):
            pass


if __name__ == "__main__":
    main()
