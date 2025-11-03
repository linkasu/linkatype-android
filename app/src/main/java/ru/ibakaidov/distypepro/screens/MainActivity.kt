package ru.ibakaidov.distypepro.screens

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityMainBinding
import ru.ibakaidov.distypepro.utils.Tts
import ru.ibakaidov.distypepro.utils.TtsHolder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: Tts
    private var currentSlotIndex: Int = 0
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
        enableOfflinePersistence()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        binding.toolbar.subtitle = ""
        applyWindowInsets()

        WindowCompat.getInsetsController(window, binding.root)?.let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = true
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
        R.id.logout_menu_item -> {
            Firebase.auth.signOut()
            val intent = Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
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

    private fun enableOfflinePersistence() {
        try {
            Firebase.database.setPersistenceEnabled(true)
        } catch (e: DatabaseException) {
            // Firebase throws if persistence was already enabled; ignore to keep idempotent.
        }
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
}
