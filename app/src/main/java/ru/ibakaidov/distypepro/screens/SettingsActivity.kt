package ru.ibakaidov.distypepro.screens

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivitySettingsBinding
import ru.ibakaidov.distypepro.utils.Tts
import ru.ibakaidov.distypepro.utils.TtsHolder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var tts: Tts

    private var voiceItems: List<VoiceItem> = emptyList()
    private var eventsJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TtsHolder.get(this)

        applyWindowInsets()
        setupToolbar()
        setupControls()
        observeTtsEvents()
        loadInitialValues()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupControls() {
        val hintColor = ColorStateList.valueOf(
            MaterialColors.getColor(binding.voiceInputLayout, com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        binding.voiceInputLayout.setHintTextColor(hintColor)
        binding.voiceInputLayout.setEndIconTintList(hintColor)
        binding.voiceDropdown.setTextColor(
            MaterialColors.getColor(binding.voiceDropdown, com.google.android.material.R.attr.colorOnSurface)
        )
        binding.voiceDropdown.setHintTextColor(hintColor)

        binding.switchUseYandex.setOnCheckedChangeListener { _, isChecked ->
            tts.setUseYandex(isChecked)
            updateVoiceList(isChecked)
        }

        binding.voiceDropdown.setOnItemClickListener { _, _, position, _ ->
            voiceItems.getOrNull(position)?.let { item ->
                tts.setVoice(item.id)
            }
        }

        binding.volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tts.setVolume(value)
            }
        }

        binding.rateSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tts.setRate(value)
            }
        }

        binding.pitchSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tts.setPitch(value)
            }
        }

        binding.buttonTest.setOnClickListener {
            tts.speak(getString(R.string.settings_test_phrase))
        }

        binding.buttonStop.setOnClickListener { tts.stop() }

        binding.buttonPlayLast.setOnClickListener { tts.playLastAudio() }

        binding.switchCacheEnabled.setOnCheckedChangeListener { _, isChecked ->
            tts.setCacheEnabled(isChecked)
            updateCacheControlsEnabled(isChecked)
            showSnackbar(
                if (isChecked) getString(R.string.settings_cache_enabled_on)
                else getString(R.string.settings_cache_enabled_off)
            )
        }

        binding.cacheLimitSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tts.setCacheSizeLimitMb(value.toDouble())
                updateCacheLimitLabel(value.toDouble())
            }
        }

        binding.buttonClearCache.setOnClickListener {
            lifecycleScope.launch {
                binding.buttonClearCache.isEnabled = false
                tts.clearCache()
                updateCacheInfo()
                showSnackbar(getString(R.string.settings_cache_cleared))
                binding.buttonClearCache.isEnabled = true
            }
        }

        binding.buttonRefreshCache.setOnClickListener {
            lifecycleScope.launch { updateCacheInfo() }
        }

        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun loadInitialValues() {
        binding.switchUseYandex.isChecked = tts.getUseYandex()
        binding.volumeSlider.value = tts.getVolume()
        binding.rateSlider.value = tts.getRate()
        binding.pitchSlider.value = tts.getPitch()
        updateVoiceList(binding.switchUseYandex.isChecked)

        binding.switchCacheEnabled.isChecked = tts.getCacheEnabled()
        updateCacheControlsEnabled(binding.switchCacheEnabled.isChecked)
        binding.cacheLimitSlider.value = tts.getCacheSizeLimitMb().toFloat().coerceIn(100f, 10000f)
        updateCacheLimitLabel(tts.getCacheSizeLimitMb())
        lifecycleScope.launch { updateCacheInfo() }
    }

    private fun updateVoiceList(useYandex: Boolean) {
        lifecycleScope.launch {
            val voices = if (useYandex) {
                tts.getYandexVoices().map { voice ->
                    VoiceItem(voice.voiceURI, "${voice.text} (${getString(R.string.settings_voice_yandex)})")
                }
            } else {
                val offline = tts.getOfflineVoices()
                if (offline.isEmpty()) {
                    showSnackbar(getString(R.string.settings_no_offline_voices))
                }
                offline.map { voice ->
                    val locale = voice.locale ?: ""
                    VoiceItem(voice.voiceId, "${voice.title}${if (locale.isNotEmpty()) " ($locale)" else ""}")
                }
            }

            voiceItems = voices
            val adapter = ArrayAdapter(
                this@SettingsActivity,
                R.layout.item_dropdown_menu,
                voices.map { it.label }
            ).apply {
                setDropDownViewResource(R.layout.item_dropdown_menu_dropdown)
            }
            binding.voiceDropdown.setAdapter(adapter)

            val selected = tts.getSelectedVoice().voiceId
            val index = voices.indexOfFirst { it.id == selected }
            if (index >= 0) {
                binding.voiceDropdown.setText(voices[index].label, false)
            } else if (voices.isNotEmpty()) {
                binding.voiceDropdown.setText(voices.first().label, false)
            } else {
                binding.voiceDropdown.setText("", false)
            }

            binding.voiceInputLayout.isEnabled = voices.isNotEmpty()
        }
    }

    private fun observeTtsEvents() {
        eventsJob?.cancel()
        eventsJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tts.events().collect { event ->
                    when (event) {
                        is Tts.TtsEvent.SpeakingStarted ->
                            binding.ttsStatusText.text = getString(R.string.tts_status_speaking)

                        is Tts.TtsEvent.SpeakingCompleted ->
                            binding.ttsStatusText.text = getString(R.string.tts_status_ready)

                        is Tts.TtsEvent.Error ->
                            binding.ttsStatusText.text = getString(R.string.tts_status_error, event.message)

                        is Tts.TtsEvent.TemporarilyUnavailable ->
                            binding.ttsStatusText.text = event.message

                        is Tts.TtsEvent.Status ->
                            binding.ttsStatusText.text = event.message

                        is Tts.TtsEvent.DownloadStarted ->
                            binding.ttsStatusText.text = getString(R.string.tts_status_download)

                        is Tts.TtsEvent.DownloadProgress ->
                            binding.ttsStatusText.text = getString(
                                R.string.tts_status_download_progress,
                                event.current,
                                event.total
                            )

                        is Tts.TtsEvent.DownloadCompleted ->
                            binding.ttsStatusText.text = getString(R.string.tts_status_ready)
                    }
                }
            }
        }
    }

    private suspend fun updateCacheInfo() {
        val info = tts.getCacheInfo()
        binding.cacheInfoText.text = getString(
            R.string.settings_cache_info,
            info.fileCount,
            info.sizeMb,
            info.sizeLimitMb
        )
        updateCacheLimitLabel(info.sizeLimitMb)
    }

    private fun updateCacheControlsEnabled(enabled: Boolean) {
        binding.cacheLimitSlider.isEnabled = enabled
        binding.buttonClearCache.isEnabled = enabled
        binding.buttonRefreshCache.isEnabled = enabled
    }

    private fun updateCacheLimitLabel(limitMb: Double) {
        binding.cacheLimitValue.text = getString(
            R.string.settings_cache_limit_value,
            limitMb.toInt()
        )
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_delete_account_title)
            .setMessage(R.string.settings_delete_account_message)
            .setPositiveButton(R.string.settings_delete_account_confirm) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteAccount() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            showSnackbar(getString(R.string.auth_error_generic))
            return
        }

        binding.buttonDeleteAccount.isEnabled = false
        binding.deleteAccountProgress.isVisible = true

        lifecycleScope.launch {
            try {
                val database = Firebase.database.reference.child("users/$userId")
                database.removeValue().await()

                Firebase.auth.currentUser?.delete()?.await()

                val intent = Intent(this@SettingsActivity, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                binding.buttonDeleteAccount.isEnabled = true
                binding.deleteAccountProgress.isVisible = false
                showSnackbar(e.localizedMessage ?: getString(R.string.auth_error_generic))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eventsJob?.cancel()
    }

    private data class VoiceItem(val id: String, val label: String)

    private fun applyWindowInsets() {
        val root = binding.root
        val initialPaddingLeft = root.paddingLeft
        val initialPaddingTop = root.paddingTop
        val initialPaddingRight = root.paddingRight
        val initialPaddingBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
