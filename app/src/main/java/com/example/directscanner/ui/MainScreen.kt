package com.example.directscanner.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.directscanner.ColorAssigner
import com.example.directscanner.DetectedNetwork
import com.example.directscanner.ScanSpeed
import com.example.directscanner.hslToColor
import kotlin.math.ceil
import kotlin.math.sqrt

@Composable
fun MainScreen(
    hasPermission: Boolean,
    networks: List<DetectedNetwork>,
    colorAssigner: ColorAssigner,
    currentSpeed: ScanSpeed,
    notificationsEnabled: Boolean,
    onSpeedChange: (ScanSpeed) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Header(
                hasPermission = hasPermission,
                count = networks.size,
                currentSpeed = currentSpeed,
                notificationsEnabled = notificationsEnabled,
                onSpeedChange = onSpeedChange,
                onNotificationsToggle = onNotificationsToggle,
                onRequestPermission = onRequestPermission
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !hasPermission -> CenteredMessage("Wi-Fi scan permission is required to detect nearby networks.")
                    networks.isEmpty() -> CenteredMessage("No marked networks detected nearby.", dim = true)
                    else -> NetworkGrid(networks, colorAssigner)
                }
            }
        }
    }
}

@Composable
private fun Header(
    hasPermission: Boolean,
    count: Int,
    currentSpeed: ScanSpeed,
    notificationsEnabled: Boolean,
    onSpeedChange: (ScanSpeed) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    Column {
        Text(
            text = "DIRECT NETWORKS: $count",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScanSpeed.entries.forEach { speed ->
                    if (speed == currentSpeed) {
                        Button(onClick = { onSpeedChange(speed) }) {
                            Text(speed.label, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(onClick = { onSpeedChange(speed) }) {
                            Text(speed.label, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Alerts", color = Color.White, fontSize = 12.sp)
                Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsToggle)
            }
        }
        if (!hasPermission) {
            Spacer(Modifier.height(6.dp))
            Button(onClick = onRequestPermission) { Text("Grant permission") }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, dim: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = if (dim) Color.Gray else Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}

/**
 * Lays every detected network out in a fixed rows x columns grid sized so
 * ALL of them are visible at once (no scrolling), regardless of how many
 * are found. Each cell always exists; the card drawn inside it grows and
 * gets bigger text the closer that network is.
 */
@Composable
private fun NetworkGrid(networks: List<DetectedNetwork>, colorAssigner: ColorAssigner) {
    val count = networks.size
    val columns = ceil(sqrt(count.toFloat())).toInt().coerceAtLeast(1)
    val rows = ceil(count / columns.toFloat()).toInt().coerceAtLeast(1)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until columns) {
                    val index = r * columns + c
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (index < count) {
                            NetworkCard(networks[index], colorAssigner)
                        }
                    }
                }
            }
        }
    }
}

/** Maps RSSI (~ -95 dBm far .. -35 dBm very close) to a 0f..1f proximity value. */
private fun proximityFraction(rssi: Int): Float {
    val clamped = rssi.coerceIn(-95, -35)
    return (clamped + 95) / 60f
}

/** A continuously alternating red<->blue color, like a police light bar. */
@Composable
private fun rememberSirenColor(periodMillis: Int = 400): Color {
    val infiniteTransition = rememberInfiniteTransition(label = "siren")
    val fraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sirenFraction"
    )
    return lerp(Color(0xFFFF0000), Color(0xFF0040FF), fraction)
}

@Composable
private fun NetworkCard(network: DetectedNetwork, colorAssigner: ColorAssigner) {
    val proximity = proximityFraction(network.rssi)          // 0 far .. 1 close
    val fillFraction = 0.45f + proximity * 0.55f              // bigger box when closer
    val sirenColor = rememberSirenColor()
    val cardColor = sirenColor.copy(alpha = 0.55f + proximity * 0.45f) // dimmer flash when far

    // A distinct, stable border color per SSID, so networks stay tellable
    // apart at a glance even while every box is flashing red/blue.
    val borderHue = colorAssigner.hueFor(network.ssid)
    val borderColor = hslToColor(hue = borderHue, saturation = 0.9f, lightness = 0.65f)
    val borderWidth = (3 + proximity * 4).dp

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(fillFraction)
                .clip(RoundedCornerShape(16.dp))
                .background(cardColor)
                .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = network.ssid,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (15 + proximity * 11).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${network.rssi} dBm",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
