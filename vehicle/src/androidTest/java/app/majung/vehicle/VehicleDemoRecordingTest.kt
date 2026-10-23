package app.majung.vehicle

import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt-in recording of real UI + SDK + local HTTP, with a human/browser handoff at each step. */
class VehicleDemoRecordingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = ApplicationProvider.getApplicationContext<VehicleApplication>()
    private val folder get() = File(app.filesDir, "demo-recording")

    @Test fun pickupWithPassengerBrowser() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("recordDemo") == "true")
        assertTrue(Build.MODEL.contains("emulator", true))
        folder.mkdirs()
        folder.listFiles()?.forEach { it.delete() }
        val controller = app.pickup
        try {
            instrumentation.runOnMainSync { controller.prepareNextPickup() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                press(scenario, "내비게이션 연결")
                await("Navigation destination missing") {
                    app.destination.selection.current(SystemClock.elapsedRealtime()) != null
                }
                phase("ready")
                gate("create")
                press(scenario, "이 장소로 픽업 만들기")
                await("Creation failed") { controller.pickupId != null && !controller.busy }
                var invitation: String? = null
                instrumentation.runOnMainSync { invitation = controller.invitationForHostBrowser() }
                assertTrue("Invitation missing", invitation != null)
                File(folder, "invitation.json").writeText(JSONObject().put("inviteLink", invitation).toString())
                phase("inviting")
                await("Passenger did not join", 180_000) { controller.canApprove }
                phase("waiting-approval")
                gate("approve")
                press(scenario, "참여자 확인하고 공유 시작")
                confirm("확인하고 시작")
                await("Approval failed") { controller.status == "ACTIVE" && !controller.busy }
                phase("active")
                await("Passenger not ready", 180_000) { controller.readiness == "READY" }
                phase("passenger-ready")
                gate("arrive")
                press(scenario, "도착 알리기 · 공유 유지")
                confirm("도착 알리기")
                await("Arrival failed") { controller.status == "ARRIVED" && !controller.busy }
                phase("arrived")
                gate("complete")
                press(scenario, "탑승 확인 · 픽업 완료")
                confirm("탑승 확인하고 완료")
                await("Completion failed") { controller.status == "COMPLETED" && !controller.busy }
                phase("completed")
                gate("finish")
            }
        } finally {
            try {
                instrumentation.runOnMainSync {
                    if (controller.canCancel) controller.cancel()
                    app.destination.disconnect()
                }
                await("Cleanup still busy") { !controller.busy }
            } finally {
                folder.listFiles()?.forEach { it.delete() }
            }
        }
    }

    private fun press(scenario: ActivityScenario<MainActivity>, text: String) {
        scenario.onActivity { activity ->
            fun find(view: View): Button? {
                if (view is Button && view.text.toString() == text) return view
                if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
                return null
            }
            val button = find(activity.window.decorView) ?: throw AssertionError("Demo button missing")
            assertTrue("Demo button disabled", button.isEnabled)
            button.performClick()
        }
        instrumentation.waitForIdleSync()
    }

    private fun confirm(text: String) {
        instrumentation.waitForIdleSync()
        val button = instrumentation.uiAutomation.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(text)?.firstOrNull { it.text?.toString() == text && it.isClickable }
            ?: throw AssertionError("Confirmation missing")
        assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun phase(value: String) {
        File(folder, "phase.txt").writeText(value)
    }

    private fun gate(name: String) {
        val file = File(folder, "$name.go")
        val deadline = SystemClock.elapsedRealtime() + 180_000
        while (!file.exists() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertTrue("Demo handoff timed out: $name", file.exists())
        file.delete()
    }

    private fun await(message: String, timeout: Long = 20_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            var done = false
            instrumentation.runOnMainSync { done = condition() }
            if (done) return
            SystemClock.sleep(100)
        }
        throw AssertionError(message)
    }
}
