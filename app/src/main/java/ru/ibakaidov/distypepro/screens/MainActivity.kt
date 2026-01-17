package ru.ibakaidov.distypepro.screens

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityMainBinding
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.utils.Tts
import ru.ibakaidov.distypepro.utils.TtsHolder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: Tts
    private val sdk by lazy { SharedSdkProvider.get(this) }
    private var currentSlotIndex: Int = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var realtimeJob: Job? = null
    private val slotLabels = intArrayOf(
        R.string.chat_slot_one,
        R.string.chat_slot_two,
        R.string.chat_slot_three
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        flushOfflineQueue()
        startPeriodicSync()
        startRealtimeSync()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        binding.toolbar.subtitle = ""
        applyWindowInsets()

        WindowCompat.getInsetsController(window, binding.root)?.let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !isNightMode()
        }

        tts = TtsHolder.get(this)
        binding.inputGroup.setTts(tts)
        binding.bankGroup.setTts(tts)
        observeTtsEvents()

        onBackPressedDispatcher.addCallback(this) {
            binding.inputGroup.back()
            binding.bankGroup.back()
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
    }

    override fun onStop() {
        unregisterNetworkCallback()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding.bankGroup.refresh()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.chat_selector_menu_item -> {
            showChatSelectorPopup()
            true
        }

        R.id.spotlight_menu_item -> {
            binding.inputGroup.spotlight()
            true
        }

        R.id.clear_menu_item -> {
            binding.inputGroup.clear()
            true
        }

        R.id.settings_menu_item -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.dialog_menu_item -> {
            startActivity(Intent(this, DialogActivity::class.java))
            true
        }
        R.id.logout_menu_item -> {
            lifecycleScope.launch {
                runCatching { sdk.authRepository.logout() }
                val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val chatSelectorItem = menu.findItem(R.id.chat_selector_menu_item)
        chatSelectorItem?.title = getString(slotLabels[currentSlotIndex])
        return super.onPrepareOptionsMenu(menu)
    }

    private fun showChatSelectorPopup() {
        val popup = androidx.appcompat.widget.PopupMenu(this, binding.toolbar)
        slotLabels.forEachIndexed { index, labelRes ->
            popup.menu.add(0, index, index, labelRes)
        }
        popup.setOnMenuItemClickListener { item ->
            val slotIndex = item.itemId
            if (slotIndex in slotLabels.indices && slotIndex != currentSlotIndex) {
                binding.inputGroup.switchSlot(currentSlotIndex, slotIndex)
                currentSlotIndex = slotIndex
                invalidateOptionsMenu()
            }
            true
        }
        popup.show()
    }

    private fun switchChatSlot(slotIndex: Int) {
        if (slotIndex in slotLabels.indices && slotIndex != currentSlotIndex) {
            binding.inputGroup.switchSlot(currentSlotIndex, slotIndex)
            currentSlotIndex = slotIndex
            invalidateOptionsMenu()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized && isFinishing) {
            tts.shutdown()
            TtsHolder.clear()
        }
    }

    private fun flushOfflineQueue() {
        lifecycleScope.launch {
            sdk.offlineQueueProcessor.flush()
        }
    }

    private fun startPeriodicSync() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    sdk.offlineQueueProcessor.flush()
                    delay(SYNC_INTERVAL_MS)
                }
            }
        }
    }

    private fun startRealtimeSync() {
        if (realtimeJob != null) return
        realtimeJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val result = runCatching { sdk.changesSyncer.pollOnce() }
                    result.onSuccess { response ->
                        if (response.changes.isNotEmpty()) {
                            Firebase.analytics.logEvent(
                                "realtime_sync",
                                bundleOf("changes" to response.changes.size),
                            )
                            binding.bankGroup.refresh()
                        }
                    }.onFailure { error ->
                        Firebase.analytics.logEvent(
                            "realtime_sync_error",
                            bundleOf("message" to (error.message ?: "unknown")),
                        )
                        delay(REALTIME_RETRY_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                flushOfflineQueue()
            }
        }
        connectivityManager.registerNetworkCallback(request, callback)
        networkCallback = callback
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.unregisterNetworkCallback(callback)
        networkCallback = null
    }

    private fun applyWindowInsets() {
        val appBar = binding.appBar
        val content = binding.contentContainer
        val appBarInitialTop = appBar.paddingTop
        val contentInitialLeft = content.paddingLeft
        val contentInitialTop = content.paddingTop
        val contentInitialRight = content.paddingRight
        val contentInitialBottom = content.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(appBar) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = appBarInitialTop + statusBars.top)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = contentInitialLeft + systemBars.left,
                top = contentInitialTop,
                right = contentInitialRight + systemBars.right,
                bottom = contentInitialBottom + systemBars.bottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun observeTtsEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tts.events().collect { event ->
                    when (event) {
                        is Tts.TtsEvent.DownloadCompleted -> {
                            val message = event.path?.let {
                                getString(R.string.tts_download_saved, it)
                            } ?: getString(R.string.tts_download_finished)
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        }

                        is Tts.TtsEvent.TemporarilyUnavailable -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                        }

                        is Tts.TtsEvent.Error -> {
                            val text = getString(R.string.tts_status_error, event.message)
                            Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun isNightMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private companion object {
        private const val SYNC_INTERVAL_MS = 60_000L
        private const val REALTIME_RETRY_DELAY_MS = 3_000L
    }
}
