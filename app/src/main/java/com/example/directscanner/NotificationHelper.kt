package com.example.directscanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "direct_network_alerts"
private var notificationIdCounter = 1000

/** Posts an immediate, high-priority alert the moment a new marked
 *  network shows up. Respects the user's notifications on/off setting
 *  (checked by the caller) and silently no-ops if POST_NOTIFICATIONS
 *  hasn't been granted. */
object NotificationHelper {

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nearby DIRECT network alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts the instant a marked DIRECT- printer-style network is detected nearby."
                enableLights(true)
                lightColor = AndroidColor.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 150, 200, 150, 400)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyNewNetwork(context: Context, network: DetectedNetwork) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setColor(AndroidColor.RED)
            .setContentTitle("DIRECT network detected")
            .setContentText(network.ssid)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter++, notification)
        } catch (se: SecurityException) {
            // POST_NOTIFICATIONS not granted; nothing to do.
        }
    }
}
