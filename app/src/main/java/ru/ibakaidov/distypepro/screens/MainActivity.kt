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
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.bank.BankPlacement
import ru.ibakaidov.distypepro.bank.BankPlacementStore
import ru.ibakaidov.distypepro.components.BankActionHandler
import ru.ibakaidov.distypepro.components.BankChromeMenuBinder
import ru.ibakaidov.distypepro.components.BankChromeState
import ru.ibakaidov.distypepro.databinding.ActivityMainBinding
import ru.ibakaidov.distypepro.dialogs.ConfirmDialog
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.shared.session.AppMode
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryCountBucket
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryFailureCode
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryOutcome
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryOutcomeKind
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryResult
import ru.ibakaidov.distypepro.telemetry.TelemetryService
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.Tts
import ru.ibakaidov.distypepro.utils.TtsHolder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: Tts
    private val sdk by lazy { SharedSdkProvider.get(this) }
    private val bankPlacementStore by lazy { BankPlacementStore(this) }
    private val telemetry by lazy { TelemetryService.get(this) }
    private val isOfflineMode by lazy { sdk.sessionRepository.getMode() == AppMode.OFFLINE }
    private var currentSlotIndex: Int = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var realtimeJob: Job? = null
    private var currentBankPlacement: BankPlacement = BankPlacement.MAIN
    private var inlineBankChromeState: BankChromeState? = null
    private var isInputFocused: Boolean = false
    private val slotLabels = listOf(
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
        if (!isOfflineMode) {
            flushOfflineQueue()
            startPeriodicSync()
            startRealtimeSync()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.chatSlotButton.setOnClickListener { showChatSelectorPopup() }
        binding.openBankCard.setOnClickListener { openBankScreen() }
        setupInlineBankToolbar()
        updateChatSelectorTitle()
        applyWindowInsets()

        WindowCompat.getInsetsController(window, binding.root)?.let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !isNightMode()
        }

        tts = TtsHolder.get(this)
        binding.inputGroup.setTts(tts)
        binding.inputGroup.setOnInputFocusChangedListener { hasFocus ->
            if (isInputFocused == hasFocus) return@setOnInputFocusChangedListener
            isInputFocused = hasFocus
            applyBankPlacement(refreshInlineBank = false)
        }
        binding.inlineBankGroup.setTts(tts)
        binding.inlineBankGroup.setChromeStateListener { state ->
            inlineBankChromeState = state
            renderInlineBankChrome(state)
        }
        currentBankPlacement = bankPlacementStore.get()
        applyBankPlacement(refreshInlineBank = true)
        observeTtsEvents()

        onBackPressedDispatcher.addCallback(this) {
            if (binding.inputGroup.back()) {
                return@addCallback
            }
            if (isInlineBankActive() && binding.inlineBankGroup.back()) {
                return@addCallback
            }
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        applyBankPlacement(refreshInlineBank = true)
    }

    override fun onStart() {
        super.onStart()
        if (!isOfflineMode) {
            registerNetworkCallback()
        }
    }

    override fun onStop() {
        if (!isOfflineMode) {
            unregisterNetworkCallback()
        }
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val logoutTitle = if (isOfflineMode) {
            R.string.auth_online_required_action
        } else {
            R.string.logout
        }
        menu.findItem(R.id.logout_menu_item)?.setTitle(logoutTitle)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
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
            if (isOfflineMode) {
                openOnlineAuth()
            } else {
                confirmLogout()
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun showChatSelectorPopup() {
        val popup = androidx.appcompat.widget.PopupMenu(this, binding.chatSlotButton)
        slotLabels.forEachIndexed { index, labelRes ->
            popup.menu.add(Menu.NONE, index, index, labelRes)
        }
        popup.setOnMenuItemClickListener { item ->
            switchChatSlot(item.itemId)
            true
        }
        popup.show()
    }

    private fun switchChatSlot(slotIndex: Int) {
        currentSlotIndex = safeSlotIndex(currentSlotIndex)
        val targetIndex = safeSlotIndex(slotIndex)
        if (targetIndex != currentSlotIndex) {
            binding.inputGroup.switchSlot(currentSlotIndex, targetIndex)
            currentSlotIndex = targetIndex
        }
        updateChatSelectorTitle()
    }

    private fun updateChatSelectorTitle() {
        currentSlotIndex = safeSlotIndex(currentSlotIndex)
        binding.chatSlotButton.text = getString(slotLabels[currentSlotIndex])
    }

    private fun setupInlineBankToolbar() {
        binding.bankInlineToolbar.inflateMenu(R.menu.bank_toolbar_menu)
        binding.bankInlineToolbar.setNavigationOnClickListener {
            binding.inlineBankGroup.back()
        }
        binding.bankInlineToolbar.setOnMenuItemClickListener { item ->
            BankActionHandler.handle(item.itemId, binding.inlineBankGroup)
        }
    }

    private fun renderInlineBankChrome(state: BankChromeState) {
        val navigationIcon = if (state.canNavigateBack) {
            AppCompatResources.getDrawable(this, R.drawable.ic_baseline_arrow_back_24)
        } else {
            null
        }
        binding.bankInlineToolbar.title = state.title
        binding.bankInlineToolbar.navigationIcon = navigationIcon
        binding.bankInlineToolbar.navigationContentDescription = if (state.canNavigateBack) {
            getString(R.string.bank_back)
        } else {
            null
        }
        BankChromeMenuBinder.bind(binding.bankInlineToolbar.menu, state)
    }

    private fun applyBankPlacement(refreshInlineBank: Boolean) {
        val placement = bankPlacementStore.get()
        val placementChanged = placement != currentBankPlacement
        currentBankPlacement = placement
        val showInlineBank = placement == BankPlacement.MAIN && !isInputFocused

        updateSectionLayout(placement, showInlineBank)
        binding.bankInlineSection.isVisible = showInlineBank
        binding.openBankCard.isVisible = placement == BankPlacement.SEPARATE_ACTIVITY

        if (showInlineBank) {
            renderInlineBankChrome(inlineBankChromeState ?: binding.inlineBankGroup.chromeState())
            if (refreshInlineBank || placementChanged) {
                binding.inlineBankGroup.refresh()
            }
        }
    }

    private fun updateSectionLayout(
        placement: BankPlacement,
        showInlineBank: Boolean,
    ) {
        val inlineSpacing = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val regularSpacing = resources.getDimensionPixelSize(R.dimen.spacing_large)

        binding.inputGroup.updateLayoutParams<LinearLayout.LayoutParams> {
            if (placement == BankPlacement.MAIN && showInlineBank) {
                height = 0
                weight = INPUT_SECTION_WEIGHT
                bottomMargin = inlineSpacing
            } else if (placement == BankPlacement.MAIN) {
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                weight = 0f
                bottomMargin = 0
            } else {
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                weight = 0f
                bottomMargin = regularSpacing
            }
        }

        binding.bankInlineSection.updateLayoutParams<LinearLayout.LayoutParams> {
            height = if (showInlineBank) 0 else LinearLayout.LayoutParams.WRAP_CONTENT
            weight = if (showInlineBank) BANK_SECTION_WEIGHT else 0f
        }
    }

    private fun isInlineBankActive(): Boolean {
        return currentBankPlacement == BankPlacement.MAIN && binding.bankInlineSection.isVisible
    }

    private fun openBankScreen() {
        startActivity(Intent(this, BankActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun safeSlotIndex(index: Int): Int = index.coerceIn(slotLabels.indices)

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

    private fun confirmLogout() {
        ConfirmDialog.showConfirmDialog(this, R.string.logout_confirm, object : Callback<Unit> {
            override fun onDone(result: Unit) {
                performLogout()
            }
        })
    }

    private fun performLogout() {
        lifecycleScope.launch {
            runCatching { sdk.authRepository.logout() }
            val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun openOnlineAuth() {
        val intent = Intent(this, AuthActivity::class.java).apply {
            putExtra(AuthActivity.EXTRA_FORCE_ONLINE_MODE, true)
        }
        startActivity(intent)
        finish()
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
                            telemetry.report(
                                TelemetryOutcome(
                                    kind = TelemetryOutcomeKind.SYNC_COMPLETED,
                                    result = TelemetryResult.COMPLETED,
                                    countBucket = TelemetryCountBucket.fromCount(response.changes.size),
                                ),
                            )
                        }
                    }.onFailure {
                        telemetry.report(
                            TelemetryOutcome(
                                kind = TelemetryOutcomeKind.SYNC_COMPLETED,
                                result = TelemetryResult.FAILED,
                                countBucket = TelemetryCountBucket.ONE,
                                failureCode = TelemetryFailureCode.NETWORK_UNAVAILABLE,
                            ),
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
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(
                left = contentInitialLeft + systemBars.left,
                top = contentInitialTop,
                right = contentInitialRight + systemBars.right,
                bottom = contentInitialBottom + maxOf(systemBars.bottom, ime.bottom)
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
        private const val INPUT_SECTION_WEIGHT = 2f
        private const val BANK_SECTION_WEIGHT = 8f
    }
}
