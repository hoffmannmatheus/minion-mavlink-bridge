package com.mhsilva.minioncamera.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.mhsilva.minioncamera.utils.isIndicatable
import com.mhsilva.minioncamera.utils.isNotifiable
import com.mhsilva.minioncamera.utils.isReadable
import com.mhsilva.minioncamera.utils.printGattTable
import java.util.UUID

/**
 * Abstracts Bluetooth Low Energy connection complexity by connecting to our BLE device,
 * establishing notifications, and providing data & connection status updates.
 *
 * This is the operation sequence for connecting and establishing notification-based updates with
 * the Bluetooth Characteristic:
 * 1) Scan for BLE devices, filtering by the known UUID (same UUID set in the Arduino).
 * 2) Connect to the device (via GATT).
 * 3) Get the devices Services & Characteristic.
 * 4) Check whether the Characteristic is readable & notifiable.
 * 5) Subscribe to the Characteristic notifications (write the notification GATT descriptor).
 *   - After this step, we can finally consider the connection is "done".
 * 6) Finally, read the latest value of the Characteristic.
 *   - We will not receive a characteristic update notification unless the data changes
 *
 * It is important to note that all of these operations are done only when the previous was
 * confirmed (via BluetoothGattCallback), otherwise we might overwhelm the connection or enter into
 * an unknown/inconsistent state.
 *
 * This implementation is heavily based on this excellent guide: https://punchthrough.com/android-ble-guide/
 */


// TODO: make BluetoothHelper be injectable via Dagger instead, and have LiveData variables for "connectionStatus" and "data" instead of a listener interface.
@SuppressLint("MissingPermission")
class BluetoothHelper(
    private val context: Context,
    private val listener: BluetoothHelperListener,
) {

    companion object {
        private const val TAG = "BluetoothHelper"

        /**
         * The UUID used by our Arduino BLE device.
         */
        private val DEVICE_UUID = UUID.fromString("1a1a3616-e532-4a4c-87b1-19c4f4ec590b")

        /**
         * GATT UUIDs used by the device GATT Service & Characteristics.
         */
        private val GATT_UUID_SERVICE = UUID.fromString("1a1a3616-e532-4a4c-87b1-19c4f4ec590b")
        private val GATT_UUID_CHAR_STATE = UUID.fromString("6af662f3-5393-41ed-af8b-02fafe592177")

        /**
         * Pre-defined UUID for the notification descriptor, used to enable characteristic update
         * notifications.
         */
        private val GATT_NOTIF_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    fun connect() {
        if (isScanning) {
            Log.w(TAG, "already scanning!")
            return
        }
        Log.d(TAG, "starting scan...")
        val filter = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString(DEVICE_UUID.toString())
        ).build()
        bleScanner.startScan(listOf(filter), scanSettings, scanCallback)
        isScanning = true
    }

    fun disconnect(onPurpose: Boolean = false) {
        stopScan()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        if (onPurpose) {
            listener.onDisconnected()
        } else {
            listener.onConnectFailed()
        }
    }

    private fun stopScan() {
        isScanning = false
        bleScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.d(TAG, "Found BLE device! Name: ${device?.name ?: "Unnamed"}, address: $device, rssi: ${result.rssi}")
            if (!isScanning) { // This can happen since socket and scanning happen async
                Log.d(TAG, "Already stopped scan, will ignore this device")
            } else if (device != null) {
                stopScan()
                onDeviceFound(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "onScanFailed with code=$errorCode")
            onConnectionFailed()
        }
    }

    private fun onDeviceFound(device: BluetoothDevice) {
        Log.d("ScanResultAdapter", "Connecting to $device")
        device.connectGatt(context, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun onConnectionComplete(gatt: BluetoothGatt?) {
        Log.i(TAG, "onConnectionComplete. Will read characteristics last values")
        readCharacteristic(gatt)
        listener.onConnected()
    }

    private fun onConnectionFailed() {
        Log.w(TAG, "onConnectionFailed")
        disconnect()
        listener.onConnectFailed()
    }

    private fun readCharacteristic(gatt: BluetoothGatt?) {
        if (gatt == null) {
            Log.e(TAG, "readCharacteristic: Can't read with null gatt!")
            return
        }
        val characteristic = gatt
            .getService(GATT_UUID_SERVICE)
            .getCharacteristic(GATT_UUID_CHAR_STATE)
        if (characteristic?.isReadable() == true) {
            Log.d(TAG, "Requesting read from characteristic $characteristic")
            gatt.readCharacteristic(characteristic)
        }
    }
    private fun enableCharacteristicNotifications() {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.e(TAG, "Not connected")
            return
        }
        val characteristic = gatt
            .getService(GATT_UUID_SERVICE)
            .getCharacteristic(GATT_UUID_CHAR_STATE)
        // First enable notifications
        val enableNotification = gatt.setCharacteristicNotification(characteristic, true)
        if (!enableNotification) {
            Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
            return
        }
        // Then write the descriptor payload
        characteristic.getDescriptor(GATT_NOTIF_DESCRIPTOR_UUID)?.let { notifyDescriptor ->
            // Either notification or indication works
            val payload = when {
                characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                else -> {
                    Log.e(TAG, "${characteristic.uuid} doesn't support notifications/indications")
                    return
                }
            }
            writeDescriptor(gatt, notifyDescriptor, payload)
        } ?: Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    private fun writeDescriptor(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        descriptor.value = payload
        gatt.writeDescriptor(descriptor)
    }

    private fun onUpdateCharacteristic(characteristic:BluetoothGattCharacteristic) {
        val value = String(characteristic.value)
        when (characteristic.uuid) {
            GATT_UUID_CHAR_STATE -> {
                Log.d(TAG, "MAVLink State characteristic value update: $value")
                listener.onMAVLinkStateUpdate(value)
            }
            else -> {
                Log.d(TAG, "Unknown characteristic (${characteristic.uuid}) value update: $value")
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Device=($address) GATT State = CONNECTED! Continuing with discoverServices...")
                        bluetoothGatt = gatt
                        // Calling `discoverServices` from Main Thread, following the advice in here:
                        // https://punchthrough.com/android-ble-guide/ (in "Discovering services")
                        Handler(Looper.getMainLooper()).post {
                            bluetoothGatt?.discoverServices()
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Device=($address) GATT State = DISCONNECTED")
                        gatt.close()
                    }
                    BluetoothProfile.STATE_CONNECTING ->
                        Log.i(TAG, "Device=($address) GATT State = CONNECTING...")
                    BluetoothProfile.STATE_DISCONNECTING ->
                        Log.i(TAG, "Device=($address) GATT State = DISCONNECTING...")
                }
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                    Log.e(TAG, "Write failed with GATT_INVALID_ATTRIBUTE_LENGTH!")
                }
                else -> {
                    Log.w(TAG, "Error $status encountered for $address! Disconnecting...")
                    gatt.close()
                    onConnectionFailed()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (gatt == null) {
                Log.e(TAG, "onServicesDiscovered failed, the Gatt device is null status = $status")
                onConnectionFailed()
                return
            }
            Log.d(TAG, "onServicesDiscovered done. Status=$status Device=${gatt.device}")

            val readableAndNotifiable = with(
                gatt.getService(GATT_UUID_SERVICE).getCharacteristic(GATT_UUID_CHAR_STATE)
            ) { isReadable() && (isNotifiable() || isIndicatable()) }
            if (readableAndNotifiable) {
                enableCharacteristicNotifications()
            } else {
                Log.e(TAG, "characteristics not readable or notifiable!")
                gatt.printGattTable()
                gatt.close()
                onConnectionFailed()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.i(TAG, "Successfully wrote descriptor! $descriptor")
                    onConnectionComplete(gatt) // finally we can get notifications about state change
                }
                else -> {
                    Log.e(TAG, "Could not write descriptor $descriptor status = $status")
                    gatt?.close()
                    onConnectionFailed()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    onUpdateCharacteristic(characteristic)
                }
                BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                    Log.e(TAG, "Read not permitted for $characteristic!")
                }
                else -> {
                    Log.e(TAG, "Characteristic read failed for $characteristic, error: $status")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            char: BluetoothGattCharacteristic
        )  = onUpdateCharacteristic(char)
    }
}
