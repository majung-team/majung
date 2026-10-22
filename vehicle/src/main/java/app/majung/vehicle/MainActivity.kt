package app.majung.vehicle

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Parked development screen, not an approved driving UI. No coordinate entry. */
class MainActivity : Activity() {
    private val app: VehicleApplication get() = application as VehicleApplication
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var destinationText: TextView
    private lateinit var pickupText: TextView
    private lateinit var connectionText: TextView
    private lateinit var createButton: Button
    private lateinit var approveButton: Button
    private lateinit var inviteButton: Button
    private lateinit var arriveButton: Button
    private lateinit var completeButton: Button
    private lateinit var cancelButton: Button
    private lateinit var nextButton: Button
    private var ticks = 0
    private val tick = object : Runnable {
        override fun run() {
            render()
            if (++ticks % 3 == 0) app.pickup.refresh()
            if (app.pickup.status in setOf("ACTIVE", "ARRIVED")) {
                if (ticks % 5 == 0) app.destination.pollTelemetry()
                app.pickup.syncVehicle(app.destination)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(Color.rgb(246, 247, 239))
        }
        layout.addView(label("마중 · 차량에서 약속 만들기", 26f))
        layout.addView(label("주차 중에 사용하는 개발 확인 화면\n내비게이션에서 목적지를 고른 뒤 확인해주세요.\n버튼을 누르면 선택한 장소가 로컬 개발 서버에 저장돼요.", 16f))
        button(layout, "내비게이션 연결") { app.destination.connect() }
        button(layout, "선택한 목적지 다시 확인") { app.destination.query(); render() }
        destinationText = label("", 20f)
        layout.addView(destinationText)
        createButton = button(layout, "이 장소로 픽업 만들기") {
            val selected = app.destination.selection.current(SystemClock.elapsedRealtime()) ?: return@button
            app.pickup.create(selected)
        }
        connectionText = label("", 14f).apply {
            id = R.id.connection_status
            setLines(1)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        layout.addView(connectionText)
        pickupText = label("", 18f).apply { id = R.id.pickup_details }
        layout.addView(pickupText)
        inviteButton = button(layout, "개발용 초대 링크 복사") {
            app.pickup.invitationForHostBrowser()?.let { link ->
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("마중 개발용 초대", link))
                AlertDialog.Builder(this).setMessage("이 Mac의 브라우저에서 붙여넣어주세요. 실제 휴대전화용 주소는 아직 없어요.")
                    .setPositiveButton("확인", null).show()
            }
        }
        approveButton = button(layout, "참여자 확인하고 공유 시작") {
            AlertDialog.Builder(this).setMessage("초대한 사람이 맞나요? 확인하면 이번 픽업의 장소를 공유해요.")
                .setPositiveButton("확인하고 시작") { _, _ -> app.pickup.approve() }
                .setNegativeButton("돌아가기", null).show()
        }
        button(layout, "서버 상태 다시 확인") { app.pickup.refresh() }
        arriveButton = button(layout, "도착 알리기 · 공유 유지") {
            if (!app.pickup.canArrive) return@button
            AlertDialog.Builder(this).setMessage("약속 장소에 도착했나요? 탑승을 확인하기 전까지 위치 공유는 유지돼요.")
                .setPositiveButton("도착 알리기") { _, _ -> app.pickup.arrive() }
                .setNegativeButton("돌아가기", null).show()
        }.apply { id = R.id.arrive_pickup }
        completeButton = button(layout, "탑승 확인 · 픽업 완료") {
            if (!app.pickup.canComplete) return@button
            AlertDialog.Builder(this).setMessage("탑승자가 차에 탔나요? 픽업을 완료하면 위치 공유가 종료돼요.")
                .setPositiveButton("탑승 확인하고 완료") { _, _ -> app.pickup.complete() }
                .setNegativeButton("돌아가기", null).show()
        }.apply { id = R.id.complete_pickup }
        cancelButton = button(layout, "픽업 취소 · 공유 종료") {
            AlertDialog.Builder(this).setMessage("픽업을 취소하고 위치 공유를 종료할까요?")
                .setPositiveButton("픽업 취소") { _, _ -> app.pickup.cancel() }
                .setNegativeButton("돌아가기", null).show()
        }
        cancelButton.id = R.id.cancel_pickup
        nextButton = button(layout, "새 픽업 준비") {
            app.pickup.prepareNextPickup()
            app.destination.query()
        }
        layout.addView(label("좌표를 입력할 필요가 없어요.\n승인 후 SDK 위치·남은 시간을 서버로 전달해요.\n화면을 떠나면 공유를 멈춰요. 장소 변경은 아직 지원하지 않아요.\n앱 프로세스가 종료되면 초대·운전자 접근 정보는 복원되지 않아요.", 14f))
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onStart() {
        super.onStart()
        app.pickup.changed = ::render
        render()
        handler.postDelayed(tick, 1000)
    }

    override fun onStop() {
        handler.removeCallbacks(tick)
        app.pickup.changed = null
        // Destination freshness must be re-established after returning to the screen.
        app.destination.disconnect()
        super.onStop()
    }

    private fun render() {
        val place = app.destination.selection.current(SystemClock.elapsedRealtime())
        val pickup = app.pickup
        destinationText.updateText("${app.destination.status}\n\n${place?.label ?: "확인된 목적지가 없어요. 목적지를 다시 확인해주세요."}")
        createButton.isEnabled = place != null && pickup.canCreate
        inviteButton.isEnabled = pickup.invitationForHostBrowser() != null
        approveButton.isEnabled = pickup.canApprove
        arriveButton.isEnabled = pickup.canArrive
        completeButton.isEnabled = pickup.canComplete
        cancelButton.isEnabled = pickup.canCancel
        nextButton.visibility = if (pickup.status in setOf("CANCELLED", "COMPLETED", "EXPIRED")) android.view.View.VISIBLE else android.view.View.GONE
        val readiness = when (pickup.readiness) {
            "READY" -> "준비 완료"
            "PREPARING" -> "출발 준비 중"
            else -> "아직 준비 상태를 바꾸지 않았어요"
        }
        val phase = when (pickup.status) {
            "INVITING" -> "탑승자 참여를 기다리고 있어요"
            "WAITING_APPROVAL" -> "참여 요청을 확인해주세요"
            "ACTIVE" -> "픽업 진행 중"
            "ARRIVED" -> "약속 장소에 도착했어요. 탑승 확인 전까지 위치 공유를 유지해요."
            "INVITE_EXPIRED" -> "초대가 만료됐어요. 픽업 취소 후 새 약속을 준비해주세요."
            "COMPLETED" -> "탑승 확인 완료 · 위치 공유 종료"
            "CANCELLED", "EXPIRED" -> "위치 공유 종료"
            else -> "아직 만든 픽업이 없어요"
        }
        connectionText.updateText(when {
            pickup.busy -> "요청 처리 중…"
            pickup.pickupId == null -> "픽업을 만들면 서버와 연결해요"
            pickup.online -> "서버 연결됨"
            else -> "서버 연결 확인 필요"
        })
        pickupText.updateText(listOfNotNull(
            pickup.place?.let { "이번 픽업 장소: ${it.label}" }, phase,
            if (pickup.status in setOf("ACTIVE", "ARRIVED")) pickup.vehicleMessage else null,
            pickup.participantName?.let { "$it 님 · $readiness" }, pickup.message.takeIf { it.isNotBlank() }
        ).joinToString("\n\n"))
    }

    private fun TextView.updateText(value: String) {
        if (text.toString() != value) text = value
    }

    private fun label(value: String, size: Float): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(Color.rgb(25, 59, 52))
        setPadding(0, 12, 0, 12)
    }

    private fun button(parent: LinearLayout, title: String, action: () -> Unit): Button = Button(this).apply {
        text = title
        setOnClickListener { action(); render() }
        parent.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60.dp))
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
