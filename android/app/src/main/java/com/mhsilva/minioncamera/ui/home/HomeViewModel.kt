package com.mhsilva.minioncamera.ui.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mhsilva.minioncamera.bluetooth.BluetoothHelper
import com.mhsilva.minioncamera.bluetooth.BluetoothHelperListener
import com.mhsilva.minioncamera.utils.hasRequiredRuntimePermissions

@SuppressLint("MissingPermission")
class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _connectionStatus = MutableLiveData(ConnectionStatus.SETTING_UP)
    private val _mavLinkMode = MutableLiveData<String?>(null)
    private var bluetoothHelper: BluetoothHelper? = null

    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus
    val mavlinkMode: LiveData<String?> = _mavLinkMode

    fun setupBluetooth(context: Context) {
        _connectionStatus.value = ConnectionStatus.STANDING_BY
        bluetoothHelper = BluetoothHelper(context, object : BluetoothHelperListener {
            // From background thread, need to use postValue for data updates
            override fun onConnected() {
                _connectionStatus.postValue(ConnectionStatus.CONNECTED)
            }
            override fun onConnectFailed() {
                _connectionStatus.postValue(ConnectionStatus.ERROR)
            }
            override fun onMAVLinkStateUpdate(value: String) {
                _mavLinkMode.postValue(value)
            }
        })
    }

    fun teardownBluetooth() {
        bluetoothHelper?.disconnect()
    }

    fun connect(context: Context) {
        if (!context.hasRequiredRuntimePermissions()) {
            _connectionStatus.value = ConnectionStatus.NEED_PERMISSIONS
        } else {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            bluetoothHelper?.connect()
        }
    }
}