package app.majung.probe

import android.app.Application

class ProbeApplication : Application() {
    lateinit var probe: NaviProbe
        private set

    override fun onCreate() {
        super.onCreate()
        probe = NaviProbe(this)
    }
}
