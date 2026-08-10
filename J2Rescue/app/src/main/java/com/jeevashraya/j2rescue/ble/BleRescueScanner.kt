package com.jeevashraya.j2rescue.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.jeevashraya.j2rescue.model.ScoutNodeAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleRescueScanner(private val context: Context) {

    companion object {
        private const val TAG = "BleRescueScanner"
        const val SERVICE_UUID_STRING = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        val SCOUT_SERVICE_UUID: ParcelUuid = ParcelUuid.fromString(SERVICE_UUID_STRING)
        const val SCOUT_DEVICE_NAME = "ScoutNode_J2"

        // 1-byte service data protocol
        const val STATE_BYTE_NORMAL: Byte = 0x00
        const val STATE_BYTE_FALL_ALERT: Byte = 0x01
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _detectedNode = MutableStateFlow<ScoutNodeAlert?>(null)
    val detectedNode: StateFlow<ScoutNodeAlert?> = _detectedNode.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { handleScanResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            _scanError.value = "Scan failed with error code: $errorCode"
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _scanError.value = "Bluetooth is disabled or not available."
            return false
        }

        val scanner = bleScanner
        if (scanner == null) {
            _scanError.value = "BLE Scanner not available on this device."
            return false
        }

        if (_isScanning.value) {
            return true
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            _scanError.value = null
            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
            Log.i(TAG, "BLE Rescue Scan started successfully (listening for broadcast packets).")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permissions: ${e.message}")
            _scanError.value = "Bluetooth permissions missing: ${e.message}"
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan: ${e.message}")
            _scanError.value = "Failed to start scan: ${e.message}"
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return

        try {
            bleScanner?.stopScan(scanCallback)
            Log.i(TAG, "BLE Rescue Scan stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan: ${e.message}")
        } finally {
            _isScanning.value = false
        }
    }

    fun clearAlert() {
        _detectedNode.value = null
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val scanRecord: ScanRecord? = result.scanRecord

        val deviceName = scanRecord?.deviceName ?: device?.name ?: "Unknown"
        val deviceAddress = device?.address ?: "00:00:00:00:00:00"

        // Check if advertisement contains ScoutNode Service UUID or name
        val serviceUuids = scanRecord?.serviceUuids
        val hasScoutUuid = serviceUuids?.contains(SCOUT_SERVICE_UUID) == true
        val matchesName = deviceName.contains("ScoutNode", ignoreCase = true)

        // Extract raw Service Data for ScoutNode Service UUID
        val serviceDataBytes = scanRecord?.getServiceData(SCOUT_SERVICE_UUID)
        val rawServiceDataHex = serviceDataBytes?.joinToString(separator = "") { "%02X".format(it) } ?: "null"

        // Parse alert state from 1-byte Service Data (0x00 = NORMAL, 0x01 = FALL_ALERT)
        val alertState = parseAlertState(serviceDataBytes, scanRecord)
        val alertLabel = when (alertState) {
            true -> "FALL_ALERT"
            false -> "NORMAL"
            null -> "UNKNOWN"
        }

        // ==========================================
        // DETAILED LOGCAT OUTPUT FOR EVERY BLE PACKET
        // ==========================================
        Log.d(
            TAG,
            """
            BLE DEVICE:
            name=$deviceName
            address=$deviceAddress
            rssi=${result.rssi}
            serviceUuids=${serviceUuids?.joinToString { it.toString() } ?: "none"}
            scoutUuidFound=$hasScoutUuid
            serviceData=$rawServiceDataHex
            alert=$alertLabel
            """.trimIndent()
        )

        // Filter: Must match ScoutNode UUID, ScoutNode name, or have valid ScoutNode service data
        if (!hasScoutUuid && !matchesName && serviceDataBytes == null) {
            return
        }

        val isAlertActive = (alertState == true)
        val alertMessage = when (isAlertActive) {
            true -> "FALL / LANDSLIDE DETECTED"
            false -> "MONITORING - SAFE"
        }

        val scoutAlert = ScoutNodeAlert(
            deviceName = if (deviceName == "Unknown") SCOUT_DEVICE_NAME else deviceName,
            deviceAddress = deviceAddress,
            rssi = result.rssi, // RSSI represents signal strength only
            isAlertActive = isAlertActive,
            alertMessage = alertMessage,
            timestamp = System.currentTimeMillis()
        )

        _detectedNode.value = scoutAlert
        Log.i(TAG, "--> ScoutNode Accepted: isAlertActive=$isAlertActive ($alertMessage) RSSI=${result.rssi} dBm")
    }

    /**
     * Parses the 1-byte BLE Service Data for ScoutNode:
     * 0x00 -> Normal (false)
     * 0x01 -> Fall Alert (true)
     */
    private fun parseAlertState(serviceDataBytes: ByteArray?, scanRecord: ScanRecord?): Boolean? {
        if (serviceDataBytes != null && serviceDataBytes.isNotEmpty()) {
            val byteVal = serviceDataBytes[0]
            if (byteVal == STATE_BYTE_FALL_ALERT) return true
            if (byteVal == STATE_BYTE_NORMAL) return false
        }

        // Secondary fallback: search raw bytes for 128-bit UUID followed by 0x01 or 0x00
        scanRecord?.bytes?.let { rawBytes ->
            val uuidBytes = SCOUT_SERVICE_UUID.uuid
            val msb = uuidBytes.mostSignificantBits
            val lsb = uuidBytes.leastSignificantBits
            // Check if rawBytes contain explicit alert flag
            for (i in 0 until rawBytes.size - 1) {
                if (rawBytes[i] == 0x16.toByte() && i + 17 < rawBytes.size) {
                    val payloadByte = rawBytes[i + 17]
                    if (payloadByte == STATE_BYTE_FALL_ALERT) return true
                    if (payloadByte == STATE_BYTE_NORMAL) return false
                }
            }
        }

        return if (serviceDataBytes != null) false else null
    }
}
