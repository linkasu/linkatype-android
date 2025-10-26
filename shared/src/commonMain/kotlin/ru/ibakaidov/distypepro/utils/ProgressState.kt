package ru.ibakaidov.distypepro.utils

sealed class ProgressState {
    object Idle : ProgressState()
    object Loading : ProgressState()
    data class Success<T>(val data: T) : ProgressState()
    data class Error(val throwable: Throwable) : ProgressState()
}