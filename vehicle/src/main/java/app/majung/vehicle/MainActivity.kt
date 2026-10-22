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
    private lateinit var createButton: Button
    private lateinit var approveButton: Button
    private lateinit var inviteButton: Button
    private lateinit var cancelButton: Button
    private lateinit var nextButton: Button
    private var ticks = 0
    private val tick = object : Runnable {
        override fun run() {
            render()
            if (++ticks % 3 == 0) app.pickup.refresh()
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
        pickupText = label("", 18f)
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
        cancelButton = button(layout, "픽업 취소 · 공유 종료") {
            AlertDialog.Builder(this).setMessage("픽업을 취소하고 위치 공유를 종료할까요?")
                .setPositiveButton("픽업 취소") { _, _ -> app.pickup.cancel() }
                .setNegativeButton("돌아가기", null).show()
        }
        nextButton = button(layout, "새 픽업 준비") {
            app.pickup.prepareNextPickup()
            app.destination.query()
        }
        layout.addView(label("좌표를 입력할 필요가 없어요.\n위치·ETA 자동 전송과 장소 변경은 다음 단계에서 연결해요.\n앱 프로세스가 종료되면 초대·운전자 접근 정보는 복원되지 않아요.", 14f))
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onStart() {
        super.onStart()
        app.pickup.changed = ::render
        handler.post(tick)
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
        destinationText.text = "${app.destination.status}\n\n${place?.label ?: "확인된 목적지가 없어요. 목적지를 다시 확인해주세요."}"
        createButton.isEnabled = place != null && pickup.canCreate
        inviteButton.isEnabled = pickup.invitationForHostBrowser() != null
        approveButton.isEnabled = pickup.canApprove
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
            "INVITE_EXPIRED" -> "초대가 만료됐어요. 픽업 취소 후 새 약속을 준비해주세요."
            "CANCELLED", "COMPLETED", "EXPIRED" -> "위치 공유 종료"
            else -> "아직 만든 픽업이 없어요"
        }
        pickupText.text = listOfNotNull(
            if (pickup.busy) "서버 확인 중…" else if (pickup.pickupId != null && !pickup.online) "서버 연결 확인 필요" else null,
            pickup.place?.let { "이번 픽업 장소: ${it.label}" }, phase,
            pickup.participantName?.let { "$it 님 · $readiness" }, pickup.message.takeIf { it.isNotBlank() }
        ).joinToString("\n\n")
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
