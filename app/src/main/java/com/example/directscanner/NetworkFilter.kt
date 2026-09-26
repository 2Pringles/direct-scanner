package com.example.directscanner

/**
 * Keywords that identify a "DIRECT-" SSID as printer-style Wi-Fi Direct
 * broadcast (as opposed to some other DIRECT-prefixed device, like a TV
 * or a phone hotspot). Add brand/model terms here as needed.
 */
private val PRINTER_KEYWORDS = listOf(
    "PRINTER", "PRINT", "HP", "CANON", "EPSON", "BROTHER", "LEXMARK",
    "INKJET", "LASERJET", "DESKJET", "OFFICEJET", "XEROX", "KYOCERA",
    "RICOH", "SAMSUNG"
)

/**
 * Returns true only if [ssid]:
 *  1. Starts with the literal, all-caps prefix "DIRECT-" (case-sensitive —
 *     a lowercase or mixed-case "direct-" does NOT match), and
 *  2. Contains printer-style branding/model lingo somewhere after the prefix.
 */
fun isTargetNetwork(ssid: String?): Boolean {
    if (ssid.isNullOrBlank()) return false
    if (!ssid.startsWith("DIRECT-")) return false
    val upper = ssid.uppercase()
    return PRINTER_KEYWORDS.any { upper.contains(it) }
}
