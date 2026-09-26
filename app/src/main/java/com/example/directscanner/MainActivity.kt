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
    private val colorAssigner = ColorAssigner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanManager = WifiScanManager(this, lifecycleScope)

        setContent {
            var hasPermission by remember { mutableStateOf(hasRequiredPermissions()) }
            var currentSpeed by remember { mutableStateOf(ScanSpeed.NORMAL) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                hasPermission = results.values.all { it }
                if (hasPermission) scanManager.start(currentSpeed)
            }

            LaunchedEffect(Unit) {
                if (hasPermission) {
                    scanManager.start(currentSpeed)
                } else {
                    permissionLauncher.launch(requiredPermissions())
                }
            }

            val networks by scanManager.detectedNetworks.collectAsStateWithLifecycle()

            MainScreen(
                hasPermission = hasPermission,
                networks = networks,
                colorAssigner = colorAssigner,
                currentSpeed = currentSpeed,
                onSpeedChange = {
                    currentSpeed = it
                    scanManager.setSpeed(it)
                },
                onRequestPermission = { permissionLauncher.launch(requiredPermissions()) }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanManager.stop()
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun hasRequiredPermissions(): Boolean =
        requiredPermissions().all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
}
