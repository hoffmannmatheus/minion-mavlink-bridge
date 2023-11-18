package com.mhsilva.minioncamera.bluetooth

interface BluetoothHelperListener {
    fun onConnected()
    fun onConnectFailed()
    fun onMAVLinkStateUpdate(value: String)
}