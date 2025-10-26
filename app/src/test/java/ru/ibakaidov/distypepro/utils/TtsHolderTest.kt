package ru.ibakaidov.distypepro.utils

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class TtsHolderTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    TtsHolder.clear()
  }

  @After
  fun tearDown() {
    TtsHolder.clear()
  }

  @Test
  fun get_createsInstanceOnFirstCall() {
    val tts = TtsHolder.get(context)

    assertNotNull(tts)
  }

  @Test
  fun get_returnsSameInstanceOnSubsequentCalls() {
    val first = TtsHolder.get(context)
    val second = TtsHolder.get(context)

    assertSame(first, second)
  }

  @Test
  fun get_usesApplicationContext() {
    val activityContext = mockk<Context>(relaxed = true)
    val appContext = RuntimeEnvironment.getApplication()
    every { activityContext.applicationContext } returns appContext

    val tts = TtsHolder.get(activityContext)

    verify { activityContext.applicationContext }
    assertNotNull(tts)
  }

  @Test
  fun set_replacesInstance() {
    val original = TtsHolder.get(context)
    val replacement = Tts(context)

    TtsHolder.set(replacement)
    val retrieved = TtsHolder.get(context)

    assertSame(replacement, retrieved)
    assertTrue(original !== retrieved)
  }

  @Test
  fun clear_removesInstance() {
    TtsHolder.get(context)

    TtsHolder.clear()

    val afterClear1 = TtsHolder.get(context)
    val afterClear2 = TtsHolder.get(context)

    assertNotNull(afterClear1)
    assertSame(afterClear1, afterClear2)
  }

  @Test
  fun get_threadSafe_returnsConsistentInstance() {
    val threadCount = 10
    val latch = CountDownLatch(threadCount)
    val executor = Executors.newFixedThreadPool(threadCount)
    val instances = mutableListOf<Tts>()

    repeat(threadCount) {
      executor.submit {
        val instance = TtsHolder.get(context)
        synchronized(instances) {
          instances.add(instance)
        }
        latch.countDown()
      }
    }

    latch.await()
    executor.shutdown()

    val unique = instances.toSet()
    assertEquals(1, unique.size)
  }

  @Test
  fun get_afterClear_createsNewInstance() {
    val first = TtsHolder.get(context)
    TtsHolder.clear()
    val second = TtsHolder.get(context)

    assertNotNull(first)
    assertNotNull(second)
    assertTrue(first !== second)
  }

  @Test
  fun set_withCustomInstance_persistsAcrossCalls() {
    val custom = Tts(context)
    TtsHolder.set(custom)

    val retrieved1 = TtsHolder.get(context)
    val retrieved2 = TtsHolder.get(context)

    assertSame(custom, retrieved1)
    assertSame(custom, retrieved2)
  }

  @Test
  fun clear_multipleTimes_doesNotCrash() {
    TtsHolder.get(context)

    TtsHolder.clear()
    TtsHolder.clear()
    TtsHolder.clear()

    val instance = TtsHolder.get(context)
    assertNotNull(instance)
  }

  @Test
  fun singleton_pattern_multipleContexts_sameInstance() {
    val context1 = RuntimeEnvironment.getApplication()
    val context2 = mockk<Context>(relaxed = true)
    every { context2.applicationContext } returns context1

    val instance1 = TtsHolder.get(context1)
    val instance2 = TtsHolder.get(context2)

    assertSame(instance1, instance2)
  }
}

