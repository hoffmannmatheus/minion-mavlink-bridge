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

enum {
  COLOR_RED   = 0,
  COLOR_GREEN = 1,
  COLOR_BLUE  = 2,
  COLOR_WHITE = 3
};

const char* deviceServiceUuid = "1a1a3616-e532-4a4c-87b1-19c4f4ec590b";
const char* deviceServiceCharacteristicUuid = "6148df43-7c4c-4964-a1ad-bfbfb9032b97";

int mavlinkValue = 42;

BLEService mavlinkService(deviceServiceUuid); 
BLEStringCharacteristic mavlinkCharacteristic(deviceServiceCharacteristicUuid, BLERead | BLENotify | BLEIndicate, 128);

void setup() {
  Serial.begin(9600);
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
  mavlinkService.addCharacteristic(mavlinkCharacteristic);
  BLE.addService(mavlinkService);
  BLE.advertise();

  Serial.println("Nano 33 BLE (Peripheral Device)");
  Serial.println(" ");
}

void loop() {
  BLEDevice central = BLE.central();
  delay(500);

  if (central && central.connected() && BLE.connected()) {
    Serial.print("* Writing value to mavlinkCharacteristic: ");
    Serial.println(mavlinkValue);
    
    setColor(COLOR_GREEN);
    mavlinkCharacteristic.writeValue(String(mavlinkValue));
  } else {
    setColor(COLOR_RED);
    Serial.println("- Discovering central device...");
  }
  mavlinkValue++;
}
