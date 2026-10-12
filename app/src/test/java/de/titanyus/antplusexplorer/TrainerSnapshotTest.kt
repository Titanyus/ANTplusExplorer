package de.titanyus.antplusexplorer

import org.junit.Assert.*
import org.junit.Test

class TrainerSnapshotTest {
    @Test fun bufferedMeasurementsCannotOverwriteLiveValuesOrRefreshReception() {
        val state = TrainerSnapshot(powerWatts = 180.0, lastDataAt = 1_000, dataEvents = 3)
        val next = state.withMeasurement(10_000, buffered = true, live = true) { it.copy(powerWatts = 90.0) }
        assertSame(state, next)
    }

    @Test fun syntheticStopValueDoesNotRefreshReception() {
        val state = TrainerSnapshot(powerWatts = 180.0, lastDataAt = 1_000, dataEvents = 3)
        val next = state.withMeasurement(10_000, buffered = false, live = false) { it.copy(powerWatts = 0.0) }
        assertEquals(0.0, next.powerWatts!!, 0.0)
        assertEquals(1_000L, next.lastDataAt)
        assertEquals(3L, next.dataEvents)
    }

    @Test fun genuineMeasurementRefreshesReceptionEvenIfValueHasNotChanged() {
        val state = TrainerSnapshot(powerWatts = 180.0, lastDataAt = 1_000, dataEvents = 3)
        val next = state.withMeasurement(10_000, buffered = false, live = true) { it.copy(powerWatts = 180.0) }
        assertEquals(10_000L, next.lastDataAt)
        assertEquals(4L, next.dataEvents)
    }

    @Test fun connectionAloneDoesNotProveDataReception() {
        val state = TrainerSnapshot(active = true, tracking = true)
        assertEquals("Noch keine Live-Messdaten empfangen", state.reception(10_000))
    }

    @Test fun recentlyReceivedDataIsReportedWithEventCount() {
        val state = TrainerSnapshot(active = true, tracking = true, lastDataAt = 1_000, dataEvents = 12)
        assertEquals("Live-Daten empfangen · vor 2 s · 12 Ereignisse", state.reception(3_000))
    }

    @Test fun receptionExpiresWithoutNewEvents() {
        val state = TrainerSnapshot(active = true, tracking = true, lastDataAt = 1_000)
        assertTrue(state.reception(6_001).startsWith("Keine aktuellen Daten"))
    }

    @Test fun interruptedConnectionDoesNotClaimLiveData() {
        val state = TrainerSnapshot(active = true, tracking = false, lastDataAt = 1_000)
        assertTrue(state.reception(1_500).startsWith("Empfang unterbrochen"))
        assertTrue(state.copy(active = false, tracking = true).reception(1_500).startsWith("Empfang unterbrochen"))
    }

    @Test fun antInvalidSentinelsAndNonFiniteValuesRemainMissing() {
        assertNull(validMetric(-1))
        assertNull(validMetric(Double.NaN))
        assertNull(validMetric(Double.POSITIVE_INFINITY))
        assertEquals("—", metric(null, "W"))
        assertEquals("—", metric(-1, "W"))
    }

    @Test fun zeroPowerAndZeroSpeedAreRealMeasurements() {
        assertEquals(0.0, validMetric(0)!!, 0.0)
        assertEquals("0 W", metric(0, "W"))
        assertEquals(0.0, speedKmh(0)!!, 0.0)
    }

    @Test fun speedConvertsMetersPerSecondToKilometersPerHour() {
        assertEquals(18.0, speedKmh(5)!!, 0.0001)
        assertNull(speedKmh(-1))
        assertEquals("18,0 km/h", metric(speedKmh(5), "km/h", 1))
    }
}
