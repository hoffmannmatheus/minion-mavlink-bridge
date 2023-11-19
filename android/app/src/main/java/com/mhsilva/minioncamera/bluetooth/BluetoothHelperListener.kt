package com.mhsilva.minioncamera.bluetooth

interface BluetoothHelperListener {
    fun onConnected()
    fun onDisconnected()
    fun onConnectFailed()
    fun onMAVLinkStateUpdate(value: String)
}