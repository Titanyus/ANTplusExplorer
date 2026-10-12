package de.titanyus.antplusexplorer

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.UUID

internal data class BleDeviceChoice(val address: String, val name: String, val rssi: Int) {
    override fun toString() = "$name · ${address.takeLast(8)} · $rssi dBm"
}

/** Called only after MainActivity has obtained BLUETOOTH_SCAN and BLUETOOTH_CONNECT. */
@SuppressLint("MissingPermission")
internal class BleTrainerClient(
    private val activity: Activity,
    private val onChanged: (TrainerSnapshot) -> Unit,
    private val onDevices: (List<BleDeviceChoice>) -> Unit,
    private val onLog: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0
    private var scannerCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var pending: BluetoothGattDescriptor? = null
    private val subscriptions = ArrayDeque<BluetoothGattCharacteristic>()
    private val found = linkedMapOf<String, BleDeviceChoice>()
    private var cadence = BleCadenceDecoder()
    private var notifications = 0
    private var subscribed = 0
    var snapshot = TrainerSnapshot(profile = ConnectionProfile.BLE)
        private set

    private val adapter get() = activity.getSystemService(BluetoothManager::class.java)?.adapter

    fun scan() {
        release()
        found.clear()
        onDevices(emptyList())
        snapshot = TrainerSnapshot(profile = ConnectionProfile.BLE, active = true, searching = true,
            status = "BLE-Suche · JFICycle oder iTrack in der Geräteliste auswählen")
        emit()
        try {
            val radio = adapter ?: return disconnect("Bluetooth-Adapter nicht vorhanden")
            if (!radio.isEnabled) return disconnect("Bluetooth einschalten und erneut suchen")
            val scanner = radio.bluetoothLeScanner ?: return disconnect("BLE-Scanner nicht verfügbar")
            val session = generation
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    dispatch(session) {
                        val address = result.device.address
                        if (address !in found && found.size >= 60) return@dispatch
                        val name = result.scanRecord?.deviceName ?: result.device.name ?: "Unbenannt"
                        found[address] = BleDeviceChoice(address, name, result.rssi)
                        onDevices(found.values.toList())
                    }
                }
                override fun onScanFailed(errorCode: Int) {
                    dispatch(session) { disconnect("BLE-Suche fehlgeschlagen (Code $errorCode)") }
                }
            }
            scannerCallback = callback
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            onLog("Bluetooth LE: ungefilterte Gerätesuche für 20 Sekunden gestartet")
            main.postDelayed({
                if (session == generation && scannerCallback != null) {
                    stopScan()
                    snapshot = snapshot.copy(searching = false,
                        status = "BLE-Suche beendet · ${found.size} Geräte · auswählen oder Suche abbrechen")
                    onLog(snapshot.status)
                    emit()
                }
            }, 20_000)
        } catch (error: RuntimeException) {
            disconnect("BLE-Suche fehlgeschlagen: ${error.javaClass.simpleName} · ${error.message.orEmpty()}")
        }
    }

    fun connect(device: BleDeviceChoice) {
        release()
        cadence = BleCadenceDecoder()
        notifications = 0
        subscribed = 0
        snapshot = TrainerSnapshot(profile = ConnectionProfile.BLE, active = true,
            status = "Bluetooth LE wird verbunden…", device = "${device.name} · BLE ${device.address}")
        onLog("Verbinde über Bluetooth LE: ${snapshot.device}")
        emit()
        val session = generation
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) {
                dispatch(session) {
                    if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                        disconnect("BLE-Verbindung beendet (GATT $status)")
                    } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                        snapshot = snapshot.copy(tracking = true, status = "Bluetooth LE verbunden · Datendienste werden geprüft")
                        onLog(snapshot.status)
                        emit()
                        if (!connection.discoverServices()) disconnect("BLE-Dienstsuche konnte nicht gestartet werden")
                    }
                }
            }

            override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) {
                dispatch(session) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        disconnect("BLE-Dienstsuche fehlgeschlagen (GATT $status)")
                    } else discover(connection)
                }
            }

            override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                dispatch(session) {
                    if (pending?.characteristic?.uuid != descriptor.characteristic.uuid) return@dispatch
                    pending = null
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        subscribed++
                        onLog("BLE-Datenabo aktiv: ${descriptor.characteristic.uuid}")
                    } else onLog("BLE-Datenabo abgelehnt: ${descriptor.characteristic.uuid} · GATT $status")
                    subscribeNext(connection)
                }
            }

            override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                val copy = value.copyOf()
                dispatch(session) { received(characteristic.uuid, copy) }
            }
        }
        try {
            val radio = adapter ?: return disconnect("Bluetooth-Adapter nicht vorhanden")
            gatt = radio.getRemoteDevice(device.address).connectGatt(activity, false, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) disconnect("BLE-Verbindungsaufbau konnte nicht gestartet werden")
            main.postDelayed({
                if (session == generation && snapshot.equipment == "—") disconnect("BLE-Verbindung/Dienstsuche: Zeitüberschreitung")
            }, 20_000)
        } catch (error: RuntimeException) {
            disconnect("BLE-Verbindung fehlgeschlagen: ${error.javaClass.simpleName} · ${error.message.orEmpty()}")
        }
    }

    private fun discover(connection: BluetoothGatt) {
        val services = connection.services
        val labels = services.map { serviceName(it.uuid) }.distinct()
        snapshot = snapshot.copy(equipment = labels.joinToString(", ").ifEmpty { "Keine Dienste" })
        for (service in services) {
            onLog("BLE-Dienst ${service.uuid}: ${serviceName(service.uuid)}")
            for (characteristic in service.characteristics) {
                onLog("  Merkmal ${characteristic.uuid} · Eigenschaften ${characteristic.properties}")
                val supportsUpdates = characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                if (characteristic.uuid in SUPPORTED && supportsUpdates) subscriptions.addLast(characteristic)
            }
        }
        subscribeNext(connection)
    }

    private fun subscribeNext(connection: BluetoothGatt) {
        while (subscriptions.isNotEmpty()) {
            val characteristic = subscriptions.removeFirst()
            val descriptor = characteristic.getDescriptor(CCCD)
            if (descriptor == null || !connection.setCharacteristicNotification(characteristic, true)) {
                onLog("BLE-Datenabo nicht möglich: ${characteristic.uuid}")
                continue
            }
            val value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            pending = descriptor
            val result = connection.writeDescriptor(descriptor, value)
            if (result != BluetoothStatusCodes.SUCCESS) {
                pending = null
                onLog("BLE-Datenabo konnte nicht gestartet werden: Code $result")
                continue
            }
            val session = generation
            main.postDelayed({
                if (session == generation && pending === descriptor) disconnect("BLE-Datenabo: Zeitüberschreitung")
            }, 5_000)
            return
        }
        snapshot = snapshot.copy(status = if (subscribed > 0)
            "Bluetooth LE verbunden · $subscribed Messdatenabos aktiv" else
            "Bluetooth LE verbunden · keine unterstützten Messdatenabos verfügbar")
        onLog(snapshot.status)
        emit()
    }

    private fun received(uuid: UUID, value: ByteArray) {
        notifications++
        if (notifications <= 4) onLog("BLE-Daten $uuid: " + value.take(32).joinToString(" ") { "%02X".format(it.toInt() and 0xff) })
        val sample = when (uuid) {
            INDOOR_BIKE -> indoorBikeData(value)
            CYCLING_POWER -> cyclingPowerData(value)
            CSC -> cadence.decode(value)
            else -> null
        }
        if (sample == null) {
            if (notifications <= 4) onLog("BLE-Messdaten unvollständig oder nicht dekodierbar")
            return
        }
        val before = snapshot.dataEvents
        snapshot = snapshot.withMeasurement(SystemClock.elapsedRealtime(), buffered = false, live = sample.hasValues, update = sample::applyTo)
        if (before == 0L && snapshot.dataEvents > 0L) onLog("Erste Trainingswerte über Bluetooth LE empfangen")
        emit()
    }

    fun disconnect(reason: String = "Bluetooth LE getrennt") {
        release()
        snapshot = snapshot.copy(active = false, searching = false, tracking = false, status = reason)
        onLog(reason)
        emit()
    }

    private fun stopScan() {
        val callback = scannerCallback
        scannerCallback = null
        if (callback != null) runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
    }

    private fun release() {
        generation++
        stopScan()
        pending = null
        subscriptions.clear()
        gatt?.let { connection ->
            runCatching { connection.disconnect() }
            connection.close()
        }
        gatt = null
    }

    private fun emit() = onChanged(snapshot)
    private fun dispatch(session: Int, action: () -> Unit) {
        main.post { if (session == generation) action() }
    }

    companion object {
        private fun uuid(short: String) = UUID.fromString("0000$short-0000-1000-8000-00805f9b34fb")
        private val CCCD = uuid("2902")
        private val INDOOR_BIKE = uuid("2ad2")
        private val CYCLING_POWER = uuid("2a63")
        private val CSC = uuid("2a5b")
        private val SUPPORTED = setOf(INDOOR_BIKE, CYCLING_POWER, CSC)
        private fun serviceName(id: UUID): String = when (id) {
            uuid("1826") -> "Fitness Machine (FTMS)"
            uuid("1818") -> "Cycling Power"
            uuid("1816") -> "Cycling Speed/Cadence"
            uuid("180a") -> "Device Information"
            uuid("180f") -> "Battery"
            uuid("1800") -> "Generic Access"
            uuid("1801") -> "Generic Attribute"
            else -> id.toString()
        }
    }
}
