package app.majung.vehicle

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/** Real emulator NaviHelper callback -> real local HTTP server. No fake SDK callback. */
class VehiclePickupTest {
    @Test fun createPickupFromNavigationAndApprovePassenger() {
        assertTrue("Run only on the official emulator", Build.MODEL.contains("emulator", true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = ApplicationProvider.getApplicationContext<VehicleApplication>()
        val controller = app.pickup
        val api = PickupApiClient()
        var selected: PickupPlace? = null
        instrumentation.runOnMainSync { app.destination.connect() }
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
            api.request("PUT", "/$id/readiness", token, JSONObject().put("readiness", "READY").put("sequence", 1))
            instrumentation.runOnMainSync { controller.refresh() }
            assertTrue("Passenger readiness not reflected", await(10_000) { controller.readiness == "READY" && !controller.busy })
            instrumentation.runOnMainSync { controller.cancel() }
            assertTrue("Cancellation failed", await(10_000) { controller.status == "CANCELLED" && !controller.busy })
            assertTrue("End must hide place", api.request("GET", "/$id", token).isNull("place"))
        } finally {
            instrumentation.runOnMainSync { if (controller.canCancel) controller.cancel(); app.destination.disconnect() }
        }
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
