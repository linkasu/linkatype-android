package ru.ibakaidov.distypepro.bank

import java.util.Locale

internal actual fun platformLowercase(value: String): String =
    value.lowercase(Locale.getDefault())
