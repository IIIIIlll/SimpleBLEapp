package com.example.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val notificationHelper = NotificationHelper(context)

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    notificationHelper.showBluetoothStatusNotification(true)
                }
                BluetoothAdapter.STATE_OFF -> {
                    notificationHelper.showBluetoothStatusNotification(false)
                }
            }
        }
    }
}
