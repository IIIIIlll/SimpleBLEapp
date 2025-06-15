package com.example.bleglucose

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ScanActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var toggleButton: Button
    private var scanCallback: ScanCallback? = null
    private var scanning = false
    private val permissionRequestCode = 1001

    private lateinit var notificationManager: NotificationManager
    private val notificationId = 101
    private val channelId = "glucose_monitor_critical_channel"
    private var isBluetoothEnabled = false
    private var isConnected = false

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    isBluetoothEnabled = state == BluetoothAdapter.STATE_ON
                    if (!isBluetoothEnabled) {
                        showCriticalBluetoothNotification()
                        Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
                    } else {
                        updateNormalBluetoothNotification()
                    }
                }
                ACTION_CONNECTION_UPDATE -> {
                    isConnected = intent.getBooleanExtra(EXTRA_IS_CONNECTED, false)
                    updateNormalBluetoothNotification()
                }
            }
        }
    }

    @SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createCriticalNotificationChannel()

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true

        listView = findViewById(R.id.device_list)
        toggleButton = findViewById(R.id.scan_toggle)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!hasPermissions()) {
            requestPermissions()
        }

        toggleButton.setOnClickListener {
            if (scanning) {
                stopScanning()
            } else if (hasPermissions()) {
                startScanning()
            } else {
                requestPermissions()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            val intent = Intent(this, DeviceActivity::class.java).apply {
                putExtra("device_address", device.address)
                putExtra(EXTRA_IS_CONNECTED, true)
            }
            startActivity(intent)

            isConnected = true
            updateNormalBluetoothNotification()
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ACTION_CONNECTION_UPDATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothStateReceiver, filter)
        }

        if (!isBluetoothEnabled) {
            showCriticalBluetoothNotification()
        } else {
            updateNormalBluetoothNotification()
        }
    }

    private fun createCriticalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Critical Glucose Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Bluetooth is off"
                enableVibration(true)
                lightColor = Color.RED
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("NotificationPermission")
    private fun showCriticalBluetoothNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("‼️ BLUETOOTH REQUIRED")
            .setContentText("Turn on Bluetooth to monitor glucose")
            .setSmallIcon(R.drawable.ic_bluetooth_status)
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun updateNormalBluetoothNotification() {
        val (title, text, color) = when {
            isConnected -> Triple(
                "✓ Glucose Monitor Active",
                "Receiving data...",
                Color.GREEN
            )
            else -> Triple(
                "Bluetooth Ready",
                "Scanning for glucose devices...",
                Color.BLUE
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_bluetooth_status)
            .setColor(color)
            .setContentIntent(createPendingIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, ScanActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasPermissions(): Boolean {
        val permissions = requiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
            )
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions(), permissionRequestCode)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScanning() {
        adapter.clear()
        deviceList.clear()

        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        }

        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: return

                if (name.contains("glucose", true) || name.contains("dexcom", true) || name.contains("libre", true)) {
                    if (deviceList.none { it.address == device.address }) {
                        deviceList.add(device)
                        adapter.add("$name\n${device.address}")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@ScanActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothLeScanner?.startScan(scanCallback)
        scanning = true
        toggleButton.text = "Stop Scan"
        updateNormalBluetoothNotification()
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        scanCallback?.let { bluetoothLeScanner?.stopScan(it) }
        scanning = false
        toggleButton.text = "Start Scan"
        updateNormalBluetoothNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        notificationManager.cancel(notificationId)
        if (scanning) stopScanning()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startScanning()
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    companion object {
        const val ACTION_CONNECTION_UPDATE = "com.example.bleglucose.CONNECTION_UPDATE"
        const val EXTRA_IS_CONNECTED = "is_connected"
    }
}
