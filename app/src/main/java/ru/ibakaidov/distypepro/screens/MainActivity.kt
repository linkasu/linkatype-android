package ru.ibakaidov.distypepro.screens

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Spinner
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityMainBinding
import ru.ibakaidov.distypepro.utils.Tts

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: Tts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableOfflinePersistence()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        tts = Tts(this)
        binding.inputGroup.setTts(tts)
        binding.bankGroup.setTts(tts)

        onBackPressedDispatcher.addCallback(this) {
            binding.inputGroup.back()
            binding.bankGroup.back()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val spinner = menu.findItem(R.id.chats_spinner).actionView as? Spinner
        spinner?.let(binding.inputGroup::setChatSpinner)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.clear_menu_item -> {
            binding.inputGroup.clear()
            true
        }

        R.id.settings_menu_item -> {
            val intent = Intent().apply {
                action = "com.android.settings.TTS_SETTINGS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.shutdown()
        }
    }

    private fun enableOfflinePersistence() {
        try {
            Firebase.database.setPersistenceEnabled(true)
        } catch (e: DatabaseException) {
            // Firebase throws if persistence was already enabled; ignore to keep idempotent.
        }
    }
}
