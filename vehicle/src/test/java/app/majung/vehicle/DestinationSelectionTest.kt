package app.majung.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationSelectionTest {
    @Test fun usesNavigationDestinationAndExpiresAtThirtySeconds() {
        val selection = DestinationSelection()
        selection.receive("선택한 장소", 37.0, 127.0, 1000)
        assertEquals(PickupPlace("선택한 장소", 37.0, 127.0), selection.current(30_999))
        assertNull(selection.current(31_000))
    }
    @Test fun invalidationDoesNotReusePreviousDestination() {
        val selection = DestinationSelection()
        selection.receive("이전 장소", 37.0, 127.0, 1000)
        selection.invalidate()
        assertNull(selection.current(1001))
        selection.receive("새 장소", 38.0, 128.0, 2000)
        assertEquals("새 장소", selection.current(2001)?.label)
    }
    @Test fun invalidOrUnnamedCoordinatesCannotCreatePickup() {
        val selection = DestinationSelection()
        for ((name, lat, lng) in listOf(Triple("", 37.0, 127.0), Triple("장소", Double.NaN, 127.0),
            Triple("장소", 91.0, 127.0), Triple("장소", 37.0, 181.0))) {
            selection.receive(name, lat, lng, 1000)
            assertNull(selection.current(1001))
        }
    }
}
