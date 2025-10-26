package ru.ibakaidov.distypepro.components

import android.content.Context
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Tts

@RunWith(RobolectricTestRunner::class)
class InputGroupTest {

  private lateinit var context: Context
  private lateinit var inputGroup: InputGroup

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    inputGroup = InputGroup(context)
  }

  @Test
  fun inputGroup_instantiates() {
    assertNotNull(inputGroup)
  }

  @Test
  fun layoutId_returnsInputGroupLayout() {
    val layoutId = inputGroup.layoutId()

    assertEquals(R.layout.input_group, layoutId)
  }

  @Test
  fun inputGroup_hasEditText() {
    inputGroup.onFinishInflate()

    val editText = inputGroup.findViewById<EditText>(R.id.text_to_speech_edittext)
    assertNotNull(editText)
  }

  @Test
  fun inputGroup_hasSayButton() {
    inputGroup.onFinishInflate()

    val button = inputGroup.findViewById<MaterialButton>(R.id.say_button)
    assertNotNull(button)
  }

  @Test
  fun inputGroup_hasSpotlightButton() {
    inputGroup.onFinishInflate()

    val button = inputGroup.findViewById<MaterialButton>(R.id.spotlight_button)
    assertNotNull(button)
  }

  @Test
  fun inputGroup_hasChatSelectorButton() {
    inputGroup.onFinishInflate()

    val button = inputGroup.findViewById<MaterialButton>(R.id.chat_selector_button)
    assertNotNull(button)
  }

  @Test
  fun setTts_storesTtsInstance() {
    val tts = mockk<Tts>(relaxed = true)
    val eventsFlow = MutableSharedFlow<Tts.TtsEvent>()
    every { tts.events() } returns eventsFlow
    every { tts.setOnPlayCallback(any()) } returns Unit

    inputGroup.setTts(tts)

    verify { tts.setOnPlayCallback(any()) }
  }

  @Test
  fun clear_clearsEditText() {
    inputGroup.onFinishInflate()
    val editText = inputGroup.findViewById<EditText>(R.id.text_to_speech_edittext)
    editText.setText("test text")

    inputGroup.clear()

    assertEquals("", editText.text.toString())
  }

  @Test
  fun back_clearsFocus() {
    inputGroup.onFinishInflate()
    val editText = inputGroup.findViewById<EditText>(R.id.text_to_speech_edittext)
    editText.requestFocus()

    inputGroup.back()

    assertFalse(editText.hasFocus())
  }

  @Test
  fun sayButton_initiallyDisabled_whenNoText() {
    inputGroup.onFinishInflate()
    val editText = inputGroup.findViewById<EditText>(R.id.text_to_speech_edittext)
    val sayButton = inputGroup.findViewById<MaterialButton>(R.id.say_button)

    editText.setText("")

    assertFalse(sayButton.isEnabled)
  }

  @Test
  fun sayButton_enabled_whenHasText() {
    inputGroup.onFinishInflate()
    val editText = inputGroup.findViewById<EditText>(R.id.text_to_speech_edittext)
    val sayButton = inputGroup.findViewById<MaterialButton>(R.id.say_button)

    editText.setText("some text")

    assertTrue(sayButton.isEnabled)
  }
}

