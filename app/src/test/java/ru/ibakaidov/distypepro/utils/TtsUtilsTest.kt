package ru.ibakaidov.distypepro.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsUtilsTest {

  @Test
  fun yandexVoice_toTtsVoice_convertsCorrectly() {
    val yandexVoice = Tts.YandexVoice("zahar", "Захар")

    val ttsVoice = with(Tts.Companion) { yandexVoice.toTtsVoice() }

    assertEquals("zahar", ttsVoice.voiceId)
    assertEquals("Захар", ttsVoice.title)
    assertEquals(Tts.VoiceProvider.YANDEX, ttsVoice.provider)
    assertTrue(ttsVoice.isDefault)
  }

  @Test
  fun yandexVoice_toTtsVoice_nonDefaultVoice() {
    val yandexVoice = Tts.YandexVoice("alena", "Алёна")

    val ttsVoice = with(Tts.Companion) { yandexVoice.toTtsVoice() }

    assertEquals("alena", ttsVoice.voiceId)
    assertEquals("Алёна", ttsVoice.title)
    assertEquals(Tts.VoiceProvider.YANDEX, ttsVoice.provider)
    assertFalse(ttsVoice.isDefault)
  }

  @Test
  fun ttsVoice_equality_worksCorrectly() {
    val voice1 = Tts.TtsVoice("id1", "Title1", "ru-RU", Tts.VoiceProvider.YANDEX, true)
    val voice2 = Tts.TtsVoice("id1", "Title1", "ru-RU", Tts.VoiceProvider.YANDEX, true)
    val voice3 = Tts.TtsVoice("id2", "Title1", "ru-RU", Tts.VoiceProvider.YANDEX, true)

    assertEquals(voice1, voice2)
    assertTrue(voice1 != voice3)
  }

  @Test
  fun ttsVoice_copy_worksCorrectly() {
    val original = Tts.TtsVoice("id1", "Title", "en-US", Tts.VoiceProvider.OFFLINE, false)

    val modified = original.copy(title = "New Title")

    assertEquals("id1", modified.voiceId)
    assertEquals("New Title", modified.title)
    assertEquals("en-US", modified.locale)
    assertEquals(Tts.VoiceProvider.OFFLINE, modified.provider)
    assertFalse(modified.isDefault)
  }

  @Test
  fun yandexVoice_equality_worksCorrectly() {
    val voice1 = Tts.YandexVoice("zahar", "Захар")
    val voice2 = Tts.YandexVoice("zahar", "Захар")
    val voice3 = Tts.YandexVoice("ermil", "Ермил")

    assertEquals(voice1, voice2)
    assertTrue(voice1 != voice3)
  }

  @Test
  fun ttsEvent_sealedClass_equality() {
    val event1 = Tts.TtsEvent.SpeakingStarted
    val event2 = Tts.TtsEvent.SpeakingStarted
    val event3 = Tts.TtsEvent.SpeakingCompleted

    assertEquals(event1, event2)
    assertTrue(event1 != event3)
  }

  @Test
  fun ttsEvent_error_containsMessage() {
    val errorEvent = Tts.TtsEvent.Error("Test error message")

    assertEquals("Test error message", errorEvent.message)
  }

  @Test
  fun ttsEvent_status_containsMessage() {
    val statusEvent = Tts.TtsEvent.Status("Processing...")

    assertEquals("Processing...", statusEvent.message)
  }

  @Test
  fun ttsEvent_downloadProgress_containsValues() {
    val progressEvent = Tts.TtsEvent.DownloadProgress(5, 10)

    assertEquals(5, progressEvent.current)
    assertEquals(10, progressEvent.total)
  }

  @Test
  fun ttsEvent_downloadCompleted_containsPath() {
    val completedEvent = Tts.TtsEvent.DownloadCompleted("/path/to/file.mp3")

    assertEquals("/path/to/file.mp3", completedEvent.path)
  }

  @Test
  fun ttsEvent_downloadCompleted_withNullPath() {
    val completedEvent = Tts.TtsEvent.DownloadCompleted(null)

    assertEquals(null, completedEvent.path)
  }

  @Test
  fun voiceProvider_hasCorrectValues() {
    val yandex = Tts.VoiceProvider.YANDEX
    val offline = Tts.VoiceProvider.OFFLINE

    assertEquals("YANDEX", yandex.name)
    assertEquals("OFFLINE", offline.name)
  }

  @Test
  fun progressState_hasAllValues() {
    val start = ProgressState.START
    val stop = ProgressState.STOP
    val error = ProgressState.ERROR

    assertEquals("START", start.name)
    assertEquals("STOP", stop.name)
    assertEquals("ERROR", error.name)
  }

  @Test
  fun progressState_enumValues_returnsAll() {
    val values = ProgressState.values()

    assertEquals(3, values.size)
    assertTrue(values.contains(ProgressState.START))
    assertTrue(values.contains(ProgressState.STOP))
    assertTrue(values.contains(ProgressState.ERROR))
  }

  @Test
  fun progressState_valueOf_returnsCorrect() {
    val start = ProgressState.valueOf("START")
    val stop = ProgressState.valueOf("STOP")
    val error = ProgressState.valueOf("ERROR")

    assertEquals(ProgressState.START, start)
    assertEquals(ProgressState.STOP, stop)
    assertEquals(ProgressState.ERROR, error)
  }
}

