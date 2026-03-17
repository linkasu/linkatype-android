package ru.ibakaidov.distypepro.bank

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class BankPlacementStoreTest {

    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val store = BankPlacementStore(preferences)

    @Test
    fun get_defaultValue_returnsMainPlacement() {
        every { preferences.getString(any(), null) } returns null

        val result = store.get()

        assertEquals(BankPlacement.MAIN, result)
    }

    @Test
    fun get_invalidStoredValue_returnsMainPlacement() {
        every { preferences.getString(any(), null) } returns "unexpected"

        val result = store.get()

        assertEquals(BankPlacement.MAIN, result)
    }

    @Test
    fun set_separateActivity_persistsStorageValue() {
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        store.set(BankPlacement.SEPARATE_ACTIVITY)

        verify { editor.putString("bank_placement", BankPlacement.SEPARATE_ACTIVITY.storageValue) }
        verify { editor.apply() }
    }
}
