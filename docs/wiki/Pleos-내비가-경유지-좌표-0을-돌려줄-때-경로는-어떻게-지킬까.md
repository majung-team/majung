**작업 영역:** 차량 앱 · 내비게이션 연동 · **기술 주제:** 경유지 좌표 누락 방어·경로 불일치 복구 · **작성자:** [SeungJunS0ng](https://github.com/SeungJunS0ng) · **기준일:** 2026년 11월 12일(KST)

[마중 Wiki 홈](https://github.com/majung-team/majung/wiki) → 차량 앱 · 내비게이션 → 경유지 좌표 0 반환과 경로 복구

마중은 별도의 서버 길안내 엔진을 두지 않고 차량의 Pleos 내비게이션 SDK(`NaviHelper`)가 알려주는 목적지·경유지·위치·예상 도착 시간(ETA)을 그대로 활용한다. 운전자가 가는 길에 탑승자를 경유지로 태워 가거나(`addWaypoint`), 안내 중인 경로를 기준으로 픽업을 만들 때 내비 SDK의 응답이 정확하다는 전제가 깔려 있었다.

하지만 공식 Pleos Connect Emulator 2.1.0 ARM64(`NaviHelper 3.0.5`, 내비게이션 앱 `3.3.2-rc5-hotfix.3`) 환경에서 경유지를 추가하고 경로를 다시 읽었을 때 예상과 다른 응답이 돌아왔다. 최종 목적지는 정상 좌표를 반환했지만, 경유지는 위도와 경도가 모두 `0.0`으로 돌아왔다.

---

## 왜 경유지만 위도·경도가 0으로 왔을까?

처음에는 마중 요청 코드에 좌표가 빠진 줄 알았다. 그러나 정상 위도·경도를 채워 `addWaypoint()`를 호출해도, 최종 목적지와 경유지를 함께 전달하는 `requestRouteWithWaypoints()`를 호출해도 결과는 같았다. `getWaypointInfo()`와 `onWayPointInfo` 콜백 모두 경유지의 원본 `latitude`와 `longitude`를 `0`으로 돌려줬다.

흥미로웠던 점은 나머지 필드였다.

- 요청할 때 넘긴 `poiId`는 응답에 그대로 남아 있었다.
- 경유지의 `remainTime`은 최종 목적지의 `remainTime`과 다른 값, 즉 실제 경유지 기준 ETA로 계산돼 들어왔다.
- 공식 시스템 이미지의 응답 경로를 읽기 전용으로 확인했을 때, 최종 목적지는 좌표가 0이면 요청 좌표로 보정하는 동작이 있었지만 경유지에는 같은 보정이 적용되지 않고 엔진 좌표를 그대로 넘기고 있었다.

단순히 "좌표가 0이니 실패"로 처리하면 경유지 픽업 기능이 아예 동작하지 않았다. 반대로 "(0, 0) 좌표를 그대로 서버에 올리거나 무시"하면 지도와 도착 감지가 망가진다. 게다가 마중 사용자가 고른 지점은 내비 업체 ID(`poiId`)가 없는 좌표 기반 장소일 수도 있었기 때문에, 외부 ID가 있을 때만 허용하던 초기 방식으로는 기능을 마칠 수 없었다.

---

## 이름이나 순번으로 맞추지 않은 이유

0 좌표를 메우는 가장 쉬운 유혹은 "경유지 배열 첫 번째 원소니 방금 추가한 장소겠지" 혹은 "이름(`poiName`)이 같으니 같은 장소겠지"라고 추정하는 것이다. 그렇게 구현하지 않은 이유는 두 가지다.

1. 운전자가 내비 앱 자체 UI에서 다른 경유지를 넣거나 순서를 바꿀 수 있다. 배열 순번만으로 좌표를 덮어쓰면 엉뚱한 지점이 탑승자 지도에 찍힌다.
2. '스타벅스 강남점'처럼 이름만 같고 위치가 다른 경유지나, ID 없는 사용자 지정 지점이 겹칠 수 있다.

그래서 `SdkPickupPlace`와 `WaypointCoordinates`에서는 식별자 1:1 대조 외의 추론을 모두 금지했다.

1. 외부 업체 `poiId`가 있으면 그대로 사용한다.
2. `poiId`가 비어 있으면 `SdkPickupPlace.withRequestIdentity()`가 `majung:` 접두사의 무작위 UUID를 만들고 `poiProvider`를 `"majung"`으로 명시한다. 기존 내비 업체의 ID 체계를 임의로 흉내 내지 않기 위해서다.
3. SDK가 응답한 `waypoints` 목록과 알고 있는 장소 목록(`knownSdkPlaces` 및 `WaypointRecentRecovery`)을 `WaypointCoordinates.resolveKnown()`으로 대조한다.
   - `poiId`가 비어 있지 않고
   - 전체 경유지 목록 안에서도 그 `poiId`가 유일(`count == 1`)하며
   - 알고 있는 장소 목록에서도 정확히 1건(`singleOrNull()`)으로 매칭될 때만 `0.0, 0.0` 좌표를 요청 좌표로 보정한다.
4. 응답에서 `poiId`가 빠졌거나 중복되거나 불일치하면, 생성·공유를 즉시 차단하고 `NavigationFailure.WAYPOINT_COORDINATES_UNAVAILABLE`로 전환한다.
5. 정상적인 비-0 좌표가 돌아오면 보정 로직을 타지 않고 SDK 원본 좌표를 그대로 쓴다. 나중에 SDK가 수정돼 정상 좌표를 주더라도 별도 수정 없이 그대로 통과하게 두기 위해서다.

| 경유지 상황 | 좌표 대조 · 복구 규칙 | 실패 시 동작 |
| --- | --- | --- |
| **정상 비-0 좌표 응답** | SDK 원본 위·경도 그대로 사용 | 보정 없이 통과 |
| **마중이 추가한 경유지 `(0, 0)`** | `poiId` 유일성 + 1:1 매칭 시 요청 좌표 보정 | `WAYPOINT_COORDINATES_UNAVAILABLE` |
| **기존 경유지 `(0, 0)`** | `getRecentDestinationInfo()`(8초) 1건 일치 복원 | 운전자가 **위치 다시 선택** 재지정 |

이 보정은 "입력에 ID가 없어서 마중이 발급한 경우"에만 동작한다. 이미 내비에 들어있던 기존 경유지(마중이 추가한 적이 없는 경유지)가 좌표 0을 돌려줄 때는 어떻게 할지가 다음 문제였다.

---

## 마중이 추가하지 않은 기존 경유지는 어떻게 복원했을까?

기존 경유지 문제는 10월 31일에 `WaypointRecentRecovery`를 추가해 분리 해결했다. 좌표가 0인 기존 경유지의 `poiId`가 비어 있지 않고 경로 내에서 유일하다면, SDK의 최근 장소 목록(`getRecentDestinationInfo()`)을 한 번(8초 제한) 조회해 `poiId`·`poiProvider`·`poiSubId`가 정확히 1건 일치하는 정상 좌표를 찾는다.

최근 장소 목록에서도 못 찾으면 임의로 복구하지 않는다. 대신 **경유지에서 만나기** 흐름에서 운전자가 최근 장소 목록에서 직접 지점을 다시 고르고, 경유지 전체 순서와 유지할 최종 목적지를 눈으로 확인한 뒤 경로를 다시 맞추게 했다.

> [!NOTE]
> 이 현상은 앱 코드 안에서만 땜질하고 끝내지 않았다. 2026년 10월 29일 공모전 공식 문의처(`appdevchallenge@hyundai-partners.com`)에 재현 조건(`Pleos Connect Emulator 2.1.0 ARM64`, `NaviHelper 3.0.5`, 내비 `3.3.2-rc5-hotfix.3`), `addWaypoint()` / `requestRouteWithWaypoints()` 재현 절차, `WaypointResponseRegressionTest`(76.231초 OK) 결과를 정리해 분리 보고했다([원본 확인 요청 자료](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/waypoint-provider-report.md)). 앱 측 방어 구현과 SDK 제공 측 수정 추적을 같은 완료로 섞지 않기 위해서다.

---

## 경로 불일치·무한 루프는 어떻게 정리했을까?

경유지 좌표 문제를 막고 나자 두 가지 실사용 문제가 추가로 드러났다.

### 1. 내비 경로와 요청 장소가 어긋났을 때 복구 (`#113`, `2d340c0`)

경유지 추가를 요청한 직후, 운전자가 내비게이션 화면에서 경로를 수정하거나 내비가 기존 안내를 종료해 버리는 경우가 있었다. 이전에는 `RequestPickupFlow`와 `PickupCreationSection`이 경로 불일치(`PICKUP_NOT_FOUND`, `ROUTE_CHANGED`, `WAYPOINT_COORDINATES_UNAVAILABLE`, `WAYPOINT_CONFIRMATION_TIMEOUT`)를 만나면 거기서 막혀 다시 진행하기 어려웠다.

11월 5일 PR [#113](https://github.com/majung-team/majung-vehicle-app/commit/2d340c08e59ee67f51a0016f828d8cada76c0c83)에서 흐름을 두 갈래로 정리했다.

- **대기 요청(`QUEUED`) 시작 시**: 이미 내비에 안내 중인 경로가 있으면 `addWaypoint()`로 경유지를 추가하고, `confirmedNoGuidance` 상태(안내 중인 경로가 없는 상태)면 `navigateToPickup()`으로 최종 목적지를 직접 설정한다.
- **경로 불일치 발생 시**: `NavigationFailure.requiresPickupReselection`에 해당하는 오류면 **최종 목적지에서 만나기**(`useFinalDestination()`) 버튼으로 픽업 장소 후보만 내비 최종 목적지로 재확인해 막힌 흐름을 풀 수 있게 했다. 이때도 `RouteValidation`이 응답-대-응답 확인 간격(`CONFIRMATION_GAP_MS`)과 타임아웃을 체크해 미완료 콜백이 시간 경과만으로 통과하지 않도록 막는다.

### 2. '내비게이션 다시 연결' 반복 터치와 스피너 멈춤 (`f39e51b`, `2e1060d`)

또 하나는 내비 연결이 끊겼거나 경로가 없을 때 UX였다. 사용자가 `[내비게이션 다시 연결]`을 연타하거나 확인이 끝나지 않은 상태에서 버튼이 열려 있으면 재시도 요청이 겹치고 진행 표시줄이 계속 돌아가는 현상이 있었다.

`f39e51b`와 `2e1060d`에서는 `PickupCreationSection`에 `isRetrying` 가드를 추가해 확인 중 중복 클릭을 막고, 연결 타임아웃·미연결·경로 미확인 상태에서는 `[내비게이션 열기]`를 분명하게 노출하되 재확인 중에는 재터치를 차단했다. 또한 `DestinationReader.kt`의 `connectionTimeout`이 만료되면 이전 `NaviHelper` 인스턴스를 해제하고 `generation` 세대 번호를 올려 뒤늦게 도착한 이전 콜백이 현재 상태를 오염시키지 않도록 막았다(`NavigationConnectionTimeoutTest`).

---

## 확인한 것과 확인하지 못한 것

| 검증 · 시험 대상 | 실행 환경 · 기준 | 확인 결과 |
| --- | --- | --- |
| **`WaypointResponseRegressionTest`** | 별도 Pleos 에뮬레이터 (2026-10-29) | `(0, 0)` 환경 `poiId` 1건 매칭·ETA 분리·재연결 (`76.231초 OK`) |
| **`existing-waypoint-recovery`** | 단위 60 + SDK·Android 6건 (10-31) | ID 없는 경유지 재선택·2곳 순서·최종 목적지 보존 (`156.079초`) |
| **`RouteValidation` 외 3종 · 빌드** | 로컬 단위 시험 · Lint · APK (PR `#113` 이후) | 확인 간격·세대 번호 타임아웃·재선택 복구 단위 검증 통과 |

**확인하지 못한 점**

- 실제 양산 차량이나 다른 내비게이션 앱 버전에서의 `onWayPointInfo` 좌표 반환 여부와 공식 문의에 대한 SDK 측 패치 일정.
- 차량 앱 CI는 빌드·단위 시험·Lint만 돌며 Pleos 에뮬레이터 SDK 시험은 포함하지 않는다. 실제 에뮬레이터 회귀는 로컬 AVD(`Majung_Pleos_2_1`)에서 수동 실행했다.
- 내비 자체에서 일부 적용된 경유지 상태까지 마중이 자동으로 원상 복구해 주는 것은 아니므로, 경로 변경 실패 시 운전자가 내비 경로를 한 번 더 확인해야 한다.

---

## 관련 커밋·문서

- [WaypointCoordinates / SdkPickupPlace (`SdkPickupPlace.kt`)](https://github.com/majung-team/majung-vehicle-app/blob/main/vehicle/src/main/java/app/majung/vehicle/navi/SdkPickupPlace.kt)
- [경유지 좌표 누락 처리 기록 (`docs/waypoint-coordinates.md`)](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/waypoint-coordinates.md) · [제공 측 확인 요청 자료 (`docs/waypoint-provider-report.md`)](https://github.com/majung-team/majung-vehicle-app/blob/main/docs/waypoint-provider-report.md)
- [PR #113: 내비 경로 불일치 시 최종 목적지 재선택 복구 (`2d340c0`)](https://github.com/majung-team/majung-vehicle-app/commit/2d340c08e59ee67f51a0016f828d8cada76c0c83)
- [내비 연결 루프 방지 및 UX 보완 (`f39e51b`)](https://github.com/majung-team/majung-vehicle-app/commit/f39e51b86ba8ed58d7db18c713bab87a2bd423f3) · [`2e1060d`](https://github.com/majung-team/majung-vehicle-app/commit/2e1060db70e1412147b2041b8f8e7f8f309ac2db)
