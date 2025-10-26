package ru.ibakaidov.distypepro.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TtsCacheManagerTest {

  private lateinit var context: Context
  private lateinit var cacheManager: TtsCacheManager
  private lateinit var cacheDir: File

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    cacheManager = TtsCacheManager(context)
    cacheDir = File(context.filesDir, "tts_cache")
    cacheDir.mkdirs()
  }

  @After
  fun tearDown() = runTest {
    cacheManager.clearCache()
    cacheDir.deleteRecursively()
  }

  @Test
  fun generateCacheKey_producesMd5Hash() {
    val key1 = cacheManager.generateCacheKey("test", "voice1")
    val key2 = cacheManager.generateCacheKey("test", "voice1")
    val key3 = cacheManager.generateCacheKey("test", "voice2")

    assertEquals(32, key1.length)
    assertEquals(key1, key2)
    assertTrue(key1 != key3)
  }

  @Test
  fun generateCacheKey_differentTextDifferentKey() {
    val key1 = cacheManager.generateCacheKey("hello", "voice")
    val key2 = cacheManager.generateCacheKey("world", "voice")

    assertTrue(key1 != key2)
  }

  @Test
  fun generateCacheKey_differentVoiceDifferentKey() {
    val key1 = cacheManager.generateCacheKey("text", "voice1")
    val key2 = cacheManager.generateCacheKey("text", "voice2")

    assertTrue(key1 != key2)
  }

  @Test
  fun getCachedFile_returnsNullWhenNotCached() = runTest {
    val result = cacheManager.getCachedFile("nonexistent")

    assertNull(result)
  }

  @Test
  fun getCachedFile_returnsFileWhenExists() = runTest {
    val key = "testkey123"
    val file = File(cacheDir, "$key.mp3")
    file.writeText("test content")

    val result = cacheManager.getCachedFile(key)

    assertNotNull(result)
    assertEquals(file.absolutePath, result?.absolutePath)
  }

  @Test
  fun isCached_returnsFalseWhenNotCached() = runTest {
    val result = cacheManager.isCached("nonexistent")

    assertFalse(result)
  }

  @Test
  fun isCached_returnsTrueWhenExists() = runTest {
    val key = "existingkey"
    val file = File(cacheDir, "$key.mp3")
    file.writeText("cached data")

    val result = cacheManager.isCached(key)

    assertTrue(result)
  }

  @Test
  fun saveToCache_createsFile() = runTest {
    val key = "savetest"
    val data = "audio data".toByteArray()

    val result = cacheManager.saveToCache(key, data)

    assertNotNull(result)
    assertTrue(result!!.exists())
    assertEquals(data.size.toLong(), result.length())
  }

  @Test
  fun saveToCache_whenDisabled_returnsNull() = runTest {
    cacheManager.setCacheEnabled(false)
    val key = "disabledtest"
    val data = "data".toByteArray()

    val result = cacheManager.saveToCache(key, data)

    assertNull(result)
  }

  @Test
  fun getCacheInfo_returnsCorrectInfo() = runTest {
    cacheManager.setCacheEnabled(true)
    cacheManager.setCacheSizeLimitMb(100.0)

    val file1 = File(cacheDir, "file1.mp3")
    val file2 = File(cacheDir, "file2.mp3")
    file1.writeBytes(ByteArray(1024 * 100))
    file2.writeBytes(ByteArray(1024 * 200))

    val info = cacheManager.getCacheInfo()

    assertTrue(info.enabled)
    assertEquals(100.0, info.sizeLimitMb, 0.1)
    assertEquals(2, info.fileCount)
    assertTrue(info.sizeMb > 0.0)
  }

  @Test
  fun getCacheInfo_calculatesUsagePercentage() = runTest {
    cacheManager.setCacheSizeLimitMb(1.0)
    val file = File(cacheDir, "testfile.mp3")
    file.writeBytes(ByteArray(512 * 1024))

    val info = cacheManager.getCacheInfo()

    assertTrue(info.usagePercentage > 40.0)
    assertTrue(info.usagePercentage < 60.0)
  }

  @Test
  fun getCacheInfo_isNearLimit_detectsHighUsage() = runTest {
    cacheManager.setCacheSizeLimitMb(1.0)
    val file = File(cacheDir, "largefile.mp3")
    file.writeBytes(ByteArray(950 * 1024))

    val info = cacheManager.getCacheInfo()

    assertTrue(info.isNearLimit)
  }

  @Test
  fun clearCache_removesAllFiles() = runTest {
    val file1 = File(cacheDir, "file1.mp3")
    val file2 = File(cacheDir, "file2.mp3")
    file1.writeText("data1")
    file2.writeText("data2")

    cacheManager.clearCache()

    val info = cacheManager.getCacheInfo()
    assertEquals(0, info.fileCount)
    assertEquals(0.0, info.sizeMb, 0.01)
  }

  @Test
  fun getCacheEnabled_defaultsToTrue() {
    val result = cacheManager.getCacheEnabled()

    assertTrue(result)
  }

  @Test
  fun setCacheEnabled_storesValue() {
    cacheManager.setCacheEnabled(false)

    val result = cacheManager.getCacheEnabled()

    assertFalse(result)
  }

  @Test
  fun getCacheSizeLimitMb_defaultsTo2048() {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    prefs.edit().clear().apply()

    val newManager = TtsCacheManager(context)
    val result = newManager.getCacheSizeLimitMb()

    assertEquals(2048.0, result, 0.1)
  }

  @Test
  fun setCacheSizeLimitMb_storesValue() {
    cacheManager.setCacheSizeLimitMb(500.0)

    val result = cacheManager.getCacheSizeLimitMb()

    assertEquals(500.0, result, 0.1)
  }

  @Test
  fun setCacheSizeLimitMb_normalizesNegativeToZero() {
    cacheManager.setCacheSizeLimitMb(-100.0)

    val result = cacheManager.getCacheSizeLimitMb()

    assertEquals(0.0, result, 0.1)
  }

  @Test
  fun saveToCache_respectsLimit_deletesOldFiles() = runTest {
    cacheManager.setCacheSizeLimitMb(0.5)

    val key1 = "old"
    val key2 = "new"
    val largeData = ByteArray(300 * 1024)

    cacheManager.saveToCache(key1, largeData)
    Thread.sleep(100)
    cacheManager.saveToCache(key2, largeData)

    val info = cacheManager.getCacheInfo()
    assertTrue(info.sizeMb < 0.6)
  }

  @Test
  fun cacheInfo_usagePercentage_handlesZeroLimit() {
    cacheManager.setCacheSizeLimitMb(0.0)

    val info = TtsCacheManager.TtsCacheInfo(
      enabled = true,
      sizeMb = 10.0,
      sizeLimitMb = 0.0,
      fileCount = 5
    )

    assertEquals(0.0, info.usagePercentage, 0.01)
  }

  @Test
  fun cacheInfo_isNearLimit_falseWhenBelowThreshold() {
    val info = TtsCacheManager.TtsCacheInfo(
      enabled = true,
      sizeMb = 50.0,
      sizeLimitMb = 100.0,
      fileCount = 10
    )

    assertFalse(info.isNearLimit)
  }

  @Test
  fun saveToCache_writesCorrectContent() = runTest {
    val key = "contenttest"
    val expectedData = "expected audio content".toByteArray()

    val file = cacheManager.saveToCache(key, expectedData)

    assertNotNull(file)
    val actualData = file!!.readBytes()
    assertTrue(expectedData.contentEquals(actualData))
  }

  @Test
  fun getCachedFile_withDifferentExtension_returnsNull() = runTest {
    val key = "wrongext"
    val file = File(cacheDir, "$key.wav")
    file.writeText("wrong extension")

    val result = cacheManager.getCachedFile(key)

    assertNull(result)
  }
}

