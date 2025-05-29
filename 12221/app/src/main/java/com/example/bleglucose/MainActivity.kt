package com.example.bleglucose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bleglucose.ui.theme.BleglucoseTheme

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val bonded: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BleglucoseTheme {
                MainScreen()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val tabs = listOf("SCANNER", "GRAPH", "CONNECTIONS")
    var selectedTab by remember { mutableIntStateOf(0) }

    val devices = listOf(
        BleDevice("Device A", "00:11:22:33:44:55", -43, false),
        BleDevice("DAMP155C", "15:53:7A:8E:21:56", -60, false),
        BleDevice(null, "FA:1A:22:AB:CD:EF", -75, true)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Devices") },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Filled.Bluetooth, contentDescription = null) }
                    IconButton(onClick = { }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Stop scanning logic */ },
                containerColor = Color.Red
            ) {
                Text("Stop Scanning")
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, text ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text) }
                    )
                }
            }
            when (selectedTab) {
                0 -> BleDeviceList(devices)
                1 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Graph Page") }
                2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Active Connections") }
            }
        }
    }
}

@Composable
fun BleDeviceList(devices: List<BleDevice>) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            BleDeviceCard(device)
        }
    }
}

@Composable
fun BleDeviceCard(device: BleDevice) {
    Card(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.name ?: "N/A",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text("RSSI: ${device.rssi} dBm")
                Text("Address: ${device.address}")
                Text(if (device.bonded) "Bonded" else "Not bonded")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { /* Connect logic */ },
                enabled = !device.bonded
            ) {
                Text(if (device.bonded) "Connected" else "CONNECT")
            }
        }
    }
}
