package app.majung.vehicle

import java.time.Instant
import kotlin.math.abs

/** SDK callback receipt times, not GPS acquisition times (the SDK does not supply those). */
data class VehicleSample(
    val latitude: Double,
    val longitude: Double,
    val durationSeconds: Long,
    val observedAt: Instant,
    val revision: Long
)

class VehicleTelemetry {
    private data class Fix(val latitude: Double, val longitude: Double, val elapsed: Long, val wall: Instant)
    private data class Estimate(val destination: PickupPlace, val seconds: Long, val elapsed: Long, val wall: Instant)
    private var fix: Fix? = null
    private var estimate: Estimate? = null
    private var revision = 0L

    fun location(latitude: Double, longitude: Double, elapsed: Long, wall: Instant) {
        fix = if (latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0
            && longitude in -180.0..180.0) Fix(latitude, longitude, elapsed, wall) else null
        revision++
    }

    fun destination(place: PickupPlace, seconds: Long, elapsed: Long, wall: Instant) {
        estimate = if (seconds >= 0) Estimate(place, seconds, elapsed, wall) else null
        revision++
    }

    fun invalidate() {
        fix = null
        estimate = null
        revision++
    }

    fun current(pickupPlace: PickupPlace, elapsed: Long): VehicleSample? {
        val location = fix ?: return null
        val eta = estimate ?: return null
        if (elapsed < location.elapsed || elapsed < eta.elapsed
            || elapsed - location.elapsed >= 10_000 || elapsed - eta.elapsed >= 10_000) return null
        if (abs(eta.destination.latitude - pickupPlace.latitude) > 0.000001
            || abs(eta.destination.longitude - pickupPlace.longitude) > 0.000001) return null
        // Keep the oldest receipt time; sending again must not make an old component fresh.
        return VehicleSample(location.latitude, location.longitude, eta.seconds,
            minOf(location.wall, eta.wall), revision)
    }
}
