package ru.ibakaidov.distypepro.bank

enum class BankPlacement(
    val storageValue: String,
) {
    MAIN("main"),
    SEPARATE_ACTIVITY("separate_activity"),
    ;

    companion object {
        fun fromStorage(rawValue: String?): BankPlacement {
            return entries.firstOrNull { it.storageValue == rawValue } ?: MAIN
        }
    }
}
