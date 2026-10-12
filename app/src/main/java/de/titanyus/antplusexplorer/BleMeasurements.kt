package de.titanyus.antplusexplorer

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal enum class BleField { SPEED, CADENCE, POWER, DISTANCE, HEART_RATE, ELAPSED }

internal data class BleMeasurement(val values: Map<BleField, Number?>) {
    val hasValues get() = values.values.any { it != null }
    fun applyTo(state: TrainerSnapshot): TrainerSnapshot = state.copy(
        speedKmh = if (BleField.SPEED in values) values[BleField.SPEED]?.toDouble() else state.speedKmh,
        cadenceRpm = if (BleField.CADENCE in values) values[BleField.CADENCE]?.toDouble() else state.cadenceRpm,
        powerWatts = if (BleField.POWER in values) values[BleField.POWER]?.toDouble() else state.powerWatts,
        distanceMeters = if (BleField.DISTANCE in values) values[BleField.DISTANCE]?.toLong() else state.distanceMeters,
        heartRateBpm = if (BleField.HEART_RATE in values) values[BleField.HEART_RATE]?.toInt() else state.heartRateBpm,
        elapsedSeconds = if (BleField.ELAPSED in values) values[BleField.ELAPSED]?.toDouble() else state.elapsedSeconds,
    )
}

/** Bluetooth SIG FTMS Indoor Bike Data (2AD2), little endian, fields in flag order. */
internal fun indoorBikeData(data: ByteArray): BleMeasurement? = decodeFrame(data) { buffer ->
    val flags = buffer.u16()
    if (flags and 0xe000 != 0) return@decodeFrame null // Unknown fields cannot be decoded safely.
    val values = mutableMapOf<BleField, Number?>()
    if (flags and 1 == 0) values[BleField.SPEED] = buffer.u16().takeUnless { it == 0xffff }?.div(100.0)
    if (flags and 2 != 0) buffer.u16() // Average speed
    if (flags and 4 != 0) values[BleField.CADENCE] = buffer.u16().takeUnless { it == 0xffff }?.div(2.0)
    if (flags and 8 != 0) buffer.u16() // Average cadence
    if (flags and 16 != 0) values[BleField.DISTANCE] = buffer.u8() or (buffer.u8() shl 8) or (buffer.u8() shl 16)
    if (flags and 32 != 0) buffer.short // Resistance level; read-only prototype does not use it.
    if (flags and 64 != 0) values[BleField.POWER] = buffer.short.toInt().takeIf { it >= 0 && it != 0x7fff }
    if (flags and 128 != 0) buffer.short // Average power
    if (flags and 256 != 0) { buffer.u16(); buffer.u16(); buffer.u8() } // Expended energy
    if (flags and 512 != 0) values[BleField.HEART_RATE] = buffer.u8().takeIf { it > 0 }
    if (flags and 1024 != 0) buffer.u8() // Metabolic equivalent
    if (flags and 2048 != 0) values[BleField.ELAPSED] = buffer.u16()
    if (flags and 4096 != 0) buffer.u16() // Remaining time
    BleMeasurement(values)
}

/** Cycling Power Measurement (2A63): mandatory power, optional fields are not needed. */
internal fun cyclingPowerData(data: ByteArray): BleMeasurement? = decodeFrame(data) { buffer ->
    val flags = buffer.u16()
    val power = buffer.short.toInt().takeIf { it >= 0 }
    // Validate the advertised optional fields; never accept a truncated packet as valid power.
    for ((flag, size) in listOf(0 to 1, 2 to 2, 4 to 6, 5 to 4, 6 to 4,
        7 to 4, 8 to 3, 9 to 2, 10 to 2, 11 to 2)) {
        if (flags and (1 shl flag) != 0) repeat(size) { buffer.get() }
    }
    BleMeasurement(mapOf(BleField.POWER to power))
}

/** CSC Measurement (2A5B). No guessed wheel circumference; only crank cadence. */
internal class BleCadenceDecoder {
    private var previousRevolutions: Int? = null
    private var previousTime: Int? = null

    fun decode(data: ByteArray): BleMeasurement? = decodeFrame(data) { buffer ->
        val flags = buffer.u8()
        if (flags and 0xfc != 0) return@decodeFrame null
        if (flags and 1 != 0) { buffer.int; buffer.u16() } // Wheel data
        if (flags and 2 == 0) return@decodeFrame BleMeasurement(emptyMap())
        val revolutions = buffer.u16()
        val time = buffer.u16() // 1/1024 s, wraps every 64 s.
        val rpm = previousTime?.let { previous ->
            val ticks = (time - previous) and 0xffff
            val turns = (revolutions - previousRevolutions!!) and 0xffff
            if (ticks > 0) turns * 60.0 * 1024 / ticks else null
        }
        previousRevolutions = revolutions
        previousTime = time
        BleMeasurement(if (rpm != null) mapOf(BleField.CADENCE to rpm) else emptyMap())
    }
}

private fun ByteBuffer.u8() = get().toInt() and 0xff
private fun ByteBuffer.u16() = short.toInt() and 0xffff
private inline fun decodeFrame(data: ByteArray, block: (ByteBuffer) -> BleMeasurement?): BleMeasurement? =
    try { block(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)) }
    catch (_: BufferUnderflowException) { null }
