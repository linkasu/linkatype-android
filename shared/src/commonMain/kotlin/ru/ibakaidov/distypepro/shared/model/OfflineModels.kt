package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class OfflineQueueEntry(
    val id: Long,
    val entityType: String,
    val opType: String,
    val payload: String,
    val createdAt: Long,
    val retryCount: Long,
    val lastError: String? = null,
)
