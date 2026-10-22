package app.majung.vehicle

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.concurrent.Executors

/** One application-owned session. Credentials stay in memory, not saved state or preferences. */
class PickupController(private val api: PickupApiClient = PickupApiClient()) {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var driverToken: String? = null
    private var inviteToken: String? = null
    private var participantId: String? = null
    private var uncertainCreate = false
    var pickupId: String? = null
        private set
    var place: PickupPlace? = null
        private set
    var status = ""
        private set
    var participantName: String? = null
        private set
    var readiness = ""
        private set
    var busy = false
        private set
    var online = false
        private set
    var message = ""
        private set
    var changed: (() -> Unit)? = null

    val canCreate: Boolean get() = !busy && pickupId == null && !uncertainCreate
    val canApprove: Boolean get() = !busy && online && status == "WAITING_APPROVAL" && participantId != null
    val canCancel: Boolean get() = !busy && online && pickupId != null && status !in ended

    fun create(selectedPlace: PickupPlace) {
        if (!canCreate) return
        uncertainCreate = true
        runRequest({ api.create(selectedPlace) }, { result ->
            val id = result.getString("pickupId")
            val owner = result.getString("driverToken")
            val invitation = result.getString("inviteToken")
            pickupId = id
            driverToken = owner
            inviteToken = invitation
            place = selectedPlace
            status = "INVITING"
            message = "픽업을 만들었어요. 초대 링크로 참여한 사람을 확인해주세요."
            uncertainCreate = false
        }, creating = true)
    }

    fun refresh() {
        val id = pickupId ?: return
        val token = driverToken ?: return
        if (busy || status in ended) return
        runRequest({ api.request("GET", "/$id", token) }, ::receiveSnapshot)
    }

    fun approve() {
        if (!canApprove) return
        val path = "/$pickupId/participants/$participantId/approval"
        val token = driverToken ?: return
        runRequest({ api.request("POST", path, token) }, ::receiveSnapshot)
    }

    fun cancel() {
        if (!canCancel) return
        val path = "/$pickupId/cancellation"
        val token = driverToken ?: return
        runRequest({ api.request("POST", path, token) }, ::receiveSnapshot)
    }

    fun prepareNextPickup() {
        if (busy || status !in ended) return
        pickupId = null
        driverToken = null
        inviteToken = null
        participantId = null
        participantName = null
        place = null
        readiness = ""
        status = ""
        message = ""
        online = false
        uncertainCreate = false
        changed?.invoke()
    }

    fun invitationForHostBrowser(): String? {
        val id = pickupId ?: return null
        val token = inviteToken ?: return null
        // Emulator-only workflow. Never infer a public address from this link.
        return if (BuildConfig.DEBUG && status == "INVITING") "http://127.0.0.1:8080/#join/$id/$token" else null
    }

    private fun receiveSnapshot(data: JSONObject) {
        status = data.getString("status")
        val participant = data.optJSONObject("participant")
        participantId = participant?.optString("participantId")
        participantName = participant?.optString("displayName")
        readiness = participant?.optString("readiness") ?: ""
        if (status != "INVITING") inviteToken = null
        if (status in ended) {
            driverToken = null
            inviteToken = null
            participantId = null
            participantName = null
            place = null
            message = "픽업 공유가 종료됐어요. 이 앱의 접근 정보도 지웠어요."
        }
    }

    private fun runRequest(call: () -> JSONObject, accept: (JSONObject) -> Unit, creating: Boolean = false) {
        if (busy) return
        busy = true
        message = ""
        changed?.invoke()
        worker.execute {
            val result = runCatching(call)
            handler.post {
                busy = false
                result.fold(onSuccess = { data ->
                    try {
                        accept(data)
                        online = true
                    } catch (_: Exception) {
                        online = false
                        message = "서버 응답 형식을 확인하지 못했어요."
                    }
                }, onFailure = { error ->
                    online = false
                    if (creating && error is ApiRejected && error.status in 400..499) uncertainCreate = false
                    message = if (creating && uncertainCreate)
                        "생성 결과를 확인하지 못했어요. 중복 생성을 막기 위해 자동 재시도하지 않아요."
                    else "서버와 연결하지 못했어요. 다시 확인해주세요."
                    if (error is ApiRejected && error.status in setOf(401, 403, 404)) {
                        driverToken = null
                        inviteToken = null
                        participantId = null
                        status = "EXPIRED"
                        place = null
                        participantName = null
                        message = "이 픽업에 접근할 수 없어요. 접근 정보를 지웠어요."
                    }
                })
                changed?.invoke()
            }
        }
    }

    companion object { private val ended = setOf("COMPLETED", "CANCELLED", "EXPIRED") }
}
