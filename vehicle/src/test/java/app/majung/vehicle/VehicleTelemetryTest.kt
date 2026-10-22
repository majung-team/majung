package app.majung.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class VehicleTelemetryTest {
    private val place = PickupPlace("픽업 장소", 37.0, 127.0)
    private val time = Instant.parse("2026-10-23T00:00:00Z")

    @Test fun needsBothLocationAndEtaForTheConfirmedPlace() {
        val buffer = VehicleTelemetry()
        buffer.location(36.0, 126.0, 1000, time)
        assertNull(buffer.current(place, 1001))
        buffer.destination(place, 120, 1500, time.plusMillis(500))
        val sample = buffer.current(place, 1501)!!
        assertEquals(120, sample.durationSeconds)
        assertEquals(time, sample.observedAt)
        assertEquals(36.0, sample.latitude, 0.0)
    }

    @Test fun anotherDestinationCannotPublishEtaForThisPickup() {
        val buffer = VehicleTelemetry()
        buffer.location(36.0, 126.0, 1000, time)
        buffer.destination(place.copy(latitude = 38.0), 120, 1000, time)
        assertNull(buffer.current(place, 1001))
    }

    @Test fun newLocationDoesNotRefreshAnOldEta() {
        val buffer = VehicleTelemetry()
        buffer.location(36.0, 126.0, 1000, time)
        buffer.destination(place, 120, 1000, time)
        buffer.location(36.1, 126.1, 10_000, time.plusSeconds(9))
        assertEquals(time, buffer.current(place, 10_001)!!.observedAt)
        assertNull(buffer.current(place, 11_000))
    }

    @Test fun invalidationRequiresBothComponentsAgain() {
        val buffer = VehicleTelemetry()
        buffer.location(36.0, 126.0, 1000, time)
        buffer.destination(place, 120, 1000, time)
        buffer.invalidate()
        buffer.destination(place, 100, 2000, time.plusSeconds(1))
        assertNull(buffer.current(place, 2001))
        buffer.location(36.1, 126.1, 2001, time.plusMillis(1001))
        assertEquals(100, buffer.current(place, 2002)!!.durationSeconds)
    }

    @Test fun rejectsInvalidCoordinatesNegativeEtaAndClockRollback() {
        val buffer = VehicleTelemetry()
        buffer.destination(place, 120, 1000, time)
        buffer.location(Double.NaN, 126.0, 1000, time)
        assertNull(buffer.current(place, 1001))
        buffer.location(36.0, 126.0, 1000, time)
        buffer.destination(place, -1, 1000, time)
        assertNull(buffer.current(place, 1001))
        buffer.destination(place, 0, 1000, time)
        assertNull(buffer.current(place, 999))
        assertEquals(0, buffer.current(place, 1001)!!.durationSeconds)
    }
}
