@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ru.ibakaidov.distypepro.shared.utils

import platform.posix.time

actual fun currentTimeMillis(): Long = time(null) * 1000L
