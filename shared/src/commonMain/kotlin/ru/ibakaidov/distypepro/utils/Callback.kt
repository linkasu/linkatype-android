package ru.ibakaidov.distypepro.utils

/**
 * Lightweight callback abstraction used across Firebase listeners and platform code.
 */
interface Callback<T> {
    fun onDone(result: T)
    fun onError(exception: Exception? = null) {}
}
