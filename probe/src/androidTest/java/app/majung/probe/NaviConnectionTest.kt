package app.majung.probe

import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integration against the emulator's real Navi Service, never a fake callback source. */
class NaviConnectionTest {
    @Test
    fun receiveEtaForOfficialExampleRoute() {
        assertTrue("Only run the probe against an emulator", Build.MODEL.contains("emulator", true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = ApplicationProvider.getApplicationContext<ProbeApplication>()
        val probe: NaviProbe = app.probe
        instrumentation.runOnMainSync { probe.connect() }
        assertTrue("NaviHelper connection failed", await(20_000) { probe.connection == "연결 성공" })
        val emptyPoiId: Boolean = InstrumentationRegistry.getArguments().getString("poiMode") == "empty"
        instrumentation.runOnMainSync { probe.requestExampleRoute(emptyPoiId) }
        val received: Boolean = await(35_000) {
            Regex("예상 [1-9][0-9]*초").containsMatchIn(probe.driving)
        }
        instrumentation.runOnMainSync { probe.query() }
        SystemClock.sleep(3000)
        instrumentation.runOnMainSync { probe.disconnect() }
        assertTrue("No positive onDrivingInfo duration after route request; inspect navigation state and event log", received)
    }

    @Test
    fun receiveLocationAfterActivityStops() {
        assertTrue("Only run the probe against an emulator", Build.MODEL.contains("emulator", true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val probe: NaviProbe = ApplicationProvider.getApplicationContext<ProbeApplication>().probe
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.runOnMainSync { probe.connect() }
            assertTrue("NaviHelper connection failed", await(20_000) { probe.connection == "연결 성공" })
            assertTrue("Initial location not received", await(10_000) { probe.locationRevision > 0 })
            scenario.moveToState(Lifecycle.State.CREATED)
            var before = 0
            instrumentation.runOnMainSync { before = probe.locationRevision; probe.query() }
            val received = await(10_000) { probe.locationRevision > before }
            instrumentation.runOnMainSync { probe.disconnect() }
            assertTrue("No location response while Activity was stopped", received)
        }
    }

    @Test
    fun connectAndReceiveLocation() {
        assertTrue("Only run the probe against an emulator", Build.MODEL.contains("emulator", true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = ApplicationProvider.getApplicationContext<ProbeApplication>()
        val probe: NaviProbe = app.probe
        instrumentation.runOnMainSync { probe.connect() }
        assertTrue("NaviHelper service did not connect in 20 seconds", await(20_000) {
            probe.connection == "연결 성공"
        })
        instrumentation.runOnMainSync { probe.query() }
        assertTrue("Connected, but no current-location callback in 15 seconds", await(15_000) {
            probe.location.startsWith("위도")
        })
        // Observe unsolicited updates before release; do not count manual polling as a stream.
        SystemClock.sleep(5000)
        instrumentation.runOnMainSync { probe.disconnect() }
    }

    private fun await(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline: Long = SystemClock.elapsedRealtime() + timeoutMs
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        while (SystemClock.elapsedRealtime() < deadline) {
            var satisfied = false
            instrumentation.runOnMainSync { satisfied = predicate() }
            if (satisfied) return true
            SystemClock.sleep(100)
        }
        return false
    }
}
