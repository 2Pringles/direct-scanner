package com.example.directscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.directscanner.ui.MainScreen

class MainActivity : ComponentActivity() {

    private lateinit var scanManager: WifiScanManager
    private lateinit var appPreferences: AppPreferences
    private val colorAssigner = ColorAssigner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanManager = WifiScanManager(this, lifecycleScope)
        appPreferences = AppPreferences(this)
        NotificationHelper.ensureChannel(this)

        setContent {
            var hasPermission by remember { mutableStateOf(hasWifiPermission()) }
            var currentSpeed by remember { mutableStateOf(ScanSpeed.NORMAL) }
            var notificationsEnabled by remember { mutableStateOf(appPreferences.notificationsEnabled) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                hasPermission = hasWifiPermission()
                if (hasPermission) scanManager.start(currentSpeed)
            }

            // Ask for Wi-Fi (and, on Android 13+, notification) permission up front.
            LaunchedEffect(Unit) {
                if (hasPermission) {
                    scanManager.start(currentSpeed)
                } else {
                    permissionLauncher.launch(allPermissions())
                }
            }

            // Fire an instant notification the moment a *new* network is first seen,
            // as long as the user hasn't turned alerts off.
            LaunchedEffect(Unit) {
                scanManager.newNetworkEvents.collect { network ->
                    if (appPreferences.notificationsEnabled) {
                        NotificationHelper.notifyNewNetwork(this@MainActivity, network)
                    }
                }
            }

            val networks by scanManager.detectedNetworks.collectAsStateWithLifecycle()

            MainScreen(
                hasPermission = hasPermission,
                networks = networks,
                colorAssigner = colorAssigner,
                currentSpeed = currentSpeed,
                notificationsEnabled = notificationsEnabled,
                onSpeedChange = {
                    currentSpeed = it
                    scanManager.setSpeed(it)
                },
                onNotificationsToggle = { enabled ->
                    notificationsEnabled = enabled
                    appPreferences.notificationsEnabled = enabled
                },
                onRequestPermission = { permissionLauncher.launch(allPermissions()) }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanManager.stop()
    }

    private fun wifiPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun notificationPermission(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    private fun allPermissions(): Array<String> = wifiPermissions() + notificationPermission()

    private fun hasWifiPermission(): Boolean =
        wifiPermissions().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}
