package ru.ibakaidov.distypepro.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CallbackTest {

  @Test
  fun callback_onDone_receivesResult() {
    var receivedResult: String? = null

    val callback = object : Callback<String> {
      override fun onDone(result: String) {
        receivedResult = result
      }
    }

    callback.onDone("test result")

    assertEquals("test result", receivedResult)
  }

  @Test
  fun callback_onError_defaultImplementation_doesNotCrash() {
    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {}
    }

    callback.onError(IOException("Test error"))
  }

  @Test
  fun callback_onError_canBeOverridden() {
    var receivedException: Exception? = null

    val callback = object : Callback<Int> {
      override fun onDone(result: Int) {}

      override fun onError(exception: Exception?) {
        receivedException = exception
      }
    }

    val testException = RuntimeException("Error occurred")
    callback.onError(testException)

    assertEquals(testException, receivedException)
  }

  @Test
  fun callback_onError_withNullException() {
    var errorCalled = false

    val callback = object : Callback<String> {
      override fun onDone(result: String) {}

      override fun onError(exception: Exception?) {
        errorCalled = true
      }
    }

    callback.onError(null)

    assertTrue(errorCalled)
  }

  @Test
  fun callback_lambdaStyle_works() {
    var result: Int? = null

    val callback = object : Callback<Int> {
      override fun onDone(r: Int) {
        result = r
      }
    }

    callback.onDone(42)

    assertEquals(42, result)
  }

  @Test
  fun callback_withComplexType_works() {
    var receivedMap: Map<String, String>? = null

    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {
        receivedMap = result
      }
    }

    val testMap = mapOf("key1" to "value1", "key2" to "value2")
    callback.onDone(testMap)

    assertEquals(testMap, receivedMap)
  }

  @Test
  fun callback_multipleInvocations_acceptsAll() {
    val results = mutableListOf<String>()

    val callback = object : Callback<String> {
      override fun onDone(result: String) {
        results.add(result)
      }
    }

    callback.onDone("first")
    callback.onDone("second")
    callback.onDone("third")

    assertEquals(3, results.size)
    assertEquals("first", results[0])
    assertEquals("second", results[1])
    assertEquals("third", results[2])
  }

  @Test
  fun callback_withUnit_works() {
    var called = false

    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {
        called = true
      }
    }

    callback.onDone(Unit)

    assertTrue(called)
  }

  @Test
  fun callback_onError_withDifferentExceptionTypes() {
    val exceptions = mutableListOf<Exception>()

    val callback = object : Callback<String> {
      override fun onDone(result: String) {}

      override fun onError(exception: Exception?) {
        if (exception != null) {
          exceptions.add(exception)
        }
      }
    }

    callback.onError(IOException("IO Error"))
    callback.onError(IllegalArgumentException("Illegal Arg"))
    callback.onError(RuntimeException("Runtime Error"))

    assertEquals(3, exceptions.size)
    assertTrue(exceptions[0] is IOException)
    assertTrue(exceptions[1] is IllegalArgumentException)
    assertTrue(exceptions[2] is RuntimeException)
  }

  @Test
  fun callback_separateInstances_independent() {
    var result1: String? = null
    var result2: String? = null

    val callback1 = object : Callback<String> {
      override fun onDone(result: String) {
        result1 = result
      }
    }

    val callback2 = object : Callback<String> {
      override fun onDone(result: String) {
        result2 = result
      }
    }

    callback1.onDone("first")
    callback2.onDone("second")

    assertEquals("first", result1)
    assertEquals("second", result2)
  }

  @Test
  fun callback_defaultOnError_doesNothing() {
    val callback = object : Callback<Boolean> {
      override fun onDone(result: Boolean) {}
    }

    callback.onError(Exception("This should not crash"))
  }

  @Test
  fun callback_canBeUsedInList() {
    val callbacks = mutableListOf<Callback<Int>>()
    var sum = 0

    callbacks.add(object : Callback<Int> {
      override fun onDone(result: Int) {
        sum += result
      }
    })

    callbacks.add(object : Callback<Int> {
      override fun onDone(result: Int) {
        sum += result * 2
      }
    })

    callbacks[0].onDone(10)
    callbacks[1].onDone(10)

    assertEquals(30, sum)
  }

  @Test
  fun callback_nullable_result_works() {
    var receivedResult: String? = "initial"

    val callback = object : Callback<String?> {
      override fun onDone(result: String?) {
        receivedResult = result
      }
    }

    callback.onDone(null)

    assertNull(receivedResult)
  }
}

