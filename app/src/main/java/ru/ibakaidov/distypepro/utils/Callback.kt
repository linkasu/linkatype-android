package ru.ibakaidov.distypepro.utils

/**
 * Lightweight callback abstraction used across legacy Firebase listeners.
 * Replaced with Kotlin-friendly interface so callers can use lambda expressions.
 */
interface Callback<T> {
    fun onDone(result: T)
    fun onError(exception: Exception? = null) {}
}
