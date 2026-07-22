package ru.ibakaidov.distypepro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.ibakaidov.distypepro.telemetry.TelemetryService

@HiltAndroidApp
class LinkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TelemetryService.get(this).start()
    }
}
