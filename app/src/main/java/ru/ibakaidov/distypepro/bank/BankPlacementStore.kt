package ru.ibakaidov.distypepro.bank

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class BankPlacementStore(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    )

    fun get(): BankPlacement {
        return BankPlacement.fromStorage(preferences.getString(PREF_BANK_PLACEMENT, null))
    }

    fun set(value: BankPlacement) {
        preferences.edit()
            .putString(PREF_BANK_PLACEMENT, value.storageValue)
            .apply()
    }

    companion object {
        private const val PREF_BANK_PLACEMENT = "bank_placement"
    }
}
