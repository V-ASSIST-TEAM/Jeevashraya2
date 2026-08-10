package com.jeevashraya.j2rescue.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeevashraya.j2rescue.ble.BleRescueScanner
import com.jeevashraya.j2rescue.model.ScoutNodeAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RescueUiState(
    val isScanning: Boolean = false,
    val detectedNode: ScoutNodeAlert? = null,
    val isBluetoothEnabled: Boolean = true,
    val errorMessage: String? = null
)

class RescueViewModel(application: Application) : AndroidViewModel(application) {

    private val bleScanner = BleRescueScanner(application.applicationContext)

    private val _uiState = MutableStateFlow(RescueUiState())
    val uiState: StateFlow<RescueUiState> = _uiState.asStateFlow()

    private var previousAlertState = false

    init {
        viewModelScope.launch {
            combine(
                bleScanner.isScanning,
                bleScanner.detectedNode,
                bleScanner.scanError
            ) { scanning, node, error ->
                RescueUiState(
                    isScanning = scanning,
                    detectedNode = node,
                    isBluetoothEnabled = bleScanner.isBluetoothEnabled,
                    errorMessage = error
                )
            }.collect { newState ->
                _uiState.value = newState

                // Trigger vibration pattern when transitioning into an active emergency alert
                val isCurrentlyAlert = newState.detectedNode?.isAlertActive == true
                if (isCurrentlyAlert && !previousAlertState) {
                    triggerEmergencyVibration()
                }
                previousAlertState = isCurrentlyAlert
            }
        }
    }

    fun startScanning(): Boolean {
        return bleScanner.startScan()
    }

    fun stopScanning() {
        bleScanner.stopScan()
    }

    fun toggleScanning() {
        if (_uiState.value.isScanning) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    fun clearAlert() {
        bleScanner.clearAlert()
    }

    private fun triggerEmergencyVibration() {
        try {
            val context = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 400, 200, 400, 200, 600),
                    intArrayOf(0, 255, 0, 255, 0, 255),
                    -1
                )
                val combined = CombinedVibration.createParallel(vibrationEffect)
                vibratorManager?.vibrate(combined)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 400, 200, 400, 200, 600),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                }
            }
        } catch (e: Exception) {
            // Non-critical vibration fallback
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleScanner.stopScan()
    }
}
