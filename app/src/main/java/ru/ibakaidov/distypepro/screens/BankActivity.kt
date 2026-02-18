package ru.ibakaidov.distypepro.screens

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.color.DynamicColors
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityBankBinding
import ru.ibakaidov.distypepro.utils.TtsHolder

class BankActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBankBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityBankBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        applyWindowInsets()

        WindowCompat.getInsetsController(window, binding.root)?.let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !isNightMode()
        }

        val tts = TtsHolder.get(this)
        binding.bankGroup.setTts(tts)
        binding.bankGroup.attachToolbar(binding.toolbar) { handleBackNavigation() }

        onBackPressedDispatcher.addCallback(this) {
            handleBackNavigation()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bankGroup.refresh()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun handleBackNavigation() {
        if (!binding.bankGroup.back()) {
            finish()
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
                bottom = contentInitialBottom + systemBars.bottom,
            )
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun isNightMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
