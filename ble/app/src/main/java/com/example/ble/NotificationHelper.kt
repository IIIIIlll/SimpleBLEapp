package com.example.ble

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {

    private val channelId = "bluetooth_status_channel"
    private val channelName = "Bluetooth Status"
    private val notificationId = 1

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies about Bluetooth connection state"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBluetoothStatusNotification(isEnabled: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            // *** FIX: Using a standard system icon to prevent crashes ***
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true) // Don't spam the user

        if (isEnabled) {
            builder.setContentTitle("Bluetooth is On")
                .setContentText("BLE scanner is ready to use.")
                .setColor(Color.GREEN)
        } else {
            builder.setContentTitle("Bluetooth is Off")
                .setContentText("Please turn on Bluetooth to scan for devices.")
                .setColor(Color.RED)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Make the "off" notification more prominent
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If permission is not granted, do not show notification.
                // The permission request logic is handled in MainActivity.
                return
            }
            notify(notificationId, builder.build())
        }
    }
}
