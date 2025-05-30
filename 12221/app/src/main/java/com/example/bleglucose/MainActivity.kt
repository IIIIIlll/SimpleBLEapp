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
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

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
                                        onClick = { selectedTab = index }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
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
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Device Paired and Connected", Toast.LENGTH_SHORT).show()
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices()
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
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
