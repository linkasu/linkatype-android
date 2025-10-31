package ru.ibakaidov.distypepro.components

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.screens.SpotlightActivity
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.ProgressState
import ru.ibakaidov.distypepro.utils.Tts
import ru.ibakaidov.distypepro.utils.Tts.TtsEvent

class InputGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Component(context, attrs) {

    override fun layoutId(): Int = R.layout.input_group

    private lateinit var tts: Tts
    private lateinit var ttsEditText: EditText
    private lateinit var sayButton: MaterialButton
    private val textCache = arrayOf("", "", "")
    private var eventsJob: Job? = null
    private var isSpeaking: Boolean = false

    override fun initUi() {
        ttsEditText = findViewById(R.id.text_to_speech_edittext)
        sayButton = findViewById(R.id.say_button)

        setOnClickListener {
            if (ttsEditText.hasFocus()) {
                ttsEditText.clearFocus()
            }
        }

        sayButton.setOnClickListener { say() }

        ttsEditText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateActionButtonsState()
            }
        })

        updateActionButtonsState()
    }

    fun setTts(tts: Tts) {
        this.tts = tts
        tts.setOnPlayCallback(object : Callback<ProgressState> {
            override fun onDone(result: ProgressState) {
                when (result) {
                    ProgressState.START -> {
                        isSpeaking = true
                        sayButton.setText(R.string.stop)
                        sayButton.isEnabled = true
                    }
                    ProgressState.STOP, ProgressState.ERROR -> {
                        isSpeaking = false
                        sayButton.setText(R.string.say)
                        updateActionButtonsState()
                    }
                }
            }
        })
        observeTtsEvents()
    }

    fun switchSlot(fromSlot: Int, toSlot: Int) {
        textCache[fromSlot] = currentText
        setText(textCache[toSlot])
    }

    fun clear() {
        ttsEditText.setText("")
    }

    fun back() {
        ttsEditText.clearFocus()
    }

    private fun say() {
        val text = currentText
        tts.speak(text)
        Firebase.analytics.logEvent("say", null)
    }


    fun spotlight() {
        SpotlightActivity.show(context, currentText)
        Firebase.analytics.logEvent("spotlight", null)
    }

    private val currentText: String
        get() = ttsEditText.text?.toString().orEmpty()

    private fun setText(text: String) {
        ttsEditText.setText(text)
        ttsEditText.setSelection(ttsEditText.text?.length ?: 0)
        updateActionButtonsState()
    }

    private fun updateActionButtonsState() {
        val hasText = currentText.isNotBlank()
        sayButton.isEnabled = isSpeaking || hasText
    }

    private fun observeTtsEvents() {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        if (lifecycleOwner == null) {
            post { observeTtsEvents() }
            return
        }
        eventsJob?.cancel()
        eventsJob = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                tts.events().collect { event -> handleTtsEvent(event) }
            }
        }
    }

    private fun handleTtsEvent(event: TtsEvent) {
        when (event) {
            TtsEvent.SpeakingStarted -> {
                isSpeaking = true
            }

            TtsEvent.SpeakingCompleted -> {
                isSpeaking = false
                updateActionButtonsState()
            }

            is TtsEvent.Error -> {
                isSpeaking = false
                Toast.makeText(context, context.getString(R.string.tts_status_error, event.message), Toast.LENGTH_LONG).show()
                updateActionButtonsState()
            }

            is TtsEvent.Status -> Unit

            TtsEvent.DownloadStarted -> Unit

            is TtsEvent.DownloadProgress -> Unit

            is TtsEvent.DownloadCompleted -> {
                isSpeaking = false
                updateActionButtonsState()
            }
        }
    }

    private open class SimpleTextWatcher : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}
