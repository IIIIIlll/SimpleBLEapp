package com.example.bleglucose

import android.Manifest
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class DeviceActivity : AppCompatActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private var glucoseCharacteristic: BluetoothGattCharacteristic? = null
    private lateinit var tvStatus: TextView
    private lateinit var btnStartGraph: Button
    private var lastValue: Float = 0f

    private val GLUCOSE_SERVICE_UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb")
    private val GLUCOSE_MEASUREMENT_UUID = UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb")

    private val REQUEST_BLUETOOTH_CONNECT = 100
    private val REQUEST_ENABLE_BLUETOOTH = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        tvStatus = findViewById(R.id.tv_status)
        btnStartGraph = findViewById(R.id.btn_start_graph)

        if (!isBluetoothOn()) {
            showBluetoothPrompt()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_CONNECT
            )
        } else {
            startBluetoothConnection()
        }

        btnStartGraph.setOnClickListener {
            val intent = Intent(this, GraphActivity::class.java)
            intent.putExtra("value", lastValue)
            startActivity(intent)
        }
    }

    private fun isBluetoothOn(): Boolean {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        return adapter?.isEnabled == true
    }

    private fun showBluetoothPrompt() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Bluetooth Required")
            .setMessage("This app needs Bluetooth to connect to your device. Please turn on Bluetooth.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH)
            }
            .setNegativeButton("Cancel") { _, _ ->
                tvStatus.text = "Bluetooth is required. Please enable it in settings."
            }
            .create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // When returning from Bluetooth settings, check again
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (isBluetoothOn()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startBluetoothConnection()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_CONNECT
                    )
                }
            } else {
                tvStatus.text = "Bluetooth is still off. Please enable it."
            }
        }
    }

    private fun startBluetoothConnection() {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("device", BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<BluetoothDevice>("device")
        }

        if (device == null) {
            tvStatus.text = "No device provided to connect."
            return
        }

        tvStatus.text = "Connecting to: ${device.name ?: device.address}"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } catch (e: SecurityException) {
                tvStatus.text = "Bluetooth connect not allowed by system"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothConnection()
            } else {
                tvStatus.text = "Bluetooth permission denied."
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread { tvStatus.text = "Connected! Discovering services..." }
                try {
                    if (ContextCompat.checkSelfPermission(this@DeviceActivity, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        gatt.discoverServices()
                    }
                } catch (e: SecurityException) {
                    runOnUiThread { tvStatus.text = "Discover services not allowed by system" }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread { tvStatus.text = "Disconnected" }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (ContextCompat.checkSelfPermission(this@DeviceActivity, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val service = gatt.getService(GLUCOSE_SERVICE_UUID)
                    glucoseCharacteristic = service?.getCharacteristic(GLUCOSE_MEASUREMENT_UUID)
                    if (glucoseCharacteristic != null) {
                        try {
                            gatt.setCharacteristicNotification(glucoseCharacteristic, true)
                            runOnUiThread { tvStatus.text = "Ready to receive data..." }
                        } catch (e: SecurityException) {
                            runOnUiThread { tvStatus.text = "Notification not allowed by system" }
                        }
                    } else {
                        runOnUiThread { tvStatus.text = "Glucose characteristic not found." }
                    }
                }
            } catch (e: SecurityException) {
                runOnUiThread { tvStatus.text = "Service discovery not allowed by system" }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == GLUCOSE_MEASUREMENT_UUID) {
                val data = characteristic.value
                val value = data[0].toInt() and 0xFF
                lastValue = value.toFloat()
                runOnUiThread { tvStatus.text = "Glucose: $lastValue mg/dL" }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothGatt?.close()
        } catch (_: SecurityException) {}
    }
}
