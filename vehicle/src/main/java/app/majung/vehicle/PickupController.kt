package app.majung.vehicle

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
    private var refreshing = false
    private var controlGeneration = 0
    private var refreshError = false
    private var vehicleSequence = 0L
    private var vehicleGeneration = 0
    private var vehicleInFlight = false
    private var vehicleKnownEmpty = true
    private var publishedRevision: Long? = null
    private var nextVehicleSend = 0L
    private var nextInvalidationSend = 0L
    var vehicleMessage = "차량 정보 전송 대기"
        private set
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
            resetVehiclePublisher()
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
        if (busy || refreshing || status in ended) return
        runRequest({ api.request("GET", "/$id", token) }, ::receiveSnapshot, readOnly = true)
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
        controlGeneration++
        resetVehiclePublisher()
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

    fun syncVehicle(reader: DestinationReader) {
        if (status !in setOf("ACTIVE", "ARRIVED")) return
        val id = pickupId ?: return
        val token = driverToken ?: return
        val confirmedPlace = place ?: return
        val now = SystemClock.elapsedRealtime()
        val sample = reader.telemetry.current(confirmedPlace, now)
        if (sample == null) {
            vehicleMessage = "최신 차량 정보가 없어 위치·ETA 공유를 멈췄어요"
            if (vehicleKnownEmpty && !vehicleInFlight) return
            if (now < nextInvalidationSend) return
        } else {
            if (publishedRevision == sample.revision && !vehicleKnownEmpty) return
            if (now < nextVehicleSend) return
        }
        if (vehicleInFlight) return
        vehicleInFlight = true
        val sequence = ++vehicleSequence
        val generation = vehicleGeneration
        if (sample != null) nextVehicleSend = now + 3000 else nextInvalidationSend = now + 3000
        val body = JSONObject().put("sequence", sequence)
        if (sample != null) {
            body.put("latitude", sample.latitude).put("longitude", sample.longitude)
                .put("durationSeconds", sample.durationSeconds).put("sourceTime", sample.observedAt.toString())
        }
        worker.execute {
            val result = runCatching {
                api.request(if (sample == null) "DELETE" else "PUT", "/$id/vehicle-state", token, body)
            }
            handler.post {
                if (generation != vehicleGeneration || id != pickupId || status in ended) return@post
                vehicleInFlight = false
                result.fold(onSuccess = {
                    vehicleKnownEmpty = sample == null
                    publishedRevision = sample?.revision
                    vehicleMessage = if (sample == null) "서버의 이전 위치·ETA를 지웠어요"
                        else "SDK 위치·ETA를 서버에 전달했어요"
                }, onFailure = {
                    // A timed-out request may have committed: invalidate until acknowledged.
                    vehicleKnownEmpty = false
                    publishedRevision = null
                    vehicleMessage = "차량 정보 전송을 확인하지 못했어요. 이전 정보는 서버에서 만료돼요."
                })
                changed?.invoke()
                // Do not wait for the next poll if the SDK was invalidated during an upload.
                if (sample != null && reader.telemetry.current(confirmedPlace, SystemClock.elapsedRealtime()) == null) {
                    syncVehicle(reader)
                }
            }
        }
    }

    private fun resetVehiclePublisher() {
        vehicleGeneration++
        vehicleSequence = 0
        vehicleInFlight = false
        vehicleKnownEmpty = true
        publishedRevision = null
        nextVehicleSend = 0
        nextInvalidationSend = 0
        vehicleMessage = "차량 정보 전송 대기"
    }

    private fun receiveSnapshot(data: JSONObject) {
        status = data.getString("status")
        val participant = data.optJSONObject("participant")
        participantId = participant?.optString("participantId")
        participantName = participant?.optString("displayName")
        readiness = participant?.optString("readiness") ?: ""
        if (status != "INVITING") inviteToken = null
        if (status in ended) {
            resetVehiclePublisher()
            driverToken = null
            inviteToken = null
            participantId = null
            participantName = null
            place = null
            message = "픽업 공유가 종료됐어요. 이 앱의 접근 정보도 지웠어요."
        }
    }

    private fun runRequest(call: () -> JSONObject, accept: (JSONObject) -> Unit,
                           creating: Boolean = false, readOnly: Boolean = false) {
        if (busy || (readOnly && refreshing)) return
        val generation = if (readOnly) controlGeneration else ++controlGeneration
        if (readOnly) refreshing = true else {
            busy = true
            refreshError = false
            message = ""
            changed?.invoke()
        }
        worker.execute {
            val result = runCatching(call)
            handler.post {
                if (readOnly) refreshing = false else busy = false
                if (readOnly && (generation != controlGeneration || busy)) return@post
                result.fold(onSuccess = { data ->
                    try {
                        if (readOnly && refreshError) { message = ""; refreshError = false }
                        accept(data)
                        online = true
                    } catch (_: Exception) {
                        online = false
                        message = "서버 응답 형식을 확인하지 못했어요."
                        refreshError = readOnly
                    }
                }, onFailure = { error ->
                    refreshError = readOnly
                    online = false
                    if (creating && error is ApiRejected && error.status in 400..499) uncertainCreate = false
                    message = if (creating && uncertainCreate)
                        "생성 결과를 확인하지 못했어요. 중복 생성을 막기 위해 자동 재시도하지 않아요."
                    else "서버와 연결하지 못했어요. 다시 확인해주세요."
                    if (error is ApiRejected && error.status in setOf(401, 403, 404)) {
                        resetVehiclePublisher()
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
