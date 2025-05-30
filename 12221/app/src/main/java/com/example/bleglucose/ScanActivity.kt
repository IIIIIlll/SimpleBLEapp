package com.example.bleglucose

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ScanActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var toggleButton: Button
    private val permissionRequestCode = 1001
    private var scanning = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                device?.let {
                    if (!deviceList.contains(it)) {
                        deviceList.add(it)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            ContextCompat.checkSelfPermission(
                                this@ScanActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val deviceInfo = """
                                |${it.name ?: "N/A"}
                                |RSSI: $rssi dBm
                                |Address: ${it.address}
                                |${if (it.bondState == BluetoothDevice.BOND_BONDED) "Bonded" else "Not bonded"}
                            """.trimMargin()
                            adapter.add(deviceInfo)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

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
            } else {
                if (hasPermissions()) {
                    startScanning()
                } else {
                    requestPermissions()
                }
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            val intent = Intent(this, DeviceActivity::class.java)
            intent.putExtra("device", device)
            startActivity(intent)
        }
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
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions(), permissionRequestCode)
    }

    private fun startScanning() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.startDiscovery()
            scanning = true
            toggleButton.text = "Stop Scanning"
        }
    }

    private fun stopScanning() {
        unregisterReceiver(receiver)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.cancelDiscovery()
        }
        scanning = false
        toggleButton.text = "Start Scanning"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (scanning) stopScanning()
    }

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
}
