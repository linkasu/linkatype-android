package ru.ibakaidov.distypepro.components

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.screens.SpotlightActivity
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.ProgressState
import ru.ibakaidov.distypepro.utils.Tts

class InputGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Component(context, attrs) {

    override fun layoutId(): Int = R.layout.input_group

    private lateinit var tts: Tts
    private lateinit var ttsEditText: EditText
    private lateinit var sayButton: MaterialButton
    private lateinit var spotlightButton: MaterialButton
    private lateinit var chatSelectorButton: MaterialButton
    private var previousSlotIndex: Int = 0
    private val textCache = arrayOf("", "", "")
    private val slotLabels = intArrayOf(
        R.string.chat_slot_one,
        R.string.chat_slot_two,
        R.string.chat_slot_three
    )

    override fun initUi() {
        ttsEditText = findViewById(R.id.text_to_speech_edittext)
        sayButton = findViewById(R.id.say_button)
        spotlightButton = findViewById(R.id.spotlight_button)
        chatSelectorButton = findViewById(R.id.chat_selector_button)

        setOnClickListener {
            if (ttsEditText.hasFocus()) {
                ttsEditText.clearFocus()
            }
        }

        val viewGroup = this
        ttsEditText.setOnFocusChangeListener { _, hasFocus ->
            val params: ViewGroup.LayoutParams = viewGroup.layoutParams
            params.height = if (hasFocus) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            viewGroup.layoutParams = params
        }

        sayButton.setOnClickListener { say() }
        spotlightButton.setOnClickListener { spotlight() }
        setupChatSelector()
    }

    fun setTts(tts: Tts) {
        this.tts = tts
        tts.setOnPlayCallback(object : Callback<ProgressState> {
            override fun onDone(result: ProgressState) {
                val textRes = if (result == ProgressState.START) R.string.stop else R.string.say
                sayButton.setText(textRes)
            }
        })
    }

    private fun setupChatSelector() {
        chatSelectorButton.text = context.getString(slotLabels[previousSlotIndex])
        chatSelectorButton.setOnClickListener { showChatMenu() }
    }

    private fun showChatMenu() {
        val popup = PopupMenu(context, chatSelectorButton)
        slotLabels.forEachIndexed { index, labelRes ->
            popup.menu.add(Menu.NONE, index, index, labelRes)
        }

        popup.setOnMenuItemClickListener { item ->
            val newIndex = item.itemId
            if (newIndex in slotLabels.indices) {
                if (newIndex != previousSlotIndex) {
                    textCache[previousSlotIndex] = currentText
                    previousSlotIndex = newIndex
                    setText(textCache[newIndex])
                }
                chatSelectorButton.text = context.getString(slotLabels[newIndex])
            }
            true
        }

        popup.show()
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

    private fun spotlight() {
        SpotlightActivity.show(context, currentText)
        Firebase.analytics.logEvent("spotlight", null)
    }

    private val currentText: String
        get() = ttsEditText.text?.toString().orEmpty()

    private fun setText(text: String) {
        ttsEditText.setText(text)
    }
}
