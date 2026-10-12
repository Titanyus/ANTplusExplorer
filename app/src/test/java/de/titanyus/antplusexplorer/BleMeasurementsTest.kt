package de.titanyus.antplusexplorer

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BleMeasurementsTest {
    private fun shorts(vararg values: Int): ByteArray = ByteBuffer.allocate(values.size * 2)
        .order(ByteOrder.LITTLE_ENDIAN).apply { values.forEach { putShort(it.toShort()) } }.array()

    @Test fun indoorBikeDecodesSpeedCadenceAndPowerInTheirSpecifiedUnits() {
        val sample = indoorBikeData(shorts(0x44, 2534, 171, 220))!!
        val state = sample.applyTo(TrainerSnapshot())
        assertEquals(25.34, state.speedKmh!!, 0.001)
        assertEquals(85.5, state.cadenceRpm!!, 0.001)
        assertEquals(220.0, state.powerWatts!!, 0.001)
    }

    @Test fun moreDataFlagOmitsSpeedAndPreservesPreviouslyReceivedSpeed() {
        val sample = indoorBikeData(shorts(0x45, 180, 150))!!
        val state = sample.applyTo(TrainerSnapshot(speedKmh = 20.0))
        assertEquals(20.0, state.speedKmh!!, 0.0)
        assertEquals(90.0, state.cadenceRpm!!, 0.0)
        assertEquals(150.0, state.powerWatts!!, 0.0)
    }

    @Test fun optionalFieldsDoNotShiftHeartRateAndElapsedTime() {
        // MoreData, distance, resistance, power, energy, heart rate, MET and elapsed.
        val bytes = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(0xf71.toShort())
            put(0x56.toByte()); put(0x34.toByte()); put(0x12.toByte())
            putShort(20); putShort(210)
            putShort(10); putShort(200); put(5)
            put(145.toByte()); put(80.toByte()); putShort(456)
        }
        val sample = indoorBikeData(bytes.array().copyOf(bytes.position()))!!
        val state = sample.applyTo(TrainerSnapshot())
        assertEquals(0x123456L, state.distanceMeters)
        assertEquals(210.0, state.powerWatts!!, 0.0)
        assertEquals(145, state.heartRateBpm)
        assertEquals(456.0, state.elapsedSeconds!!, 0.0)
    }

    @Test fun allTruncatedIndoorBikePacketsAreRejected() {
        val frame = shorts(0x44, 2400, 180, 250)
        for (size in 0 until frame.size) assertNull("length $size", indoorBikeData(frame.copyOf(size)))
    }

    @Test fun unavailableCadenceAndPowerClearOldValuesWithoutClaimingLiveMeasurements() {
        val sample = indoorBikeData(shorts(0x45, 0xffff, 0x7fff))!!
        assertFalse(sample.hasValues)
        val state = sample.applyTo(TrainerSnapshot(powerWatts = 180.0, cadenceRpm = 80.0))
        assertNull(state.powerWatts)
        assertNull(state.cadenceRpm)
    }

    @Test fun genuineZeroValuesRemainMeasurements() {
        val sample = indoorBikeData(shorts(0x44, 0, 0, 0))!!
        assertTrue(sample.hasValues)
        assertEquals(0.0, sample.applyTo(TrainerSnapshot()).powerWatts!!, 0.0)
    }

    @Test fun powerMeasurementValidatesOptionalFieldLengths() {
        assertEquals(230, cyclingPowerData(shorts(0, 230))!!.values[BleField.POWER])
        assertNull(cyclingPowerData(shorts(0x10, 230))) // Declared wheel data absent.
        assertNull(cyclingPowerData(byteArrayOf(0, 0, 10)))
    }

    private fun crank(revolutions: Int, time: Int) = byteArrayOf(2) + shorts(revolutions, time)

    @Test fun cadenceRequiresTwoSamplesAndHandlesCounterRollover() {
        val decoder = BleCadenceDecoder()
        assertFalse(decoder.decode(crank(65535, 65024))!!.hasValues)
        val sample = decoder.decode(crank(0, 512))!! // One turn in one second.
        assertEquals(60.0, sample.values[BleField.CADENCE]!!.toDouble(), 0.001)
    }

    @Test fun unchangedCadenceCountersDoNotRefreshReception() {
        val decoder = BleCadenceDecoder()
        decoder.decode(crank(10, 1000))
        assertFalse(decoder.decode(crank(10, 1000))!!.hasValues)
    }

    @Test fun malformedCadencePacketDoesNotCorruptThePreviousSample() {
        val decoder = BleCadenceDecoder()
        decoder.decode(crank(10, 1000))
        assertNull(decoder.decode(byteArrayOf(2, 50)))
        assertEquals(60.0, decoder.decode(crank(11, 2024))!!.values[BleField.CADENCE]!!.toDouble(), 0.001)
    }
}
