package com.example.bleglucose

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bleglucose.ui.theme.BleglucoseTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var scanning by mutableStateOf(false)
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use BluetoothManager for modern API
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Prompt the user to enable Bluetooth if it is off
        promptEnableBluetooth()

        setContent {
            BleglucoseTheme {
                val tabs = listOf("SCANNER", "GRAPH", "CONNECTIONS")
                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(title = { Text("BLE Devices") })
                            TabRow(selectedTabIndex = selectedTab) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        text = { Text(title) },
                                        selected = selectedTab == index,
                                        onClick = {
                                            // Always stop scan when leaving SCANNER tab
                                            if (selectedTab == 0 && scanning) stopBLEScan()
                                            selectedTab = index
                                        }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (selectedTab == 0) {
                            ExtendedFloatingActionButton(
                                onClick = { toggleScan() },
                                containerColor = if (scanning) Color.Red else Color.Green
                            ) {
                                Text(if (scanning) "Stop Scanning" else "Start Scan")
                            }
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        0 -> ScannerScreen(modifier = Modifier.padding(padding))
                        1 -> GraphScreen(modifier = Modifier.padding(padding))
                        2 -> ConnectionsScreen(modifier = Modifier.padding(padding))
                    }
                }
            }
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            // Check permission for Android 12 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                // Optionally, request permission here or show a message
                Toast.makeText(this, "Bluetooth Connect permission required to enable Bluetooth.", Toast.LENGTH_SHORT).show()
                return
            }

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
            Toast.makeText(this, "Bluetooth is required for this app.", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        super.onDestroy()
        // Ensure scan is stopped and clean up
        if (scanning) stopBLEScan()
    }

    @Composable
    fun ScannerScreen(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (scanning) "Scanning for BLE devices..." else "Tap to start scanning")
        }
    }

    @Composable
    fun GraphScreen(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Graph Data Here")
        }
    }

    @Composable
    fun ConnectionsScreen(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Previously connected devices here")
        }
    }

    private fun toggleScan() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions(), PERMISSION_REQUEST_CODE)
            return
        }
        if (!scanning) startBLEScan() else stopBLEScan()
    }

    private fun hasPermissions(): Boolean {
        val permissions = requiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> {
        val basePermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            basePermissions += listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }
        return basePermissions.toTypedArray()
    }

    private fun startBLEScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Bluetooth Scan permission required", Toast.LENGTH_SHORT).show()
            return
        }
        scanning = true
        bluetoothAdapter.bluetoothLeScanner.startScan(leScanCallback)
    }

    private fun stopBLEScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        scanning = false
        bluetoothAdapter.bluetoothLeScanner.stopScan(leScanCallback)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Check permission before using device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            if (result.device.name?.contains("GlucoseDevice") == true) {
                stopBLEScan()
                connectDevice(result.device)
            }
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Bluetooth Connect permission required", Toast.LENGTH_SHORT).show()
            return
        }
        device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Device Paired and Connected", Toast.LENGTH_SHORT).show()
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ActivityCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        gatt.discoverServices()
                    }
                }
            }
            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
            ) {
                val rawData = characteristic.value
                transmitDataToGraph(rawData)
            }
        })
    }

    private fun transmitDataToGraph(rawData: ByteArray) {
        val glucoseValue = rawData.first().toFloat() // Replace with actual conversion logic
        val intent = Intent(this, GraphActivity::class.java).apply {
            putExtra("glucoseValue", glucoseValue)
        }
        startActivity(intent)
    }
}
