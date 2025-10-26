package ru.ibakaidov.distypepro.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.AdapterView
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
    private lateinit var sayButton: Button
    private lateinit var spotlightButton: ImageButton
    private var previousSpinnerIndex: Int = 0
    private val textCache = arrayOf("", "", "")

    override fun initUi() {
        ttsEditText = findViewById(R.id.text_to_speech_edittext)
        sayButton = findViewById(R.id.say_button)
        spotlightButton = findViewById(R.id.spotlight_button)

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

    fun setChatSpinner(spinner: Spinner) {
        spinner.adapter = ArrayAdapter(
            context,
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            listOf("1", "2", "3")
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                textCache[previousSpinnerIndex] = currentText
                setText(textCache[position])
                previousSpinnerIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
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
