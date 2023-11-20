/*
  BLE_Peripheral.ino

  This program uses the ArduinoBLE library to set-up an Arduino Nano 33 BLE 
  as a peripheral device and specifies a service and a characteristic. Depending 
  of the value of the specified characteristic, an on-board LED gets on. 

  The circuit:
  - Arduino Nano 33 BLE. 

  This example code is in the public domain.
*/

#include <ArduinoBLE.h>
#include "c_library_v2/common/mavlink.h"

enum {
  COLOR_RED   = 0,
  COLOR_GREEN = 1,
  COLOR_BLUE  = 2,
  COLOR_WHITE = 3
};

// Bluetooh service
BLEService mavlinkService("1a1a3616-e532-4a4c-87b1-19c4f4ec590b"); 
// Bluetooth characteristics
BLEStringCharacteristic mavlinkStateCharacteristic("6af662f3-5393-41ed-af8b-02fafe592177", BLERead | BLENotify | BLEIndicate, 256);
BLEStringCharacteristic mavlinkTakePictureCharacteristic("7627ba39-df6b-44a9-b02a-e7d5eccdf94f", BLERead | BLENotify | BLEIndicate, 128);

int mavlinkValue = 42;

void setup() {
  Serial.begin(9600);   // USB serial, for debugging and logs
  Serial1.begin(57600); // UART TX/RX connected to flight controller
  //while (!Serial);  Debug only
  
  pinMode(LEDR, OUTPUT);
  pinMode(LEDG, OUTPUT);
  pinMode(LEDB, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  setColor(-1);

  if (!BLE.begin()) {
    Serial.println("- Starting Bluetooth® Low Energy module failed!");
    while (1);
  }

  BLE.setLocalName("Arduino Nano 33 BLE (Peripheral)");
  BLE.setDeviceName("Arduino Nano 33 BLE");
  BLE.setAdvertisedService(mavlinkService);
  mavlinkService.addCharacteristic(mavlinkStateCharacteristic);
  BLE.addService(mavlinkService);
  BLE.advertise();

  Serial.println("Nano 33 BLE (Peripheral Device)");
  Serial.println(" ");
}

void loop() {
  BLEDevice central = BLE.central();
  delay(500);

  if (central && central.connected() && BLE.connected()) {
    Serial.print("* Writing value to mavlinkStateCharacteristic: ");
    Serial.println(mavlinkValue);
    
    setColor(COLOR_GREEN);
    mavlinkStateCharacteristic.writeValue(String(mavlinkValue));
  } else {
    setColor(COLOR_RED);
    //Serial.println("- Discovering central device...");
  }
  mavlinkValue++;
  mavLinkHeartbeat();
}

void on_mode_update(String armed_and_mode) {
    Serial.print("on_mode_update: ");
    Serial.println(armed_and_mode);
}

void on_mission_state_update(String sequence, String mission_state) {
    Serial.print("on_mission_state_update: ");
    Serial.print(sequence);  // Seems like this is the only value SpeedyBee is sending properly for now
    Serial.print(" / ");
    Serial.println(mission_state);
}

void on_picture_update(String sequence) {
    Serial.print("on_picture_update: ");
    Serial.println(sequence);
}
