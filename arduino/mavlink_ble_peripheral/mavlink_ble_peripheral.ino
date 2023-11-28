/*
  Main file, handling Arduino setup and loop.

  This file also gets "callbacks" that update the minion state, which basically is a selection of
  MAVLink and picture trigger servo information.

  If the state is updated, it is then written into the Bluetooth Characteristic, which can be
  read by and notified to BLE devices.

  The state is a sequence of numbers joined in a String, in the following format:
    base_state | mission_seq | mission_state | digicam_command_seq
  Where "base state" is a MAVLink heartbeat, and is parsed with MAV_MODE flags.
*/

#include <ArduinoBLE.h>
#include "c_library_v2/common/mavlink.h"

// Definitions
#define DEVICE_NAME                "Arduino Nano 33 BLE"
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
  //while (!Serial); // Debug only!
  
  ledPinSetup();
  bluetoothSetup();
  mavlinkSetup();
  servoSetup();
}

void loop() {
  bluetoothLoop();
  mavlinkLoop();
  servoLoop();
}

// MavLink updates
void onMavlinkBaseStateUpdate(String base_state) {
  last_base_state_received = base_state;
  sendBluetoothUpdateIfNeeded();
}

void onMavlinkMissionStateUpdate(String sequence, String mission_state) {
  last_mission_seq_received = sequence;
  last_mission_state_received = mission_state;
  sendBluetoothUpdateIfNeeded();
}

// Servo updates
void onTriggerCamera() {
  last_digicam_command++;
  sendBluetoothUpdateIfNeeded();
}

// Bluetooth write logic. Note we shouldn't spam BLE with writes.
void sendBluetoothUpdateIfNeeded() {
  String new_state = last_base_state_received + BLE_STATE_UPDATE_SEPARATOR
                   + last_mission_seq_received + BLE_STATE_UPDATE_SEPARATOR
                   + last_mission_state_received + BLE_STATE_UPDATE_SEPARATOR
                   + String(last_digicam_command);
  if (new_state != last_state_update_sent) {
    sendBluetoothStateUpdate(new_state);
    last_state_update_sent = new_state;
  }
}
