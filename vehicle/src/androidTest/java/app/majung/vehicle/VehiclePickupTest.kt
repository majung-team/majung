package app.majung.vehicle

import android.os.Build
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import android.widget.Button
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.io.File

/** Real emulator NaviHelper callback -> real local HTTP server. No fake SDK callback. */
class VehiclePickupTest {
    @Test fun arriveAndCompletePickupFromVehicleScreen() {
        assertTrue("Run only on the official emulator", Build.MODEL.contains("emulator", true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = ApplicationProvider.getApplicationContext<VehicleApplication>()
        val controller = app.pickup
        val api = PickupApiClient()
        var selected: PickupPlace? = null
        instrumentation.runOnMainSync { controller.prepareNextPickup(); app.destination.connect() }
        assertTrue("Set a navigation route before this test", await(20_000) {
            selected = app.destination.selection.current(SystemClock.elapsedRealtime())
            selected != null
        })
        try {
            instrumentation.runOnMainSync { controller.create(selected!!) }
            assertTrue("Pickup creation failed", await(15_000) { controller.pickupId != null && !controller.busy })
            var invitation: String? = null
            instrumentation.runOnMainSync {
                assertTrue(!controller.canArrive && !controller.canComplete)
                controller.complete()
                invitation = controller.invitationForHostBrowser()
            }
            val parts = URI(invitation!!).fragment.split('/')
            val id = parts[1]
            val guest = api.request("POST", "/$id/participants", body = JSONObject()
                .put("inviteToken", parts[2]).put("displayName", "시연 탑승자"))
            val token = guest.getString("participantToken")
            instrumentation.runOnMainSync { controller.refresh() }
            assertTrue("Participant not found", await(10_000) { controller.canApprove })
            instrumentation.runOnMainSync { controller.approve() }
            assertTrue("Approval failed", await(10_000) { controller.canArrive })
            api.request("PUT", "/$id/readiness", token, JSONObject().put("readiness", "READY").put("sequence", 1))

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                instrumentation.runOnMainSync { app.destination.pollTelemetry(); controller.refresh() }
                awaitVehicle(api, id, token) { it.optString("freshness") == "FRESH" }
                scenario.onActivity { activity ->
                    assertTrue(!activity.findViewById<Button>(R.id.complete_pickup).isEnabled)
                    controller.complete()
                    activity.findViewById<Button>(R.id.arrive_pickup).performClick()
                }
                clickDialogButton("돌아가기")
                assertEquals("ACTIVE", api.request("GET", "/$id", token).getString("status"))
                scenario.onActivity { it.findViewById<Button>(R.id.arrive_pickup).performClick() }
                clickDialogButton("도착 알리기")
                assertTrue("Arrival failed", await(10_000) { controller.status == "ARRIVED" && !controller.busy })
                assertEquals("ARRIVED", api.request("GET", "/$id", token).getString("status"))
                val stillSharing = awaitVehicle(api, id, token) { it.optString("freshness") == "FRESH" }
                assertTrue(!stillSharing.isNull("latitude"))
                scenario.onActivity { activity ->
                    assertTrue(!activity.findViewById<Button>(R.id.arrive_pickup).isEnabled)
                    assertTrue(activity.findViewById<Button>(R.id.complete_pickup).isEnabled)
                    activity.findViewById<Button>(R.id.complete_pickup).requestRectangleOnScreen(
                        android.graphics.Rect(0, 0, activity.findViewById<Button>(R.id.complete_pickup).width, 60), true)
                }
                saveEvidence(app, "vehicle-arrived.png")
                scenario.onActivity { it.findViewById<Button>(R.id.complete_pickup).performClick() }
                clickDialogButton("돌아가기")
                assertEquals("ARRIVED", api.request("GET", "/$id", token).getString("status"))
                scenario.onActivity { it.findViewById<Button>(R.id.complete_pickup).performClick() }
                clickDialogButton("탑승 확인하고 완료")
                assertTrue("Completion failed", await(10_000) { controller.status == "COMPLETED" && !controller.busy })
                val completed = api.request("GET", "/$id", token)
                assertTrue("Completion must hide place", completed.isNull("place"))
                assertRejected(410) { api.request("GET", "/$id/vehicle-state", token) }
                assertRejected(410) {
                    api.request("PUT", "/$id/readiness", token,
                        JSONObject().put("readiness", "PREPARING").put("sequence", 2))
                }
                instrumentation.runOnMainSync {
                    assertTrue(controller.place == null && controller.participantName == null)
                    assertTrue(!controller.canArrive && !controller.canComplete && !controller.canCancel)
                    assertTrue(controller.invitationForHostBrowser() == null)
                    controller.complete()
                    controller.arrive()
                    controller.syncVehicle(app.destination)
                }
                assertEquals("COMPLETED", api.request("GET", "/$id", token).getString("status"))
                saveEvidence(app, "vehicle-completed.png")
                instrumentation.runOnMainSync { controller.prepareNextPickup() }
                assertTrue("New pickup preparation failed", await(5000) { controller.canCreate && controller.pickupId == null })
            }
        } finally {
            instrumentation.runOnMainSync { if (controller.canCancel) controller.cancel(); app.destination.disconnect() }
        }
    }

    private fun clickDialogButton(text: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val root = instrumentation.uiAutomation.rootInActiveWindow
        val button = root?.findAccessibilityNodeInfosByText(text)?.firstOrNull {
            it.text?.toString() == text && it.isClickable
        }
        assertNotNull("Confirmation button missing", button)
        assertTrue(button!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun assertRejected(status: Int, request: () -> Unit) {
        try {
            request()
            throw AssertionError("Expected request rejection")
        } catch (error: ApiRejected) {
            assertEquals(status, error.status)
        }
    }

    /** Opt-in screenshots contain emulator UI only, never invitation or credential screens. */
    private fun saveEvidence(app: VehicleApplication, name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureEvidence") != "true") return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        SystemClock.sleep(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: throw AssertionError("Screenshot failed")
        File(app.filesDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun createPickupFromNavigationAndApprovePassenger() {
        assertTrue("Run only on the official emulator", Build.MODEL.contains("emulator", true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = ApplicationProvider.getApplicationContext<VehicleApplication>()
        val controller = app.pickup
        val api = PickupApiClient()
        var selected: PickupPlace? = null
        instrumentation.runOnMainSync { controller.prepareNextPickup(); app.destination.connect() }
        assertTrue("No selected navigation destination; set a route before this test", await(20_000) {
            selected = app.destination.selection.current(SystemClock.elapsedRealtime())
            selected != null
        })
        try {
            instrumentation.runOnMainSync {
                controller.create(selected!!)
                // A double tap must not launch another create request.
                controller.create(selected!!)
            }
            assertTrue("Pickup creation did not complete", await(15_000) { !controller.busy && controller.pickupId != null })
            var link: String? = null
            instrumentation.runOnMainSync { link = controller.invitationForHostBrowser() }
            assertNotNull("Invitation missing", link)
            if (InstrumentationRegistry.getArguments().getString("browserEvidence") == "true") {
                browserEvidence(app, controller, link!!)
                return
            }
            val parts = URI(link!!).fragment.split('/')
            val id = parts[1]
            val guest = api.request("POST", "/$id/participants", body = JSONObject()
                .put("inviteToken", parts[2]).put("displayName", "에뮬레이터 시험"))
            val token = guest.getString("participantToken")
            val before = api.request("GET", "/$id", token)
            assertTrue("Place must remain private before approval", before.isNull("place"))
            instrumentation.runOnMainSync { controller.refresh() }
            assertTrue("Join not reflected in vehicle", await(10_000) { controller.canApprove })
            instrumentation.runOnMainSync { controller.approve() }
            assertTrue("Approval failed", await(10_000) { controller.status == "ACTIVE" && !controller.busy })
            val sharedPlace = api.request("GET", "/$id", token).getJSONObject("place")
            assertEquals(selected!!.label, sharedPlace.getString("label"))
            assertTrue("Latitude differs from SDK destination", kotlin.math.abs(selected!!.latitude - sharedPlace.getDouble("latitude")) < 0.000000001)
            assertTrue("Longitude differs from SDK destination", kotlin.math.abs(selected!!.longitude - sharedPlace.getDouble("longitude")) < 0.000000001)
            instrumentation.runOnMainSync { app.destination.pollTelemetry() }
            val live = awaitVehicle(api, id, token) { it.optString("freshness") == "FRESH" }
            var sdkSample: VehicleSample? = null
            instrumentation.runOnMainSync {
                sdkSample = app.destination.telemetry.current(selected!!, SystemClock.elapsedRealtime())
            }
            assertNotNull("SDK pair missing", sdkSample)
            assertEquals(sdkSample!!.durationSeconds, live.getLong("durationSeconds"))
            assertTrue("Latitude differs from SDK location", kotlin.math.abs(sdkSample!!.latitude - live.getDouble("latitude")) < 0.000000001)
            assertTrue("Longitude differs from SDK location", kotlin.math.abs(sdkSample!!.longitude - live.getDouble("longitude")) < 0.000000001)
            instrumentation.runOnMainSync { app.destination.disconnect() }
            val hidden = awaitVehicle(api, id, token) { it.isNull("latitude") && it.isNull("durationSeconds") }
            assertTrue("Invalidation must retain the ordering floor", hidden.getLong("sequence") > live.getLong("sequence"))
            instrumentation.runOnMainSync { app.destination.connect() }
            assertTrue("SDK reconnect failed", await(20_000) {
                app.destination.selection.current(SystemClock.elapsedRealtime()) != null
            })
            instrumentation.runOnMainSync { app.destination.pollTelemetry() }
            assertTrue("Fresh reconnect data did not publish", await(10_000) {
                controller.syncVehicle(app.destination)
                controller.vehicleMessage == "SDK 위치·ETA를 서버에 전달했어요"
            })
            awaitVehicle(api, id, token) { it.optString("freshness") == "FRESH" }
            api.request("PUT", "/$id/readiness", token, JSONObject().put("readiness", "READY").put("sequence", 1))
            instrumentation.runOnMainSync { controller.refresh() }
            assertTrue("Passenger readiness not reflected", await(10_000) { controller.readiness == "READY" && !controller.busy })
            verifyStablePolling(controller)
            instrumentation.runOnMainSync { controller.cancel() }
            assertTrue("Cancellation failed", await(10_000) { controller.status == "CANCELLED" && !controller.busy })
            assertTrue("End must hide place", api.request("GET", "/$id", token).isNull("place"))
        } finally {
            instrumentation.runOnMainSync { if (controller.canCancel) controller.cancel(); app.destination.disconnect() }
        }
    }

    private fun verifyStablePolling(controller: PickupController) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            var detailsHeight = 0
            var cancelTop = 0
            scenario.onActivity { activity ->
                detailsHeight = activity.findViewById<TextView>(R.id.pickup_details).height
                cancelTop = activity.findViewById<Button>(R.id.cancel_pickup).top
            }
            assertTrue("Vehicle view did not lay out", detailsHeight > 0)
            repeat(12) {
                scenario.onActivity { activity ->
                    controller.refresh()
                    assertTrue("Background refresh disabled cancellation", activity.findViewById<Button>(R.id.cancel_pickup).isEnabled)
                    assertEquals("Background refresh changed body height", detailsHeight,
                        activity.findViewById<TextView>(R.id.pickup_details).height)
                    assertEquals("Background refresh shifted buttons", cancelTop,
                        activity.findViewById<Button>(R.id.cancel_pickup).top)
                }
                SystemClock.sleep(500)
                instrumentation.waitForIdleSync()
            }
        }
    }

    /** Opt-in local browser handoff; never write owner/participant credentials or log the invite. */
    private fun browserEvidence(app: VehicleApplication, controller: PickupController, link: String) {
        val invitation = File(app.filesDir, "browser-invite.json")
        val hide = File(app.filesDir, "browser-hide-now")
        val finish = File(app.filesDir, "browser-finish")
        hide.delete()
        finish.delete()
        try {
            invitation.writeText(JSONObject().put("inviteLink", link).toString())
            assertTrue("Browser did not join", await(120_000) {
                if (controller.canApprove) true else { controller.refresh(); false }
            })
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.approve() }
            assertTrue("Browser pickup approval failed", await(10_000) { controller.status == "ACTIVE" && !controller.busy })
            var lastPoll = 0L
            assertTrue("Browser evidence live phase timed out", await(120_000) {
                if (SystemClock.elapsedRealtime() - lastPoll >= 5000) {
                    app.destination.pollTelemetry()
                    lastPoll = SystemClock.elapsedRealtime()
                }
                controller.syncVehicle(app.destination)
                hide.exists()
            })
            InstrumentationRegistry.getInstrumentation().runOnMainSync { app.destination.disconnect() }
            assertTrue("Browser evidence hidden phase timed out", await(120_000) {
                controller.syncVehicle(app.destination)
                finish.exists()
            })
        } finally {
            invitation.delete()
            hide.delete()
            finish.delete()
        }
    }

    private fun awaitVehicle(api: PickupApiClient, id: String, token: String,
                             condition: (JSONObject) -> Boolean): JSONObject {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            val data = api.request("GET", "/$id/vehicle-state", token)
            if (condition(data)) return data
            SystemClock.sleep(150)
        }
        throw AssertionError("Vehicle state did not reach the expected condition")
    }

    private fun await(timeout: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            var done = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync { done = condition() }
            if (done) return true
            SystemClock.sleep(100)
        }
        return false
    }
}
