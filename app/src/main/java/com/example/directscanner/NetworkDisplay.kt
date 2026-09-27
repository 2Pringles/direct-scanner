package com.example.directscanner

/** Generic brand / product-line words that show up in these SSIDs but
 *  don't actually distinguish one network from another, so they're
 *  stripped out and only the meaningful remainder is shown. */
private val GENERIC_TERMS = setOf(
    "HP", "CANON", "EPSON", "BROTHER", "LEXMARK", "XEROX", "KYOCERA",
    "RICOH", "SAMSUNG", "DELL", "KONICA", "MINOLTA",
    "PRINTER", "PRINT", "INKJET", "LASERJET", "DESKJET", "OFFICEJET",
    "COLOR", "COLOUR", "SERIES", "SCAN", "SCANNER", "WIRELESS", "SMART",
    "PHOTO", "ALLINONE"
)

/**
 * Reduces a full SSID down to just its distinguishing remainder. Handles
 * the naming shapes actually seen in the wild:
 *  - "DIRECT-0F-HP Color LJ 3301"       -> "LJ 3301"
 *  - "DIRECT-fm-EPSON-SC-F500 Series"   -> "SC F500"
 *  - "DIRECT-zHKMA0OE75" (no brand/model info at all) -> "zHKMA0OE75"
 *
 * Strips the "DIRECT-" prefix and, if present, the short random code
 * right after it, splits the rest on hyphens/spaces, then drops any
 * token that's a known generic brand/product word. Whatever's left
 * (normally the model name/number) is shown; if nothing meaningful is
 * left to strip (as with a bare random code), the remainder is shown as-is.
 */
fun shortLabel(ssid: String): String {
    var rest = ssid.removePrefix("DIRECT-")
    rest = rest.replaceFirst(Regex("^[A-Za-z0-9]{1,4}-"), "").trim()
    if (rest.isEmpty()) return ssid

    val tokens = rest.split(Regex("[-\\s]+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return rest

    val filtered = tokens.filterNot { it.uppercase() in GENERIC_TERMS }
    val kept = filtered.ifEmpty { tokens }

    return kept.joinToString(" ")
}

/**
 * Very rough distance estimate from signal strength, using the standard
 * log-distance path-loss approximation. This is a guess, not a
 * measurement -- RSSI is heavily affected by walls, interference and the
 * specific radio hardware involved -- but a rough range beats nothing.
 */
fun estimateRangeFeet(rssi: Int): Int {
    val txPowerAt1m = -50.0      // assumed signal strength at 1 meter
    val pathLossExponent = 2.5   // typical indoor value
    val meters = Math.pow(10.0, (txPowerAt1m - rssi) / (10.0 * pathLossExponent))
    val feet = meters * 3.28084
    return feet.coerceIn(1.0, 500.0).toInt()
}
