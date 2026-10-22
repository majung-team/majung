package app.majung.vehicle

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/** All mutable state is confined to the main thread. No raw SDK data is logged. */
class DestinationReader(private val context: Context) {
    val selection = DestinationSelection()
    private val handler = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null
    private var listener: NaviHelperEventListener? = null
    private var generation = 0
    private var connected = false
    private var routing = false
    var status = "내비게이션 연결 전"
        private set

    fun connect() {
        if (helper != null) return
        val version = ++generation
        status = "내비게이션 연결 중"
        val instance = try { NaviHelper(context.applicationContext) }
        catch (_: Exception) { status = "내비게이션 연결 실패"; return }
        helper = instance
        val callbacks = object : NaviHelperEventListener {
            private fun update(action: () -> Unit) {
                handler.post { if (generation == version && connected) action() }
            }
            override fun onDestinationInfo(destinationInfo: DestinationInfo) = update {
                if (routing) return@update
                selection.receive(destinationInfo.name, destinationInfo.latitude,
                    destinationInfo.longitude, SystemClock.elapsedRealtime())
                status = if (selection.current(SystemClock.elapsedRealtime()) == null)
                    "내비게이션에서 목적지를 선택해주세요" else "내비게이션 목적지를 확인했어요"
            }
            override fun onRouteStarted(routeStartInfo: RouteStartInfo) = update {
                routing = true
                selection.invalidate()
                status = "경로 탐색 중 · 완료 후 목적지를 다시 확인해주세요"
            }
            override fun onDestinationChanged() = update {
                routing = true
                selection.invalidate()
                status = "목적지가 바뀌었어요 · 다시 확인해주세요"
            }
            override fun onRoutingCompleted() = update {
                routing = false
                query()
            }
            override fun onReRouteEnded() = update {
                routing = false
                query()
            }
            override fun onRouteCancelled() = update {
                routing = false
                selection.invalidate()
                status = "경로가 취소됐어요"
            }
            override fun onError(errorCode: NaviErrorCode) = update {
                selection.invalidate()
                status = "내비게이션 정보를 확인하지 못했어요"
            }
        }
        listener = callbacks
        try {
            instance.initialize(onConnected = {
                handler.post {
                    if (generation == version) {
                        connected = true
                        instance.addListener(callbacks)
                        query()
                    }
                }
            }, onFailed = {
                handler.post { if (generation == version) disconnect() }
            })
        } catch (_: Exception) { disconnect() }
    }

    fun query() {
        selection.invalidate()
        if (!connected || routing) return
        status = "선택한 목적지 확인 중"
        try { helper?.getDestinationInfo() }
        catch (_: Exception) {
            status = "목적지 조회 실패 · 다시 확인해주세요"
        }
    }

    fun disconnect() {
        generation++
        connected = false
        routing = false
        selection.invalidate()
        val instance = helper
        helper = null
        try {
            listener?.let { instance?.removeListener(it) }
            instance?.release()
        } catch (_: Exception) { /* No raw SDK error logging. */ }
        listener = null
        status = "내비게이션 연결 해제 · 이전 장소 사용 중지"
    }
}
