package ru.ibakaidov.distypepro.shared.utils

import platform.Foundation.NSUUID

actual fun generateId(): String = NSUUID().UUIDString()
