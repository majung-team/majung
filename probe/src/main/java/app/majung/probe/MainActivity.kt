package app.majung.probe

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var probe: NaviProbe
    private lateinit var status: TextView
    private lateinit var events: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            status.text = listOf(
                probe.connection, probe.location, probe.driving, probe.route,
                probe.lastReceived, probe.summary()
            ).joinToString("\n\n")
            events.text = probe.eventText()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        probe = (application as ProbeApplication).probe
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        layout.addView(label("마중 · NaviHelper 확인", 25f))
        layout.addView(label("SDK 3.0.5 · 실제 콜백만 기록\n주차 상태에서 사용하는 개발 확인용 앱입니다.\n로그는 앱 내부에 저장하며 외부 서버로 전송하지 않습니다.", 16f))
        button(layout, "SDK 연결") { probe.connect() }
        button(layout, "현재 위치·목적지·경로 조회") { probe.query() }
        button(layout, "연결 해제") { probe.disconnect() }
        if (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk")) {
            button(layout, "에뮬레이터: 공식 예제 목적지 요청") { probe.requestExampleRoute() }
        }
        status = label("연결 전", 18f)
        events = label("이벤트 기록 없음", 13f).apply { setTextIsSelectable(true) }
        layout.addView(status)
        layout.addView(label("수신 기록 · 최신순", 20f))
        layout.addView(events)
        setContentView(ScrollView(this).apply { addView(layout) })
        probe.lifecycle("activity_created")
    }

    override fun onStart() {
        super.onStart()
        probe.lifecycle("activity_started")
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        probe.lifecycle("activity_stopped; SDK listener retained for background observation")
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        probe.lifecycle("activity_destroyed; Application owns SDK connection")
        super.onDestroy()
    }

    private fun label(value: String, size: Float): TextView = TextView(this).apply {
        text = value
        textSize = size
        setPadding(0, 12, 0, 12)
    }

    private fun button(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64.dp))
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
