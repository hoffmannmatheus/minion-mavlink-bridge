/*
  Bluetooth Low Energy connection. 

  This file defines the BLE service and one "state" characteristic, and provides the means
  to write String values into this characteristic.

  Arduino Nano 33 BLE docs: https://docs.arduino.cc/tutorials/nano-33-ble/bluetooth 
*/

// Definitions
#define BLE_UUID_SERVICE       "1a1a3616-e532-4a4c-87b1-19c4f4ec590b"
#define BLE_UUID_CHAR_STATE    "6af662f3-5393-41ed-af8b-02fafe592177"
#define BLE_HEARTBEAT_INTERVAL 100 // Milliseconds

// State
BLEService mavlinkService(BLE_UUID_SERVICE); 
BLEStringCharacteristic mavlinkStateCharacteristic(BLE_UUID_CHAR_STATE, BLERead | BLENotify | BLEIndicate, 256);
unsigned long previous_ble_heartbeat_time = 0;
bool notified_connected = false; // avoid serial spam
bool notified_disconnected = false; // avoid serial spam

void bluetoothSetup() {
  if (!BLE.begin()) {
    Serial.println("BLE: starting Bluetooth® Low Energy module failed!");
    while (1);
  }

  BLE.setLocalName(DEVICE_NAME);
  BLE.setDeviceName(DEVICE_NAME);
  BLE.setAdvertisedService(mavlinkService);
  mavlinkService.addCharacteristic(mavlinkStateCharacteristic);
  BLE.addService(mavlinkService);
  BLE.advertise();
}

void bluetoothLoop() {
  unsigned long current_time = millis();
  if (current_time - previous_ble_heartbeat_time >= BLE_HEARTBEAT_INTERVAL) {
    previous_ble_heartbeat_time = current_time;

    BLEDevice central = BLE.central();
    if (central && central.connected()) {
      setColor(COLOR_GREEN);

      // Debug:
      if (!notified_connected) {
        Serial.println("BLE: Connected! ");
        notified_connected = true;
      }
      notified_disconnected = false;
    } else {
      setColor(COLOR_RED);

      // Debug:
      if (!notified_disconnected) {
        Serial.println("BLE: Disconnected, waiting for central...");
        notified_disconnected = true;
      }
      notified_connected = false;
    }
  }
}

void sendBluetoothStateUpdate(String state) {
      Serial.println("BLE: writing value to STATE char: " + state);
      mavlinkStateCharacteristic.writeValue(state);
}

