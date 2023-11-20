
# Minion MAVLink camera 




## Bluetooth information
These are the Services and Characteristics UUIDs being used:


- Arduino client: `1a1a3616-e532-4a4c-87b1-19c4f4ec590b`
  - Predefined Notification Descriptor: `00002902-0000-1000-8000-00805f9b34fb`
  - Characteristics:
    - MAVLink heartbeat, arm status, flight mode, and waypoint characteristic: `6af662f3-5393-41ed-af8b-02fafe592177`
    - MAVLink picture characteristic: `7627ba39-df6b-44a9-b02a-e7d5eccdf94f`
    - MAVLink picture characteristic: `1abaa097-6d83-4b04-a230-9e001aebf1a5`

- Some extra UUIDs if needed:
  - Generate more with https://bleid.netlify.app/


## Useful links

- Excellent and expansive Android BLE article: https://punchthrough.com/android-ble-guide/
- Arduino BLE library docs: https://www.arduino.cc/reference/en/libraries/arduinoble/
- Arduino example on BLE communication: https://docs.arduino.cc/tutorials/nano-33-ble-sense/ble-device-to-device
- Arduino with MAVLink article: https://discuss.ardupilot.org/t/mavlink-and-arduino-step-by-step/25566

