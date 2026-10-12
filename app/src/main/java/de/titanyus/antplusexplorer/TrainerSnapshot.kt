package de.titanyus.antplusexplorer

import java.util.Locale

enum class AntProfile(val label: String) {
    FITNESS("Fitness Equipment (FE / FE-C)"),
    POWER("Fahrradleistung (Bicycle Power)")
}

/** UI-independent state, reusable in B-Cycle. Missing/invalid values stay null. */
data class TrainerSnapshot(
    val profile: AntProfile = AntProfile.FITNESS,
    val active: Boolean = false,
    val searching: Boolean = false,
    val tracking: Boolean = false,
    val status: String = "Bereit zum Testen",
    val device: String = "Noch kein Gerät ausgewählt",
    val equipment: String = "—",
    val speedKmh: Double? = null,
    val powerWatts: Double? = null,
    val cadenceRpm: Double? = null,
    val heartRateBpm: Int? = null,
    val distanceMeters: Long? = null,
    val elapsedSeconds: Double? = null,
    val dataEvents: Long = 0,
    val lastDataAt: Long? = null,
) {
    fun withMeasurement(
        now: Long, buffered: Boolean, live: Boolean,
        update: (TrainerSnapshot) -> TrainerSnapshot,
    ): TrainerSnapshot {
        if (buffered) return this
        val next = update(this)
        return if (live) next.copy(dataEvents = dataEvents + 1, lastDataAt = now) else next
    }

    fun reception(now: Long): String = when {
        lastDataAt == null -> "Noch keine Live-Messdaten empfangen"
        !active || !tracking -> "Empfang unterbrochen · angezeigte Werte sind zuletzt empfangen"
        now - lastDataAt > STALE_AFTER_MS -> "Keine aktuellen Daten · zuletzt vor ${(now - lastDataAt) / 1000} s"
        else -> "Live-Daten empfangen · vor ${(now - lastDataAt) / 1000} s · $dataEvents Ereignisse"
    }

    companion object { const val STALE_AFTER_MS = 5_000L }
}

internal fun validMetric(value: Number?): Double? = value?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
internal fun speedKmh(metersPerSecond: Number?): Double? = validMetric(metersPerSecond)?.times(3.6)
internal fun metric(value: Number?, unit: String, decimals: Int = 0): String =
    validMetric(value)?.let { String.format(Locale.GERMANY, "%.${decimals}f %s", it, unit) } ?: "—"
