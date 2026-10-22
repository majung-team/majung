package app.majung.vehicle

import android.app.Application

class VehicleApplication : Application() {
    lateinit var destination: DestinationReader
        private set
    lateinit var pickup: PickupController
        private set

    override fun onCreate() {
        super.onCreate()
        destination = DestinationReader(this)
        pickup = PickupController()
        destination.telemetryChanged = { pickup.syncVehicle(destination) }
    }
}
