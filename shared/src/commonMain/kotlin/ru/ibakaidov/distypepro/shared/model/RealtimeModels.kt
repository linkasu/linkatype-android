package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChangeEvent(
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("entity_id")
    val entityId: String,
    val op: String,
    val payload: JsonElement,
    @SerialName("updated_at")
    val updatedAt: Long,
)

@Serializable
data class ChangesResponse(
    val cursor: String,
    val changes: List<ChangeEvent> = emptyList(),
)
