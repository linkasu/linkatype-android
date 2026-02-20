package ru.ibakaidov.distypepro.screens

import android.content.Intent
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.shared.session.AppMode

class SplashActivity : AppCompatActivity() {

    private val sdk by lazy { SharedSdkProvider.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            sdk.sessionRepository.getOrCreateDeviceId()
            val mode = sdk.sessionRepository.getMode()
            if (mode == AppMode.OFFLINE) {
                navigateToMain()
                return@launch
            }

            val refreshToken = sdk.tokenStorage.getRefreshToken()
            if (mode == null && !refreshToken.isNullOrBlank()) {
                sdk.sessionRepository.setMode(AppMode.ONLINE)
            }
            if (refreshToken.isNullOrBlank()) {
                navigateToAuth()
                return@launch
            }

            if (!isNetworkAvailable()) {
                navigateToMain()
                return@launch
            }

            runCatching { sdk.authRepository.refresh() }
                .onSuccess { navigateToMain() }
                .onFailure { navigateToAuth() }
        }
    }

    private fun navigateToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
