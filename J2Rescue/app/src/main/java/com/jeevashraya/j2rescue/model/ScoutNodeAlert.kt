package com.jeevashraya.j2rescue.model

/**
 * Represents the connectionless broadcast telemetry and alert state of a discovered ScoutNode.
 *
 * @property deviceName Advertised device name (e.g. "ScoutNode_J2").
 * @property deviceAddress MAC address of the hardware node.
 * @property rssi Received Signal Strength Indicator in dBm (proximity only).
 * @property isAlertActive True if the payload explicitly decoded a critical alert (e.g. "SCOUT|FALL_ALERT").
 * @property alertMessage Human-readable decoded status message.
 * @property timestamp Epoch millis timestamp of the last received advertising packet.
 */
data class ScoutNodeAlert(
    val deviceName: String,
    val deviceAddress: String,
    val rssi: Int,
    val isAlertActive: Boolean,
    val alertMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)
