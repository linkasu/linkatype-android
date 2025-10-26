package ru.ibakaidov.distypepro.components

import android.content.Context
import android.util.AttributeSet
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.ibakaidov.distypepro.R

@RunWith(RobolectricTestRunner::class)
class ComponentTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
  }

  @Test
  fun component_inflatesLayout() {
    val component = TestComponent(context)

    assertNotNull(component)
  }

  @Test
  fun component_withAttributes_instantiates() {
    val attrs = mockk<AttributeSet>(relaxed = true)

    val component = TestComponent(context, attrs)

    assertNotNull(component)
  }

  @Test
  fun component_callsLayoutId() {
    val component = TestComponent(context)

    assertEquals(R.layout.bank_group, component.layoutIdCalled)
  }

  @Test
  fun component_callsInitUiOnFinishInflate() {
    val component = TestComponent(context)
    component.onFinishInflate()

    assertTrue(component.initUiCalled)
  }

  @Test
  fun component_extendsLinearLayout() {
    val component = TestComponent(context)

    assertTrue(component is android.widget.LinearLayout)
  }

  private class TestComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
  ) : Component(context, attrs) {

    var layoutIdCalled: Int = -1
    var initUiCalled = false

    override fun layoutId(): Int {
      layoutIdCalled = R.layout.bank_group
      return layoutIdCalled
    }

    override fun initUi() {
      initUiCalled = true
    }
  }

  @Test
  fun component_inflationOrder_correct() {
    val events = mutableListOf<String>()

    val component = object : Component(context) {
      override fun layoutId(): Int {
        events.add("layoutId")
        return R.layout.bank_group
      }

      override fun initUi() {
        events.add("initUi")
      }
    }

    component.onFinishInflate()

    assertTrue(events.contains("layoutId"))
    assertTrue(events.contains("initUi"))
    assertTrue(events.indexOf("layoutId") < events.indexOf("initUi"))
  }

  @Test
  fun component_multipleInstances_independent() {
    val component1 = TestComponent(context)
    val component2 = TestComponent(context)

    component1.onFinishInflate()

    assertTrue(component1.initUiCalled)
    assertTrue(component1 !== component2)
  }
}

