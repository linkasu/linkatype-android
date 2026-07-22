package ru.ibakaidov.distypepro.shared.telemetry

enum class TelemetryConsent {
    UNKNOWN,
    GRANTED,
    DENIED,
    ;

    val permitsCollection: Boolean
        get() = this == GRANTED

    companion object {
        fun fromStorage(value: String?): TelemetryConsent = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class TelemetryConsentAction {
    GRANT,
    DENY,
}

object TelemetryConsentMachine {
    fun transition(current: TelemetryConsent, action: TelemetryConsentAction): TelemetryConsent = when (action) {
        TelemetryConsentAction.GRANT -> TelemetryConsent.GRANTED
        TelemetryConsentAction.DENY -> TelemetryConsent.DENIED
    }
}

enum class TelemetryOutcomeKind(val wireName: String) {
    PHRASE_COMPOSED("phrase_composed"),
    SPEECH_COMPLETED("speech_completed"),
    BANK_ACTION_COMPLETED("bank_action_completed"),
    DIALOG_ACTION_COMPLETED("dialog_action_completed"),
    SYNC_COMPLETED("sync_completed"),
}

enum class TelemetryResult(val wireName: String) {
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
}

enum class TelemetrySource(val wireName: String) {
    INPUT("input"),
    QUICK("quick"),
    BANK("bank"),
    DIALOG("dialog"),
    PHRASE_INSERTED("phrase_inserted"),
    PHRASE_SPOKEN("phrase_spoken"),
    READER_OPENED("reader_opened"),
    MESSAGE_SENT("message_sent"),
    SUGGESTION_ACCEPTED("suggestion_accepted"),
    SUGGESTION_DISMISSED("suggestion_dismissed"),
}

enum class TelemetryMode(val wireName: String) {
    LOCAL("local"),
    CLOUD("cloud"),
}

enum class TelemetryCountBucket(val wireName: String) {
    ONE("one"),
    TWO_TO_FIVE("two_to_five"),
    SIX_TO_TWENTY("six_to_twenty"),
    MORE_THAN_TWENTY("more_than_twenty"),
    ;

    companion object {
        fun fromCount(count: Int): TelemetryCountBucket = when {
            count <= 1 -> ONE
            count <= 5 -> TWO_TO_FIVE
            count <= 20 -> SIX_TO_TWENTY
            else -> MORE_THAN_TWENTY
        }
    }
}

enum class TelemetryDurationBucket(val wireName: String) {
    UNDER_5S("under_5s"),
    FIVE_TO_30S("5s_to_30s"),
    THIRTY_ONE_TO_2M("31s_to_2m"),
    OVER_2M("over_2m"),
    ;

    companion object {
        fun fromDurationMillis(durationMillis: Long): TelemetryDurationBucket = when {
            durationMillis < 5_000L -> UNDER_5S
            durationMillis <= 30_000L -> FIVE_TO_30S
            durationMillis <= 120_000L -> THIRTY_ONE_TO_2M
            else -> OVER_2M
        }
    }
}

enum class TelemetryFailureCode(val wireName: String) {
    ENGINE_UNAVAILABLE("engine_unavailable"),
    REQUEST_FAILED("request_failed"),
    TIMEOUT("timeout"),
    CANCELLED("cancelled"),
    VALIDATION_FAILED("validation_failed"),
    STORAGE_FAILED("storage_failed"),
    PERMISSION_DENIED("permission_denied"),
    NETWORK_UNAVAILABLE("network_unavailable"),
    CONFLICT("conflict"),
    SERVER_ERROR("server_error"),
}

data class TelemetryOutcome(
    val kind: TelemetryOutcomeKind,
    val result: TelemetryResult? = null,
    val source: TelemetrySource? = null,
    val mode: TelemetryMode? = null,
    val countBucket: TelemetryCountBucket? = null,
    val durationBucket: TelemetryDurationBucket? = null,
    val failureCode: TelemetryFailureCode? = null,
) {
    init {
        val valid = when (kind) {
            TelemetryOutcomeKind.PHRASE_COMPOSED -> result == null && source in PHRASE_SOURCES && mode == null &&
                countBucket != null && durationBucket == null && failureCode == null

            TelemetryOutcomeKind.SPEECH_COMPLETED -> result in SPEECH_RESULTS && source in PHRASE_SOURCES &&
                mode != null && countBucket != null && durationBucket != null &&
                (failureCode == null || failureCode in SPEECH_FAILURES)

            TelemetryOutcomeKind.BANK_ACTION_COMPLETED -> result in ACTION_RESULTS && source in BANK_SOURCES &&
                mode == null && countBucket == null && durationBucket == null &&
                (failureCode == null || failureCode in ACTION_FAILURES)

            TelemetryOutcomeKind.DIALOG_ACTION_COMPLETED -> result in ACTION_RESULTS && source in DIALOG_SOURCES &&
                mode == null && countBucket == null && durationBucket == null &&
                (failureCode == null || failureCode in ACTION_FAILURES)

            TelemetryOutcomeKind.SYNC_COMPLETED -> result in ACTION_RESULTS && source == null && mode == null &&
                countBucket != null && durationBucket == null &&
                (failureCode == null || failureCode in SYNC_FAILURES)
        }
        require(valid) { "Telemetry outcome is outside the LINKa Type V2 allowlist" }
    }

    companion object {
        private val PHRASE_SOURCES = setOf(TelemetrySource.INPUT, TelemetrySource.QUICK, TelemetrySource.BANK, TelemetrySource.DIALOG)
        private val SPEECH_RESULTS = setOf(TelemetryResult.COMPLETED, TelemetryResult.FAILED, TelemetryResult.CANCELLED)
        private val ACTION_RESULTS = setOf(TelemetryResult.COMPLETED, TelemetryResult.FAILED)
        private val BANK_SOURCES = setOf(TelemetrySource.PHRASE_INSERTED, TelemetrySource.PHRASE_SPOKEN, TelemetrySource.READER_OPENED)
        private val DIALOG_SOURCES = setOf(TelemetrySource.MESSAGE_SENT, TelemetrySource.SUGGESTION_ACCEPTED, TelemetrySource.SUGGESTION_DISMISSED)
        private val SPEECH_FAILURES = setOf(TelemetryFailureCode.ENGINE_UNAVAILABLE, TelemetryFailureCode.REQUEST_FAILED, TelemetryFailureCode.TIMEOUT, TelemetryFailureCode.CANCELLED)
        private val ACTION_FAILURES = setOf(TelemetryFailureCode.VALIDATION_FAILED, TelemetryFailureCode.STORAGE_FAILED, TelemetryFailureCode.PERMISSION_DENIED)
        private val SYNC_FAILURES = setOf(TelemetryFailureCode.NETWORK_UNAVAILABLE, TelemetryFailureCode.CONFLICT, TelemetryFailureCode.SERVER_ERROR)
    }
}
