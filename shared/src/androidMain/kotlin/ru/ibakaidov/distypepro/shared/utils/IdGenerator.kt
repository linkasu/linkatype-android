package ru.ibakaidov.distypepro.shared.utils

import java.util.UUID

actual fun generateId(): String = UUID.randomUUID().toString()
