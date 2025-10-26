package ru.ibakaidov.distypepro.components

import android.content.Context
import android.widget.GridView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.MaterialToolbar
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Tts

@RunWith(RobolectricTestRunner::class)
class BankGroupTest {

  private lateinit var context: Context
  private lateinit var bankGroup: BankGroup

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    bankGroup = BankGroup(context)
  }

  @Test
  fun bankGroup_instantiates() {
    assertNotNull(bankGroup)
  }

  @Test
  fun layoutId_returnsBankGroupLayout() {
    val layoutId = bankGroup.layoutId()

    assertEquals(R.layout.bank_group, layoutId)
  }

  @Test
  fun setTts_storesTtsInstance() {
    val tts = mockk<Tts>(relaxed = true)

    bankGroup.setTts(tts)
  }

  @Test
  fun bankGroup_hasToolbar() {
    bankGroup.onFinishInflate()

    val toolbar = bankGroup.findViewById<MaterialToolbar>(R.id.bank_toolbar)
    assertNotNull(toolbar)
  }

  @Test
  fun bankGroup_hasGridView() {
    bankGroup.onFinishInflate()

    val gridView = bankGroup.findViewById<GridView>(R.id.gridview)
    assertNotNull(gridView)
  }

  @Test
  fun back_doesNotCrash() {
    bankGroup.onFinishInflate()

    bankGroup.back()
  }
}

