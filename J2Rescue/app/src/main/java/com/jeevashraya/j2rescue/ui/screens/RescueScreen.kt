package com.jeevashraya.j2rescue.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeevashraya.j2rescue.model.ScoutNodeAlert
import com.jeevashraya.j2rescue.viewmodel.RescueViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescueScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RescueViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Required permissions based on Android API level
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            viewModel.startScanning()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.startScanning()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rescue Node Monitor",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasPermissions) {
                PermissionRequestCard(
                    onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions)
                    }
                )
            } else {
                // Scanning Status & Pulse
                ScanningHeader(
                    isScanning = uiState.isScanning,
                    hasDetectedNode = uiState.detectedNode != null
                )

                // Error Banner if Bluetooth is off or scan failed
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ $error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Discovered ScoutNode Telemetry / Alert Banner
                uiState.detectedNode?.let { node ->
                    ScoutNodeDetailsCard(node = node)
                } ?: run {
                    if (uiState.isScanning) {
                        WaitingForBroadcastCard()
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                // Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.toggleScanning() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.isScanning) "Pause Scan" else "Start Scan")
                    }

                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Home")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningHeader(
    isScanning: Boolean,
    hasDetectedNode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isScanning) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .scale(if (isScanning) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(Color(0xFF00C853).copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (hasDetectedNode) "Receiving BLE Broadcasts" else "Scanning for ScoutNode_J2...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = "Scanner Paused",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private data class ProximityInfo(
    val label: String,
    val color: Color,
    val level: Int,
    val description: String
)

@Composable
private fun ProximityGauge(level: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..4) {
            val isFilled = i <= level
            val segmentColor = if (isFilled) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(segmentColor)
            )
        }
    }
}

@Composable
private fun ScoutNodeDetailsCard(node: ScoutNodeAlert) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(node.timestamp) { timeFormatter.format(Date(node.timestamp)) }
    var showAdvanced by remember { mutableStateOf(false) }

    val proximityInfo = remember(node.rssi) {
        when {
            node.rssi >= -60 -> ProximityInfo(
                label = "Very Close",
                color = Color(0xFFD50000), // Crimson Red
                level = 4,
                description = "The signal is extremely strong! Look in your immediate surroundings."
            )
            node.rssi >= -75 -> ProximityInfo(
                label = "Getting Closer",
                color = Color(0xFFFF6D00), // Dark Orange
                level = 3,
                description = "You are approaching the node. Continue moving in this direction."
            )
            node.rssi >= -85 -> ProximityInfo(
                label = "Far",
                color = Color(0xFF00B0FF), // Bright Blue
                level = 2,
                description = "Signal detected. Walk in different directions to see if the signal gets stronger."
            )
            else -> ProximityInfo(
                label = "Very Far",
                color = Color(0xFF78909C), // Slate Grey
                level = 1,
                description = "The signal is very weak. Keep searching and move to higher ground."
            )
        }
    }

    // High-Priority Alert Banner vs Safe Banner
    if (node.isAlertActive) {
        EmergencyAlertBanner(node = node, formattedTime = formattedTime)
    } else {
        SafeStatusBanner(node = node, formattedTime = formattedTime)
    }

    // Hardware Telemetry Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📡 Signal Finder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Proximity indicator text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Proximity Status:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = proximityInfo.label,
                    color = proximityInfo.color,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            // Visual segment gauge
            ProximityGauge(level = proximityInfo.level, color = proximityInfo.color)

            // Guidance text
            Text(
                text = proximityInfo.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Proximity tip box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = "Walk around. If the bar fills up, you are walking in the right direction. If it drops, turn back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Expandable Technical Diagnostics Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAdvanced) "▼ Hide Diagnostic Data" else "▶ Show Diagnostic Data",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device Name:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(node.deviceName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MAC Address:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(node.deviceAddress, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Raw Signal (RSSI):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${node.rssi} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Ping Received:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(formattedTime, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyAlertBanner(node: ScoutNodeAlert, formattedTime: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "alertGlow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, Color(0xFFD50000), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE).copy(alpha = alphaAnim)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🚨 CRITICAL EMERGENCY",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFD50000)
            )

            Text(
                text = "FALL / LANDSLIDE DETECTED",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB71C1C)
            )

            Text(
                text = "ScoutNode is actively broadcasting emergency conditions. Speaker Node buzzer has been activated via ESP-NOW.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Detected at $formattedTime",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
private fun SafeStatusBanner(node: ScoutNodeAlert, formattedTime: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "✅ STATUS: SAFE / MONITORING",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            Text(
                text = "No tilt or pressure landslide thresholds exceeded. Normal baseline maintained.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D32)
            )

            Text(
                text = "Last verified at $formattedTime",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF558B2F)
            )
        }
    }
}

@Composable
private fun WaitingForBroadcastCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Text(
                text = "Listening for ScoutNode_J2 broadcast...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ensure the ESP32 ScoutNode is powered on and within Bluetooth range.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PermissionRequestCard(
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bluetooth Permissions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "J2Rescue uses Bluetooth Low Energy to passively detect emergency alert broadcasts from ScoutNode hardware.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

private fun getRssiLabel(rssi: Int): String {
    return when {
        rssi >= -60 -> "Strong"
        rssi >= -75 -> "Good"
        rssi >= -85 -> "Fair"
        else -> "Weak / Far"
    }
}