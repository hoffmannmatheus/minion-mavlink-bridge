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
    private var bluetoothHelper: BluetoothHelper? = null

    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    fun setupBluetooth(context: Context) {
        _connectionStatus.value = ConnectionStatus.STANDING_BY
        bluetoothHelper = BluetoothHelper(context, object : BluetoothHelperListener {
            override fun onConnected() {
                _connectionStatus.postValue(ConnectionStatus.CONNECTED) // from background thread
            }
            override fun onConnectFailed() {
                _connectionStatus.postValue(ConnectionStatus.ERROR) // from background thread
            }
        })
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