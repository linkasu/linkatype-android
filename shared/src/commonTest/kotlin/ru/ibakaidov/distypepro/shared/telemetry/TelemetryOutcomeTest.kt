package ru.ibakaidov.distypepro.shared.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TelemetryOutcomeTest {
    @Test
    fun consentDefaultsToUnknownAndRequiresAnExplicitGrant() {
        assertEquals(TelemetryConsent.UNKNOWN, TelemetryConsent.fromStorage("granted"))
        assertEquals(TelemetryConsent.UNKNOWN, TelemetryConsent.fromStorage(null))
        assertEquals(
            TelemetryConsent.GRANTED,
            TelemetryConsentMachine.transition(TelemetryConsent.UNKNOWN, TelemetryConsentAction.GRANT),
        )
    }

    @Test
    fun speechOutcomeRejectsFieldsOutsideTheClosedSchema() {
        assertFailsWith<IllegalArgumentException> {
            TelemetryOutcome(
                kind = TelemetryOutcomeKind.SPEECH_COMPLETED,
                result = TelemetryResult.COMPLETED,
                source = TelemetrySource.INPUT,
                mode = TelemetryMode.LOCAL,
                countBucket = TelemetryCountBucket.ONE,
                durationBucket = TelemetryDurationBucket.UNDER_5S,
                failureCode = TelemetryFailureCode.STORAGE_FAILED,
            )
        }
    }

    @Test
    fun bucketsNeverContainContentOrIdentifiers() {
        assertEquals(TelemetryCountBucket.SIX_TO_TWENTY, TelemetryCountBucket.fromCount(20))
        assertEquals(TelemetryDurationBucket.FIVE_TO_30S, TelemetryDurationBucket.fromDurationMillis(30_000L))
    }
}
