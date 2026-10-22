package app.majung.vehicle

/** Destination returned by NaviHelper, never a typed coordinate or a location fix. */
data class PickupPlace(val label: String, val latitude: Double, val longitude: Double)

class DestinationSelection {
    private var place: PickupPlace? = null
    private var receivedAt: Long = 0

    fun receive(label: String, latitude: Double, longitude: Double, now: Long) {
        place = if (label.isNotBlank() && label.length <= 120 && latitude.isFinite()
            && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
        ) PickupPlace(label.trim(), latitude, longitude) else null
        receivedAt = now
    }

    fun current(now: Long): PickupPlace? = place?.takeIf {
        now >= receivedAt && now - receivedAt < 30_000
    }

    fun invalidate() { place = null }
}
