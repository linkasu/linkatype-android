package ru.ibakaidov.distypepro.telemetry

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryConsent
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryCountBucket
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryDurationBucket
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryFailureCode
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryMode
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryOutcome
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryOutcomeKind
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryResult
import ru.ibakaidov.distypepro.shared.telemetry.TelemetrySource
import java.util.UUID

internal class TelemetryStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    @Synchronized
    fun consent(): TelemetryConsent = TelemetryConsent.fromStorage(preferences.getString(KEY_CONSENT, null))

    @Synchronized
    fun setConsent(consent: TelemetryConsent) {
        preferences.edit().putString(KEY_CONSENT, consent.name).commit()
    }

    @Synchronized
    fun enqueue(outcome: TelemetryOutcome): Boolean {
        val queued = loadQueue()
        if (queued.size >= MAX_QUEUED_OUTCOMES) return false
        queued += StoredOutcome(UUID.randomUUID().toString(), System.currentTimeMillis(), outcome)
        return saveQueue(queued)
    }

    @Synchronized
    fun takeBatch(): StoredBatch? {
        val queued = loadQueue()
        if (queued.isEmpty()) return null
        loadBatch()?.let { batch ->
            val byId = queued.associateBy { it.id }
            val records = batch.recordIds.mapNotNull(byId::get)
            if (records.size == batch.recordIds.size) return StoredBatch(batch.id, batch.sentAtMillis, records)
            clearBatch()
        }

        val records = queued.take(MAX_BATCH_OUTCOMES)
        val batch = BatchState(UUID.randomUUID().toString(), System.currentTimeMillis(), records.map { it.id })
        if (!preferences.edit().putString(KEY_BATCH, batch.toJson().toString()).commit()) return null
        return StoredBatch(batch.id, batch.sentAtMillis, records)
    }

    @Synchronized
    fun acknowledge(batch: StoredBatch) {
        val active = loadBatch() ?: return
        if (active.id != batch.id) return
        val acknowledged = active.recordIds.toSet()
        saveQueue(loadQueue().filterNot { it.id in acknowledged })
        clearBatch()
    }

    @Synchronized
    fun clearQueue() {
        preferences.edit().remove(KEY_QUEUE).remove(KEY_BATCH).commit()
    }

    @Synchronized
    fun identity(): StoredIdentity? = preferences.getString(KEY_IDENTITY, null)?.let(::parseIdentity)

    @Synchronized
    fun saveIdentity(identity: StoredIdentity) {
        preferences.edit().putString(KEY_IDENTITY, identity.toJson().toString()).commit()
    }

    @Synchronized
    fun clearIdentity() {
        preferences.edit().remove(KEY_IDENTITY).commit()
    }

    @Synchronized
    fun pendingDenial(): PendingDenial? = preferences.getString(KEY_PENDING_DENIAL, null)?.let(::parsePendingDenial)

    @Synchronized
    fun savePendingDenial(denial: PendingDenial) {
        preferences.edit().putString(KEY_PENDING_DENIAL, denial.toJson().toString()).commit()
    }

    @Synchronized
    fun clearPendingDenial() {
        preferences.edit().remove(KEY_PENDING_DENIAL).commit()
    }

    private fun loadQueue(): MutableList<StoredOutcome> = runCatching {
        val raw = preferences.getString(KEY_QUEUE, null) ?: return mutableListOf()
        JSONArray(raw).let { array -> MutableList(array.length()) { index -> parseOutcome(array.getJSONObject(index)) } }
    }.getOrElse { mutableListOf() }

    private fun saveQueue(outcomes: List<StoredOutcome>): Boolean = runCatching {
        val value = JSONArray().apply { outcomes.forEach { put(it.toJson()) } }
        preferences.edit().putString(KEY_QUEUE, value.toString()).commit()
    }.getOrDefault(false)

    private fun loadBatch(): BatchState? = preferences.getString(KEY_BATCH, null)?.let {
        runCatching { BatchState.fromJson(JSONObject(it)) }.getOrNull()
    }

    private fun clearBatch() {
        preferences.edit().remove(KEY_BATCH).commit()
    }

    private fun parseOutcome(json: JSONObject): StoredOutcome = StoredOutcome(
        id = json.getString("id"),
        occurredAtMillis = json.getLong("occurred_at"),
        outcome = TelemetryOutcome(
            kind = enumFromWire(json.getString("kind"), TelemetryOutcomeKind.entries) { it.wireName },
            result = json.optStringOrNull("result")?.let { enumFromWire(it, TelemetryResult.entries) { value -> value.wireName } },
            source = json.optStringOrNull("source")?.let { enumFromWire(it, TelemetrySource.entries) { value -> value.wireName } },
            mode = json.optStringOrNull("mode")?.let { enumFromWire(it, TelemetryMode.entries) { value -> value.wireName } },
            countBucket = json.optStringOrNull("count_bucket")?.let { enumFromWire(it, TelemetryCountBucket.entries) { value -> value.wireName } },
            durationBucket = json.optStringOrNull("duration_bucket")?.let { enumFromWire(it, TelemetryDurationBucket.entries) { value -> value.wireName } },
            failureCode = json.optStringOrNull("failure_code")?.let { enumFromWire(it, TelemetryFailureCode.entries) { value -> value.wireName } },
        ),
    )

    private fun parseIdentity(raw: String): StoredIdentity? = runCatching {
        val json = JSONObject(raw)
        StoredIdentity(
            refreshToken = json.getString("refresh_token"),
            subjectKey = json.getString("subject_key"),
            policyVersion = json.getString("policy_version"),
            accessToken = json.optStringOrNull("access_token"),
            accessExpiresAtMillis = json.optLong("access_expires_at", 0L),
        )
    }.getOrNull()

    private fun parsePendingDenial(raw: String): PendingDenial? = runCatching {
        val json = JSONObject(raw)
        PendingDenial(json.getString("refresh_token"), json.getString("policy_version"))
    }.getOrNull()

    private fun <T> enumFromWire(value: String, values: List<T>, wireName: (T) -> String): T {
        return values.firstOrNull { wireName(it) == value } ?: throw IllegalArgumentException("Unknown telemetry enum")
    }

    private fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) getString(name) else null

    internal data class StoredOutcome(
        val id: String,
        val occurredAtMillis: Long,
        val outcome: TelemetryOutcome,
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("occurred_at", occurredAtMillis)
            put("kind", outcome.kind.wireName)
            outcome.result?.let { put("result", it.wireName) }
            outcome.source?.let { put("source", it.wireName) }
            outcome.mode?.let { put("mode", it.wireName) }
            outcome.countBucket?.let { put("count_bucket", it.wireName) }
            outcome.durationBucket?.let { put("duration_bucket", it.wireName) }
            outcome.failureCode?.let { put("failure_code", it.wireName) }
        }
    }

    internal data class StoredBatch(val id: String, val sentAtMillis: Long, val records: List<StoredOutcome>)

    internal data class StoredIdentity(
        val refreshToken: String,
        val subjectKey: String,
        val policyVersion: String,
        val accessToken: String?,
        val accessExpiresAtMillis: Long,
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("refresh_token", refreshToken)
            put("subject_key", subjectKey)
            put("policy_version", policyVersion)
            accessToken?.let { put("access_token", it) }
            put("access_expires_at", accessExpiresAtMillis)
        }
    }

    internal data class PendingDenial(val refreshToken: String, val policyVersion: String) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("refresh_token", refreshToken)
            put("policy_version", policyVersion)
        }
    }

    private data class BatchState(val id: String, val sentAtMillis: Long, val recordIds: List<String>) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("sent_at", sentAtMillis)
            put("record_ids", JSONArray(recordIds))
        }

        companion object {
            fun fromJson(json: JSONObject): BatchState = BatchState(
                id = json.getString("id"),
                sentAtMillis = json.getLong("sent_at"),
                recordIds = json.getJSONArray("record_ids").let { array -> List(array.length()) { array.getString(it) } },
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "linka_type_telemetry_v2"
        const val KEY_CONSENT = "consent"
        const val KEY_QUEUE = "queue"
        const val KEY_BATCH = "batch"
        const val KEY_IDENTITY = "identity"
        const val KEY_PENDING_DENIAL = "pending_denial"
        const val MAX_QUEUED_OUTCOMES = 200
        const val MAX_BATCH_OUTCOMES = 100
    }
}
