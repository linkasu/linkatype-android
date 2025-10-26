package ru.ibakaidov.distypepro.utils

interface Callback<T> {
    fun onDone(result: T)
    fun onError(error: Throwable)
}