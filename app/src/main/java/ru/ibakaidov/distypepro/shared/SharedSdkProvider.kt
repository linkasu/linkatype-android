package ru.ibakaidov.distypepro.shared

import android.content.Context
import ru.ibakaidov.distypepro.BuildConfig
import ru.ibakaidov.distypepro.shared.auth.PlatformContext

object SharedSdkProvider {
    @Volatile
    private var instance: SharedSdk? = null

    fun get(context: Context): SharedSdk {
        return instance ?: synchronized(this) {
            instance ?: SharedSdk(
                baseUrl = BuildConfig.BACKEND_URL,
                platformContext = context.applicationContext,
            ).also { instance = it }
        }
    }
}
