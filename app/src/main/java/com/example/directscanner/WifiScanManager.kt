package com.example.directscanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Scan cadence presets. NOTE: Android itself throttles Wi-Fi scan requests
 *  for apps (roughly 4 per 2 minutes since Android 9), so TURBO's actual
 *  refresh rate may be capped by the OS on some devices — see README. */
enum class ScanSpeed(val intervalMs: Long, val label: String) {
    BATTERY_SAVER(30_000L, "Battery Saver"),
    NORMAL(10_000L, "Normal"),
    TURBO(4_000L, "Turbo")
}

data class DetectedNetwork(
    val ssid: String,
    val rssi: Int,   // dBm; closer to 0 = stronger/closer signal
    val bssid: String
)

/** Raw numbers from the most recent scan, for the in-app diagnostics
 *  readout — lets us tell "scan is returning nothing at all" apart from
 *  "scan sees networks but none match" apart from "matches, but hidden." */
data class ScanDebugInfo(
    val totalSeen: Int = 0,
    val matchedDirect: Int = 0,
    val shown: Int = 0,
    val lastScanAt: Long = 0L,
    val lastError: String? = null
)

class WifiScanManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val appPreferences: AppPreferences
) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _detectedNetworks = MutableStateFlow<List<DetectedNetwork>>(emptyList())
    val detectedNetworks: StateFlow<List<DetectedNetwork>> = _detectedNetworks

    // Fires once per SSID, the moment it's first seen this session — this is
    // what drives the instant notification, separate from the full list above.
    private val _newNetworkEvents = MutableSharedFlow<DetectedNetwork>(extraBufferCapacity = 8)
    val newNetworkEvents: SharedFlow<DetectedNetwork> = _newNetworkEvents
    private val seenSsids = mutableSetOf<String>()

    private val _debugInfo = MutableStateFlow(ScanDebugInfo())
    val debugInfo: StateFlow<ScanDebugInfo> = _debugInfo

    private var loopJob: Job? = null
    private var receiverRegistered = false

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            refreshFromLastScan()
        }
    }

    private fun refreshFromLastScan() {
        try {
            val ignored = appPreferences.ignoredSsids
            val results = wifiManager.scanResults
            val matched = results.filter { isTargetNetwork(it.SSID) }
            val filtered = matched
                .filter { it.SSID !in ignored }
                .map { DetectedNetwork(ssid = it.SSID, rssi = it.level, bssid = it.BSSID) }
                .sortedByDescending { it.rssi }
                .distinctBy { it.ssid } // keep the strongest reading per SSID
            _detectedNetworks.value = filtered

            _debugInfo.value = ScanDebugInfo(
                totalSeen = results.size,
                matchedDirect = matched.map { it.SSID }.distinct().size,
                shown = filtered.size,
                lastScanAt = System.currentTimeMillis()
            )

            filtered.forEach { network ->
                if (seenSsids.add(network.ssid)) {
                    _newNetworkEvents.tryEmit(network)
                }
            }
        } catch (se: SecurityException) {
            Log.e("WifiScanManager", "Missing permission to read scan results", se)
            _debugInfo.value = _debugInfo.value.copy(lastError = "Permission error: ${se.message}")
        }
    }

    /** Re-filters the most recent scan results immediately (no new radio
     *  scan triggered) — used right after the ignore list changes so the
     *  grid updates without waiting for the next scan cycle. */
    fun reapplyIgnoreFilter() = refreshFromLastScan()

    fun ignoreNetwork(ssid: String) {
        appPreferences.ignore(ssid)
        reapplyIgnoreFilter()
    }

    fun unignoreNetwork(ssid: String) {
        appPreferences.unignore(ssid)
        // Treat it as unseen again so it can re-notify once it reappears.
        seenSsids.remove(ssid)
        reapplyIgnoreFilter()
    }

    fun start(speed: ScanSpeed) {
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                context,
                scanReceiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
        loopJob?.cancel()
        loopJob = scope.launch {
            while (true) {
                @Suppress("DEPRECATION")
                val started = wifiManager.startScan()
                if (!started) {
                    // Request was throttled/failed; still show latest cached results.
                    refreshFromLastScan()
                }
                delay(speed.intervalMs)
            }
        }
    }

    fun setSpeed(speed: ScanSpeed) = start(speed)

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        if (receiverRegistered) {
            context.unregisterReceiver(scanReceiver)
            receiverRegistered = false
        }
    }
}
