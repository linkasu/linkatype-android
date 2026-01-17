package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.model.UserPreferences
import ru.ibakaidov.distypepro.shared.model.UserState

interface UserStateRepository {
    @Throws(Exception::class)
    suspend fun getState(): UserState

    @Throws(Exception::class)
    suspend fun updateState(
        inited: Boolean? = null,
        quickes: List<String>? = null,
        preferences: UserPreferences? = null,
    ): UserState
}
