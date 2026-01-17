package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val darkTheme: Boolean = false,
    val yandex: Boolean = true,
    val voiceUri: String? = null,
    val yandexVoice: String? = null,
    val volume: Double = 1.0,
    val rate: Double = 1.0,
    val pitch: Double = 1.0,
    val showPredictor: Boolean = true,
    val showSpotlightPredictor: Boolean = true,
    val showQuickes: Boolean = true,
    val showBank: Boolean = true,
    val saveOnSay: Boolean = false,
    val typeSound: Boolean = false,
    val speakLastWord: Boolean = false,
)

@Serializable
data class UserState(
    val inited: Boolean = false,
    val quickes: List<String> = emptyList(),
    val preferences: UserPreferences? = null,
)
