package com.mhsilva.minioncamera.ui.home

import android.R.bool
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

            override fun onDisconnected() {
                _connectionStatus.postValue(ConnectionStatus.STANDING_BY)
            }

            override fun onConnectFailed() {
                _connectionStatus.postValue(ConnectionStatus.ERROR)
            }
            override fun onMAVLinkStateUpdate(value: String) {

                // base mode, flags defined in https://mavlink.io/en/messages/common.html#MAV_MODE_FLAG
//
//                val isArmed: bool = hb.base_mode and (MAV_MODE_FLAG_SAFETY_ARMED > 0)
//                val isStabilize: bool = hb.base_mode and (MAV_MODE_FLAG_STABILIZE_ENABLED > 0)
//                val isAuto: bool = hb.base_mode and (MAV_MODE_FLAG_AUTO_ENABLED > 0)
//                val isRcConnected: bool = hb.base_mode and (MAV_MODE_FLAG_MANUAL_INPUT_ENABLED > 0)
//                val isCustom: bool = hb.base_mode and (MAV_MODE_FLAG_CUSTOM_MODE_ENABLED > 0)

                _mavLinkMode.postValue(value)
            }
        })
    }

    fun disconnect() {
        bluetoothHelper?.disconnect(onPurpose = true)
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