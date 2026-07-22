package ru.ibakaidov.distypepro.telemetry

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import ru.ibakaidov.distypepro.BuildConfig
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryConsent
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryConsentAction
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryConsentMachine
import ru.ibakaidov.distypepro.shared.telemetry.TelemetryOutcome
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class TelemetryService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val store = TelemetryStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val appSessionId = UUID.randomUUID().toString()
    private val lock = Any()

    @Volatile
    private var activeCall: Call? = null
    private var flushScheduled = false
    private var denialScheduled = false
    private var flushRetryAttempt = 0
    private var denialRetryAttempt = 0

    fun start() {
        if (store.pendingDenial() != null) scheduleDenial(0)
        when (store.consent()) {
            TelemetryConsent.GRANTED -> scheduleFlush(0)
            TelemetryConsent.DENIED -> scheduleDenial(0)
            TelemetryConsent.UNKNOWN -> Unit
        }
    }

    fun consent(): TelemetryConsent = store.consent()

    fun setConsent(action: TelemetryConsentAction): TelemetryConsent {
        val current = store.consent()
        val next = TelemetryConsentMachine.transition(current, action)
        if (next == current) return next

        when (next) {
            TelemetryConsent.GRANTED -> {
                store.setConsent(next)
                scheduleFlush(0)
            }

            TelemetryConsent.DENIED -> {
                activeCall?.cancel()
                store.identity()?.let { identity ->
                    store.savePendingDenial(TelemetryStore.PendingDenial(identity.refreshToken, identity.policyVersion))
                }
                store.clearIdentity()
                store.clearQueue()
                store.setConsent(next)
                scheduleDenial(0)
            }

            TelemetryConsent.UNKNOWN -> Unit
        }
        return next
    }

    fun report(outcome: TelemetryOutcome) {
        if (!store.consent().permitsCollection) return
        if (store.enqueue(outcome)) scheduleFlush(0)
    }

    fun close() {
        activeCall?.cancel()
        scope.cancel()
    }

    private fun scheduleFlush(delayMillis: Long) {
        synchronized(lock) {
            if (flushScheduled || !store.consent().permitsCollection) return
            flushScheduled = true
        }
        scope.launch {
            delay(delayMillis)
            synchronized(lock) { flushScheduled = false }
            flush()
        }
    }

    private fun scheduleDenial(delayMillis: Long) {
        synchronized(lock) {
            if (denialScheduled || store.pendingDenial() == null) return
            denialScheduled = true
        }
        scope.launch {
            delay(delayMillis)
            synchronized(lock) { denialScheduled = false }
            sendPendingDenial()
        }
    }

    private fun flush() {
        if (!store.consent().permitsCollection) return
        val identity = ensureIdentity() ?: return retryFlush()
        if (!store.consent().permitsCollection) return
        val batch = store.takeBatch() ?: return
        val request = Request.Builder()
            .url("$METRICS_ENDPOINT/v2/batches")
            .header("Authorization", "Bearer ${identity.accessToken}")
            .header("Idempotency-Key", batch.id)
            .post(batchJson(identity.subjectKey, batch).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        when (execute(request).status) {
            202 -> {
                if (store.consent().permitsCollection) store.acknowledge(batch)
                flushRetryAttempt = 0
                scheduleFlush(0)
            }

            401 -> {
                store.saveIdentity(identity.copy(accessToken = null, accessExpiresAtMillis = 0L))
                flushRetryAttempt = 0
                scheduleFlush(0)
            }

            403 -> {
                store.clearQueue()
                store.clearIdentity()
                store.setConsent(TelemetryConsent.DENIED)
            }

            else -> retryFlush()
        }
    }

    private fun ensureIdentity(): TelemetryStore.StoredIdentity? {
        if (!store.consent().permitsCollection) return null
        val identity = store.identity()
        if (identity != null && !identity.accessToken.isNullOrBlank() && identity.accessExpiresAtMillis > System.currentTimeMillis() + ACCESS_REFRESH_SKEW_MS) {
            return identity
        }
        if (identity != null) {
            val refreshed = refreshAccess(identity)
            if (refreshed != null) return refreshed
            return null
        }
        return registerInstallation()
    }

    private fun registerInstallation(): TelemetryStore.StoredIdentity? {
        if (!store.consent().permitsCollection) return null
        val body = JSONObject()
            .put("request_id", UUID.randomUUID().toString())
            .put("product_id", PRODUCT_ID)
            .put("platform", "android")
            .put("preference", "allowed")
            .put("policy_version", POLICY_VERSION)
            .put("recorded_at", timestamp(System.currentTimeMillis()))
        val response = execute(
            Request.Builder()
                .url("$IDENTITY_ENDPOINT/v1/public/installations")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build(),
        )
        if (response.status != 201 || !store.consent().permitsCollection) return null
        return runCatching {
            val json = JSONObject(response.body)
            val access = json.getJSONObject("metrics_token")
            TelemetryStore.StoredIdentity(
                refreshToken = json.getString("refresh_token"),
                subjectKey = json.getString("installation_key"),
                policyVersion = json.getString("policy_version"),
                accessToken = access.getString("access_token"),
                accessExpiresAtMillis = Instant.parse(access.getString("expires_at")).toEpochMilli(),
            ).also(store::saveIdentity)
        }.getOrNull()
    }

    private fun refreshAccess(identity: TelemetryStore.StoredIdentity): TelemetryStore.StoredIdentity? {
        val response = execute(
            Request.Builder()
                .url("$IDENTITY_ENDPOINT/v1/public/installations/token")
                .header("Authorization", "Bearer ${identity.refreshToken}")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build(),
        )
        if (response.status == 403) {
            store.clearQueue()
            store.clearIdentity()
            store.setConsent(TelemetryConsent.DENIED)
            return null
        }
        if (response.status == 401) {
            store.clearIdentity()
            return if (store.consent().permitsCollection) registerInstallation() else null
        }
        if (response.status != 200 || !store.consent().permitsCollection) return null
        return runCatching {
            val json = JSONObject(response.body)
            val access = json.getJSONObject("metrics_token")
            identity.copy(
                subjectKey = json.getString("installation_key"),
                accessToken = access.getString("access_token"),
                accessExpiresAtMillis = Instant.parse(access.getString("expires_at")).toEpochMilli(),
            ).also(store::saveIdentity)
        }.getOrNull()
    }

    private fun sendPendingDenial() {
        val denial = store.pendingDenial() ?: return
        val body = JSONObject()
            .put("preference", "denied")
            .put("policy_version", denial.policyVersion)
            .put("recorded_at", timestamp(System.currentTimeMillis()))
        val response = execute(
            Request.Builder()
                .url("$IDENTITY_ENDPOINT/v1/public/installations/telemetry-preference")
                .header("Authorization", "Bearer ${denial.refreshToken}")
                .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build(),
        )
        if (response.status == 200) {
            store.clearPendingDenial()
            denialRetryAttempt = 0
        } else {
            scheduleDenial(retryDelay(denialRetryAttempt++))
        }
    }

    private fun retryFlush() {
        scheduleFlush(retryDelay(flushRetryAttempt++))
    }

    private fun batchJson(subjectKey: String, batch: TelemetryStore.StoredBatch): JSONObject = JSONObject()
        .put("schema_version", 2)
        .put("batch_id", batch.id)
        .put("scope", JSONObject().put("product", PRODUCT_ID).put("subject_key", subjectKey))
        .put("stream", "outcome")
        .put("sent_at", timestamp(batch.sentAtMillis))
        .put("records", JSONArray().apply { batch.records.forEach { put(recordJson(it)) } })

    private fun recordJson(record: TelemetryStore.StoredOutcome): JSONObject = record.outcome.let { outcome ->
        JSONObject()
            .put("record_id", record.id)
            .put("occurred_at", timestamp(record.occurredAtMillis))
            .put("kind", outcome.kind.wireName)
            .put("app_session_id", appSessionId)
            .put("app", appMetadata())
            .apply {
                outcome.result?.let { put("result", it.wireName) }
                outcome.source?.let { put("source", it.wireName) }
                outcome.mode?.let { put("mode", it.wireName) }
                outcome.countBucket?.let { put("count_bucket", it.wireName) }
                outcome.durationBucket?.let { put("duration_bucket", it.wireName) }
                outcome.failureCode?.let { put("failure_code", it.wireName) }
            }
    }

    private fun appMetadata(): JSONObject = JSONObject()
        .put("version", safeMetadata(BuildConfig.VERSION_NAME))
        .put("build", BuildConfig.VERSION_CODE.toString())
        .put("platform", "android")
        .put("os_version", safeMetadata(Build.VERSION.RELEASE))
        .put("locale", safeLocale())

    private fun safeLocale(): String = when (Locale.getDefault().toLanguageTag()) {
        "ru", "ru-RU", "en", "en-US" -> Locale.getDefault().toLanguageTag()
        else -> "other"
    }

    private fun safeMetadata(value: String?): String = value?.takeIf { SAFE_METADATA.matches(it) } ?: "unknown"

    private fun timestamp(epochMillis: Long): String = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMillis))

    private fun retryDelay(attempt: Int): Long = (1_000L * (1L shl attempt.coerceAtMost(12))).coerceAtMost(MAX_RETRY_DELAY_MS)

    private fun execute(request: Request): HttpResult = runCatching {
        client.newCall(request).also { activeCall = it }.execute().use { response ->
            HttpResult(response.code, response.body?.string().orEmpty())
        }
    }.getOrDefault(HttpResult(0, "")).also { activeCall = null }

    private data class HttpResult(val status: Int, val body: String)

    companion object {
        private const val PRODUCT_ID = "linka-type"
        private const val POLICY_VERSION = "2026-07-19-v3"
        private const val IDENTITY_ENDPOINT = "https://api.identity.linka.su"
        private const val METRICS_ENDPOINT = "https://metrics.nkolinka.ru"
        private const val ACCESS_REFRESH_SKEW_MS = 30_000L
        private const val MAX_RETRY_DELAY_MS = 60 * 60 * 1_000L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val SAFE_METADATA = Regex("[A-Za-z0-9][A-Za-z0-9._:+-]{0,95}")

        @Volatile
        private var instance: TelemetryService? = null

        fun get(context: Context): TelemetryService = instance ?: synchronized(this) {
            instance ?: TelemetryService(context).also { instance = it }
        }
    }
}
