/*
  Main file
*/

#include <ArduinoBLE.h>
#include "c_library_v2/common/mavlink.h"

// Definitions
#define DEVICE_NAME                "Arduino Nano 33 BLE"
#define BLE_HEARTBEAT_INTERVAL     500        // Milliseconds
#define MAVLINK_HEARTBEAT_INTERVAL 1000       // Milliseconds
#define BLE_STATE_UPDATE_SEPARATOR "|"

enum {
  COLOR_RED   = 0,
  COLOR_GREEN = 1,
  COLOR_BLUE  = 2,
  COLOR_WHITE = 3
};

// State
String last_state_update_sent      = "";
String last_base_state_received    = "";
String last_mission_seq_received   = "";
String last_mission_state_received = "";
int last_digicam_command = 0;

void setup() {
  Serial.begin(9600); // USB serial, for debugging and logs
  //while (!Serial);  Debug only
  
  ledPinSetup();
  bluetoothSetup();
  mavlinkSetup();
}

void loop() {
  bluetoothHeartbeat();
  mavlinkHeartbeat();
}

// MavLink updates
void on_mavlink_base_state_update(String base_state) {
  last_base_state_received = base_state;
  send_bluetooth_update_if_needed();
}

void on_mavlink_mission_state_update(String sequence, String mission_state) {
  last_mission_seq_received = sequence;
  last_mission_state_received = mission_state;
  send_bluetooth_update_if_needed();
}

void on_mavlink_digicam_command() {
  last_digicam_command++;
  send_bluetooth_update_if_needed();
}

// Bluetooth write logic. Note we shouldn't spam BLE with writes.
void send_bluetooth_update_if_needed() {
  String new_state = last_base_state_received + BLE_STATE_UPDATE_SEPARATOR
                   + last_mission_seq_received + BLE_STATE_UPDATE_SEPARATOR
                   + last_mission_state_received + BLE_STATE_UPDATE_SEPARATOR
                   + String(last_digicam_command);
  if (new_state != last_state_update_sent) {
    sendBluetoothStateUpdate(new_state);
    last_state_update_sent = new_state;
  }
}
