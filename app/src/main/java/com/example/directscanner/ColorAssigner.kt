package com.example.directscanner

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private const val GOLDEN_ANGLE_DEGREES = 137.508f

/**
 * Assigns each newly-discovered SSID a distinct, stable hue (using the
 * golden-angle rotation, which keeps consecutive hues visually far apart
 * no matter how many networks are found) so the user can tell networks
 * apart at a glance across scans.
 */
class ColorAssigner {
    private val hues = LinkedHashMap<String, Float>()

    fun hueFor(ssid: String): Float = hues.getOrPut(ssid) {
        (hues.size * GOLDEN_ANGLE_DEGREES) % 360f
    }
}

/**
 * Manual HSL -> RGB conversion (hue in degrees 0-360, saturation/lightness
 * in 0f..1f). Written out explicitly rather than relying on a platform HSL
 * API, so it behaves the same across Compose/Android versions.
 */
fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = r1 + m, green = g1 + m, blue = b1 + m)
}
