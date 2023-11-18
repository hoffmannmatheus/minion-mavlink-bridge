package com.mhsilva.minioncamera.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothHelper(context: Context, val listener: BluetoothHelperListener) {

    companion object {
        private const val TAG = "BluetoothHelper"

        /**
         * The same UUID used by the BLE device. If this value doesn't match, the device will not
         * be found.
         */
        private val DEVICE_UUID: UUID = UUID.fromString("1a1a3616-e532-4a4c-87b1-19c4f4ec590b")
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // From the previous section:
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var device: BluetoothDevice? = null
    private var isScanning = false

    fun connect() {
        if (isScanning) {
            Log.d(TAG, "already scanning!")
            return
        }
        Log.d(TAG, "starting scan...")
        device = null
        val filter = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString(DEVICE_UUID.toString())
        ).build()
        bleScanner.startScan(listOf(filter), scanSettings, scanCallback)
        isScanning = true
    }

    fun disconnect() {
        if (isScanning) {
            bleScanner.stopScan(scanCallback)
            isScanning = false
        }
        closeSocket()
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            device = result.device
            Log.i(TAG, "Found BLE device! Name: ${device?.name ?: "Unnamed"}, address: $device, rssi: ${result.rssi}")
            disconnect()
            // now connect DAMMIT
            // openSocket()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "onScanFailed with code=$errorCode")
            disconnect()
            listener.onConnectFailed()
        }
    }


    private fun openSocket() {
        device?.let {
            val socket = it.createRfcommSocketToServiceRecord(DEVICE_UUID)
            val connection = SocketThread(socket)
            connection.start()
        }
    }

    private fun closeSocket() {
        //connection?.cancel()
    }

    private inner class SocketThread(val socket: BluetoothSocket) : Thread() {

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            Log.d(TAG, "ConnectThread: will try connecting")

            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect()

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Log.d(TAG, "ConnectThread: calling onSocketConnection")

            val inputStream = socket.inputStream
            //val outputStream = socket.outputStream
            val buffer = ByteArray(1024) // mmBuffer store for the stream
            var byteCount: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                byteCount = try {
                    inputStream.read(buffer)
                } catch (e: IOException) {
                    Log.w(TAG, "Input stream was disconnected", e)
                    break
                }

                // TODO send this data
                Log.i(TAG, "Input stream read $byteCount bytes: ${String(buffer)}")
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}
