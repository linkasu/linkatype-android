package ru.ibakaidov.distypepro.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.ibakaidov.distypepro.databinding.ActivitySpotlightBinding

class SpotlightActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpotlightBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotlightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding.fullscreenContent.text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
    }

    companion object {
        private const val EXTRA_TEXT = "text"

        fun show(context: Context, text: String) {
            val intent = Intent(context, SpotlightActivity::class.java).apply {
                putExtra(EXTRA_TEXT, text)
            }
            context.startActivity(intent)
        }
    }
}
