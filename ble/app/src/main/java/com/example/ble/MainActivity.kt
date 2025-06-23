package com.example.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bleScanner: BLEScanner? = null
    private val scanResults = mutableListOf<android.bluetooth.le.ScanResult>()
    private lateinit var deviceAdapter: BLEDeviceAdapter
    private lateinit var notificationHelper: NotificationHelper
    private val bluetoothStateReceiver = BluetoothStateReceiver()
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                checkBluetoothStateAndPermissions()
            } else {
                Toast.makeText(this, "Permissions required.", Toast.LENGTH_LONG).show()
                binding.scanButton.isEnabled = false
            }
        }

    private val requestBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                checkBluetoothStateAndPermissions()
            } else {
                Toast.makeText(this, "Bluetooth is required.", Toast.LENGTH_SHORT).show()
                binding.scanButton.isEnabled = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth.", Toast.LENGTH_LONG).show()
            binding.scanButton.isEnabled = false
            return
        }

        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        deviceAdapter = BLEDeviceAdapter(scanResults)
        binding.scanResultsList.adapter = deviceAdapter

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothStateReceiver, filter)
        }

        checkBluetoothStateAndPermissions()

        binding.scanButton.setOnClickListener {
            checkBluetoothStateAndPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        checkBluetoothStateAndPermissions()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        bleScanner?.stopScan()
    }

    private fun checkBluetoothStateAndPermissions() {
        val adapter = bluetoothAdapter ?: return

        if (!adapter.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionsLauncher.launch(requiredPermissions)
                return
            }

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetoothLauncher.launch(enableBtIntent)
            binding.scanButton.isEnabled = false
            return
        }

        if (!checkAllPermissionsGranted()) {
            requestPermissionsLauncher.launch(requiredPermissions)
            return
        }

        initializeBleScanner()
        startBleScan()
    }

    private fun checkAllPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initializeBleScanner() {
        val leScanner = bluetoothAdapter?.bluetoothLeScanner
        if (leScanner != null && bleScanner == null) {
            bleScanner = BLEScanner(leScanner) { result ->
                if (!scanResults.any { it.device.address == result.device.address }) {
                    scanResults.add(result)
                    deviceAdapter.notifyDataSetChanged()
                }
            }
        }
        binding.scanButton.isEnabled = true
        notificationHelper.showBluetoothStatusNotification(true)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startBleScan() {
        scanResults.clear()
        deviceAdapter.notifyDataSetChanged()

        val scanner = bleScanner
        val adapter = bluetoothAdapter

        if (scanner != null && adapter != null && adapter.isEnabled && checkAllPermissionsGranted()) {
            scanner.startScan()
            Toast.makeText(this, "Scan started.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Cannot scan, checking setup...", Toast.LENGTH_SHORT).show()
            checkBluetoothStateAndPermissions()
        }
    }
}
