package com.example.bleglucose

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ScanActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var toggleButton: Button
    private val permissionRequestCode = 1001
    private var scanning = false

    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

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

        toggleButton.setOnClickListener @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN) {
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScanning() {
        // Reset list
        adapter.clear()
        deviceList.clear()

        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        }

        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                // Flexible filtering, adjust or remove as needed
                val name = device.name ?: ""
                val matches = name.contains("glucose", true)
                        || name.contains("cgm", true)
                        || name.contains("dexcom", true)
                        || name.contains("libre", true)
                        || name.contains("abbott", true)
                        || name.contains("medtronic", true)
                        || name.isNotEmpty() // Show any named device for debugging
                val isNotAlreadyListed = deviceList.none { d -> d.address == device.address }

                if (matches && isNotAlreadyListed && rssi > -90) {
                    deviceList.add(device)
                    val deviceInfo = """
                        |${device.name ?: "N/A"}
                        |RSSI: $rssi dBm
                        |Address: ${device.address}
                        |${if (device.bondState == BluetoothDevice.BOND_BONDED) "Bonded" else "Not bonded"}
                    """.trimMargin()
                    adapter.add(deviceInfo)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@ScanActivity, "BLE Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothLeScanner?.startScan(scanCallback)
        scanning = true
        toggleButton.text = "Stop Scanning"
    }

    private fun stopScanning() {
        scanCallback?.let { if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
            bluetoothLeScanner?.stopScan(it) }
        scanning = false
        toggleButton.text = "Start Scanning"
    }

    override fun onDestroy() {
        super.onDestroy()
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
}
