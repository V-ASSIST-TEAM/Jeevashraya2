package com.jeevashraya.j2rescue

import com.jeevashraya.j2rescue.ble.BleRescueScanner
import com.jeevashraya.j2rescue.model.ScoutNodeAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoutNodeAlertTest {

    @Test
    fun testNormalByteState() {
        val stateByte: Byte = BleRescueScanner.STATE_BYTE_NORMAL
        val isAlert = (stateByte == BleRescueScanner.STATE_BYTE_FALL_ALERT)
        val alert = ScoutNodeAlert(
            deviceName = "ScoutNode_J2",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            rssi = -65,
            isAlertActive = isAlert,
            alertMessage = if (isAlert) "FALL / LANDSLIDE DETECTED" else "MONITORING - SAFE"
        )

        assertFalse(alert.isAlertActive)
        assertEquals("MONITORING - SAFE", alert.alertMessage)
        assertEquals(-65, alert.rssi)
    }

    @Test
    fun testEmergencyByteState() {
        val stateByte: Byte = BleRescueScanner.STATE_BYTE_FALL_ALERT
        val isAlert = (stateByte == BleRescueScanner.STATE_BYTE_FALL_ALERT)
        val alert = ScoutNodeAlert(
            deviceName = "ScoutNode_J2",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            rssi = -92, // Weak signal
            isAlertActive = isAlert,
            alertMessage = if (isAlert) "FALL / LANDSLIDE DETECTED" else "MONITORING - SAFE"
        )

        // Weak RSSI must NOT suppress an active alert
        assertTrue(alert.isAlertActive)
        assertEquals("FALL / LANDSLIDE DETECTED", alert.alertMessage)
        assertEquals(-92, alert.rssi)
    }

    @Test
    fun testServiceUuidDefinition() {
        assertEquals("4fafc201-1fb5-459e-8fcc-c5c9c331914b", BleRescueScanner.SERVICE_UUID_STRING)
        assertEquals("ScoutNode_J2", BleRescueScanner.SCOUT_DEVICE_NAME)
    }
}
