package com.example.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.annotation.RequiresPermission

// Your BLEScanner class (assuming it looks something like this based on the crash)
// Make sure this class correctly uses the BluetoothLeScanner instance.
// For this example, let's include a placeholder for it to show how it fits.

class BLEScanner(
    private val bluetoothLeScanner: BluetoothLeScanner, // This is the instance that was previously null
    private val onScanResult: (ScanResult) -> Unit
) {
    private val TAG = "BLEScanner"
    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "Scan result: ${result.device.address} - ${result.device.name}")
            // Call the callback function provided by MainActivity
            onScanResult(result)
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach {
                Log.d(TAG, "Batch scan result: ${it.device.address} - ${it.device.name}")
                // Call the callback function provided by MainActivity
                onScanResult(it)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan Failed: $errorCode")
            // Handle error, e.g., notify user (perhaps via the onScanResult callback or a separate callback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        Log.d(TAG, "Starting BLE scan...")
        // Ensure that bluetoothLeScanner is not null before calling startScan.
        // This check is already implicitly handled by MainActivity's flow.
        bluetoothLeScanner.startScan(scanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        Log.d(TAG, "Stopping BLE scan...")
        // Ensure that bluetoothLeScanner is not null before calling stopScan.
        bluetoothLeScanner.stopScan(scanCallback)
    }
}
