package de.titanyus.antplusexplorer

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import java.util.EnumSet

/** Owns one ANT+ connection. All state changes and listeners run on the main thread. */
class AntTrainerClient(
    private val activity: Activity,
    private val onChanged: (TrainerSnapshot) -> Unit,
    private val onLog: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0
    private var handle: PccReleaseHandle<*>? = null
    private var fitness: AntPlusFitnessEquipmentPcc? = null
    private var fitnessType: AntPlusFitnessEquipmentPcc.EquipmentType? = null
    var snapshot = TrainerSnapshot()
        private set

    fun connect(profile: AntProfile) {
        release()
        val session = generation
        snapshot = TrainerSnapshot(profile = profile, active = true, searching = true,
            status = "Gerätesuche geöffnet · bitte den Matrix auswählen")
        emit()
        onLog("Suche: ${profile.label}")
        val stateReceiver = AntPluginPcc.IDeviceStateChangeReceiver { state ->
            dispatch(session) { deviceState(state) }
        }
        try {
            handle = when (profile) {
                AntProfile.FITNESS -> AntPlusFitnessEquipmentPcc.requestNewOpenAccess(
                    activity, activity, true, -1,
                    AntPluginPcc.IPluginAccessResultReceiver { pcc, result, state ->
                        dispatch(session) {
                            if (result == RequestAccessResult.SUCCESS && pcc != null) {
                                fitness = pcc
                                connected(pcc.deviceName, pcc.antDeviceNumber, state)
                                subscribeFitness(session, pcc)
                                fitnessType?.let { subscribeFitnessType(session, pcc, it) }
                            } else accessFailed(result)
                        }
                    }, stateReceiver,
                    AntPlusFitnessEquipmentPcc.IFitnessEquipmentStateReceiver { _, _, type, state ->
                        dispatch(session) {
                            snapshot = snapshot.copy(equipment = "$type · $state")
                            if (type != fitnessType) {
                                fitnessType = type
                                fitness?.let { subscribeFitnessType(session, it, type) }
                                onLog("Fitness-Gerätetyp: $type · $state")
                            }
                            emit()
                        }
                    }
                )
                AntProfile.POWER -> AntPlusBikePowerPcc.requestAccess(
                    activity, activity, true, -1,
                    AntPluginPcc.IPluginAccessResultReceiver { pcc, result, state ->
                        dispatch(session) {
                            if (result == RequestAccessResult.SUCCESS && pcc != null) {
                                connected(pcc.deviceName, pcc.antDeviceNumber, state)
                                subscribePower(session, pcc)
                            } else accessFailed(result)
                        }
                    }, stateReceiver
                )
            }
        } catch (error: RuntimeException) {
            release()
            snapshot = snapshot.copy(active = false, searching = false, tracking = false,
                status = "ANT+-Aufruf fehlgeschlagen: ${error.javaClass.simpleName}")
            onLog("${snapshot.status}: ${error.message.orEmpty()}")
            emit()
        }
    }

    fun disconnect(reason: String = "Verbindung getrennt") {
        release()
        snapshot = snapshot.copy(active = false, searching = false, tracking = false, status = reason)
        onLog(reason)
        emit()
    }

    private fun release() {
        // Invalidate queued callbacks before closing the old channel/search.
        generation++
        handle?.close()
        handle = null
        fitness = null
        fitnessType = null
    }

    private fun dispatch(session: Int, action: () -> Unit) {
        main.post { if (session == generation) action() }
    }

    private fun emit() = onChanged(snapshot)

    private fun connected(name: String?, number: Int, state: DeviceState) {
        snapshot = snapshot.copy(searching = false, device = "${name ?: "ANT+-Gerät"} · ID $number")
        onLog("Gerät ausgewählt: ${snapshot.device}")
        deviceState(state)
    }

    private fun deviceState(state: DeviceState) {
        if (state == DeviceState.DEAD) {
            disconnect("Verbindung verloren (DEAD) · bitte erneut suchen")
            return
        }
        snapshot = snapshot.copy(tracking = state == DeviceState.TRACKING, status = when (state) {
            DeviceState.TRACKING -> "Verbunden · Messdaten werden abonniert"
            DeviceState.SEARCHING -> "Signal verloren · ANT+-Dienst sucht erneut"
            DeviceState.CLOSED -> "ANT+-Kanal geschlossen · keine Funkverbindung"
            else -> "ANT+-Status: $state"
        })
        onLog("ANT+-Status: $state")
        emit()
    }

    private fun accessFailed(result: RequestAccessResult) {
        val explanation = when (result) {
            RequestAccessResult.ADAPTER_NOT_DETECTED -> "Kein ANT-Adapter erkannt. ANT-Hardware oder USB-ANT-Stick prüfen."
            RequestAccessResult.DEPENDENCY_NOT_INSTALLED -> "ANT Radio Service und ANT+ Plugins Service installieren/aktualisieren."
            RequestAccessResult.CHANNEL_NOT_AVAILABLE -> "Kein freier ANT-Kanal. Andere ANT+-Apps schließen."
            RequestAccessResult.SEARCH_TIMEOUT -> "Kein Gerät gefunden. Treten, ANT+-Übertragung am Trainer aktivieren oder anderes Profil testen."
            RequestAccessResult.USER_CANCELLED -> "Gerätesuche abgebrochen."
            RequestAccessResult.DEVICE_ALREADY_IN_USE, RequestAccessResult.ALREADY_SUBSCRIBED -> "Gerät bereits in Benutzung. Andere ANT+-Verbindungen beenden."
            else -> "ANT+-Anfrage fehlgeschlagen. Dienste prüfen; Details stehen in Logcat."
        }
        disconnect("$explanation ($result)")
    }

    private fun measurement(
        session: Int,
        flags: EnumSet<EventFlag>,
        live: Boolean = true,
        update: (TrainerSnapshot) -> TrainerSnapshot,
    ) = dispatch(session) {
        // Buffered events do not establish current reception. Keep live values unchanged.
        val before = snapshot.dataEvents
        snapshot = snapshot.withMeasurement(SystemClock.elapsedRealtime(),
            buffered = EventFlag.WAS_BUFFERED in flags, live = live, update = update)
        if (before == 0L && snapshot.dataEvents > 0L) onLog("Erste Live-Messdaten empfangen")
        emit()
    }

    private fun subscribeFitness(session: Int, pcc: AntPlusFitnessEquipmentPcc) {
        pcc.subscribeGeneralFitnessEquipmentDataEvent { _, flags, elapsed, distance, speed, _, hr, _ ->
            measurement(session, flags) { it.copy(
                speedKmh = speedKmh(speed), distanceMeters = distance.takeIf { d -> d >= 0 },
                elapsedSeconds = validMetric(elapsed), heartRateBpm = hr.takeIf { h -> h > 0 },
            ) }
        }
    }

    private fun subscribeFitnessType(
        session: Int, pcc: AntPlusFitnessEquipmentPcc, type: AntPlusFitnessEquipmentPcc.EquipmentType,
    ) {
        when (type) {
            AntPlusFitnessEquipmentPcc.EquipmentType.BIKE ->
                pcc.bikeMethods.subscribeBikeDataEvent { _, flags, cadence, power ->
                    measurement(session, flags) { it.copy(cadenceRpm = validMetric(cadence), powerWatts = validMetric(power)) }
                }
            AntPlusFitnessEquipmentPcc.EquipmentType.TRAINER -> {
                val accepted = pcc.trainerMethods.subscribeRawTrainerDataEvent { _, flags, _, cadence, power, _ ->
                    measurement(session, flags) { it.copy(cadenceRpm = validMetric(cadence), powerWatts = validMetric(power)) }
                }
                if (!accepted) onLog("Trainer-Datenabo abgelehnt · ANT+ Plugins Service aktualisieren")
            }
            else -> onLog("$type: allgemeine Fitness-Daten werden angezeigt")
        }
    }

    private fun subscribePower(session: Int, pcc: AntPlusBikePowerPcc) {
        pcc.subscribeCalculatedPowerEvent { _, flags, source, power ->
            val realData = source in setOf(AntPlusBikePowerPcc.DataSource.POWER_ONLY_DATA,
                AntPlusBikePowerPcc.DataSource.WHEEL_TORQUE_DATA,
                AntPlusBikePowerPcc.DataSource.CRANK_TORQUE_DATA, AntPlusBikePowerPcc.DataSource.CTF_DATA)
            measurement(session, flags, live = realData) { it.copy(powerWatts = validMetric(power)) }
        }
        pcc.subscribeInstantaneousCadenceEvent { _, flags, _, cadence ->
            measurement(session, flags, live = false) { it.copy(cadenceRpm = validMetric(cadence)) }
        }
    }
}
