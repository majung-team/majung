package app.majung.probe

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import org.json.JSONObject
import java.io.File
import java.time.Instant

/** A local-only probe. It records real callbacks; it does not generate telemetry. */
class NaviProbe(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null
    private var generation: Int = 0
    private var connected: Boolean = false
    private val events: ArrayDeque<String> = ArrayDeque()
    private val lastCallback: MutableMap<String, Long> = mutableMapOf()
    private val counts: MutableMap<String, Int> = mutableMapOf()
    private val file: File = File(context.filesDir, "navi-events.jsonl")

    var connection: String = "연결 전"
        private set
    var location: String = "위치: 아직 수신하지 않음"
        private set
    var driving: String = "예상 시간: 아직 수신하지 않음"
        private set
    var route: String = "경로: 아직 수신하지 않음"
        private set
    var lastReceived: String = "마지막 수신: 없음"
        private set
    var locationRevision: Int = 0
        private set

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            record("location", currentLocationInfo.toString()) {
                locationRevision++
                location = "위도 ${currentLocationInfo.latitude}\n경도 ${currentLocationInfo.longitude}\n방향 ${currentLocationInfo.heading}"
            }
        }

        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            record("driving", drivingInfo.toString()) {
                driving = "목적지: ${drivingInfo.destination.name}\n예상 ${drivingInfo.destination.duration}초\n" +
                    drivingInfo.waypoints.mapIndexed { index, waypoint ->
                        "경유지 ${index + 1}: ${waypoint.name} / ${waypoint.duration}초"
                    }.joinToString("\n")
            }
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            record("destination", destinationInfo.toString())
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            record("route_state", routeStateInfo.toString()) { route = routeStateInfo.toString() }
        }

        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            record("route_started", routeStartInfo.toString()) { driving = "경로 탐색 중: 이전 예상 시간 사용 중지" }
        }

        override fun onRoutingCompleted() {
            record("routing_completed", "경로 탐색 완료")
        }

        override fun onReRouteEnded() {
            record("reroute_ended", "재탐색 완료")
        }

        override fun onRouteCancelled() {
            record("route_cancelled", "경로 취소") { driving = "경로 취소: 예상 시간 무효" }
        }

        override fun onDestinationChanged() {
            record("destination_changed", "목적지 변경 또는 제거") {
                driving = "목적지 변경: 새 예상 시간 대기"
            }
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            record("destination_arrived", destinationArrivedInfo.toString())
        }

        override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) {
            record("waypoint_arrived", waypointArrivedInfo.toString())
        }

        override fun onError(errorCode: NaviErrorCode) {
            record("navi_error", errorCode.toString())
        }
    }

    fun connect() {
        if (helper != null) return
        val currentGeneration: Int = ++generation
        connection = "연결 중"
        record("connect_requested", "NaviHelper 3.0.5")
        try {
            val instance = NaviHelper(context.applicationContext)
            helper = instance
            instance.initialize(
                onConnected = {
                    handler.post {
                        if (generation == currentGeneration && helper === instance) {
                            connected = true
                            connection = "연결 성공"
                            instance.addListener(listener)
                            record("connected", "리스너 등록")
                            query()
                        }
                    }
                },
                onFailed = { error ->
                    handler.post {
                        if (generation == currentGeneration) {
                            connection = "연결 실패: ${error.message}"
                            record("connection_failed", error.toString())
                            disconnect()
                        }
                    }
                }
            )
        } catch (error: Exception) {
            record("initialization_exception", error.toString())
            disconnect()
        }
    }

    fun query() {
        val instance: NaviHelper = helper ?: return
        if (!connected) return
        try {
            record("query_requested", "현재 위치·목적지·경로 상태 요청; 자동 반복 조회 없음")
            instance.getCurrentLocationInfo()
            instance.getDestinationInfo()
            instance.getRouteStateInfo()
        } catch (error: Exception) {
            record("query_exception", error.toString())
        }
    }

    fun requestExampleRoute(emptyPoiId: Boolean = false) {
        val instance: NaviHelper = helper ?: return
        if (!connected) return
        record("route_request", "공식 문서의 42dot 좌표; poiId=${if (emptyPoiId) "empty" else "official"}")
        try {
            instance.requestRoute(
                RouteInfo(
                    latitude = 37.48544722,
                    longitude = 127.03636666,
                    poiName = "42dot",
                    poiId = if (emptyPoiId) "" else "36khp37hh8gqcm5nqhe5",
                    address = "서울시 강남구 남부순환로 2621",
                    poiSubId = "0"
                )
            )
        } catch (error: Exception) {
            record("route_exception", error.toString())
        }
    }

    fun disconnect() {
        generation++
        val instance: NaviHelper? = helper
        helper = null
        connected = false
        try {
            instance?.removeListener(listener)
            instance?.release()
        } catch (error: Exception) {
            record("release_exception", error.toString())
        }
        connection = "연결 해제"
        location = "위치: 연결 해제, 이전 값 사용 중지"
        driving = "예상 시간: 연결 해제, 이전 값 사용 중지"
        route = "경로: 연결 해제"
        record("disconnected", "자동 재연결 없음")
    }

    fun lifecycle(name: String) = record("lifecycle", name)

    fun summary(): String = counts.entries.joinToString(" · ") { "${it.key}=${it.value}" }
    fun eventText(): String = events.reversed().joinToString("\n\n")

    private fun record(type: String, value: String, update: () -> Unit = {}) {
        handler.post {
            update()
            val now: Long = SystemClock.elapsedRealtime()
            val previous: Long? = lastCallback.put(type, now)
            val timestamp: String = Instant.now().toString()
            counts[type] = (counts[type] ?: 0) + 1
            if (type in setOf("location", "driving", "destination", "route_state",
                    "route_started", "routing_completed", "reroute_ended", "route_cancelled",
                    "destination_changed", "destination_arrived", "waypoint_arrived", "navi_error")) {
                lastReceived = "$timestamp / $type"
            }
            val json = JSONObject()
                .put("time", timestamp)
                .put("elapsedMs", now)
                .put("type", type)
                .put("gapMs", previous?.let { now - it } ?: JSONObject.NULL)
                .put("value", value)
            val line: String = json.toString()
            events.addLast(line)
            while (events.size > 80) events.removeFirst()
            if (file.length() > 2_000_000) {
                file.copyTo(File(context.filesDir, "navi-events.previous.jsonl"), overwrite = true)
                file.writeText("")
            }
            file.appendText(line + "\n")
            Log.i("MajungProbe", line)
        }
    }
}
