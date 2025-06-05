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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bleglucose.ui.theme.BleglucoseTheme
import kotlinx.coroutines.*

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var scanning by mutableStateOf(false)
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        promptEnableBluetooth()

        setContent {
            BleglucoseTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                if (showSplash) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Welcome to BLE Glucose!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Define screens for bottom nav
                    val bottomNavItems = listOf(
                        BottomNavItem("Demo", Icons.Default.Tv) { DemoScreen() },
                        BottomNavItem("Test", Icons.Default.Science) { TestScreen() },
                        BottomNavItem("Scan", Icons.Default.Wifi) {
                            ScanScreen(
                                scanning = scanning,
                                onScanToggle = { toggleScan() }
                            )
                        },
                        BottomNavItem("Configure", Icons.Default.Build) { ConfigureScreen() },
                        BottomNavItem("Settings", Icons.Default.Settings) { SettingsScreen() }
                    )
                    var selectedIndex by remember { mutableStateOf(2) } // Default to Scan

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                bottomNavItems.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = selectedIndex == index,
                                        onClick = {
                                            // Always stop scan when leaving Scan tab
                                            if (selectedIndex == 2 && scanning) stopBLEScan()
                                            selectedIndex = index
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            bottomNavItems[selectedIndex].screen()
                        }
                    }
                }
            }
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
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
        if (scanning) stopBLEScan()
    }

    // ======== Screens =========

    @Composable
    fun DemoScreen() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Demo Screen")
        }
    }

    @Composable
    fun TestScreen() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Test Screen")
        }
    }

    @Composable
    fun ScanScreen(
        scanning: Boolean,
        onScanToggle: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (scanning) "Scanning for BLE devices..." else "Tap to start scanning")
            Spacer(Modifier.height(24.dp))
            ExtendedFloatingActionButton(
                onClick = onScanToggle,
                containerColor = if (scanning) Color.Red else Color.Green
            ) {
                Text(if (scanning) "Stop Scanning" else "Start Scan")
            }
        }
    }

    @Composable
    fun ConfigureScreen() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Configure Screen")
        }
    }

    @Composable
    fun SettingsScreen() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Settings Screen")
        }
    }

    // ====== BLE logic unchanged ======

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
