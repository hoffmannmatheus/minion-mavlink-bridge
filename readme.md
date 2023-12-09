
# Minion MAVLink camera

An Android phone as a UAV camera (unmanned aerial vehicle) that can take pictures autonomously.

<div align="center">
  
  ![PXL_20231202_180807861](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/9cb7b5be-435a-4c2f-956f-6ba990e1027f)
  
  Minion Mk3, during an autonomous mission.
</div>

## Objective

My idea is to make a virtual balcony, 100ft up above my house, so my wife and I can see the sunset vicariously through my drone's eyes. The objective is to take pictures of the landscape above my home, from a specific altitute, position and orientation, consistently across different days.

In theory, this can be accomplished by creating a waypoint mission that the UAV can follow, which includes an instruction to take a picture at the right time. Once setup, I would simply need to get the UAV in the air, and let it execute mission.

In practice, I ran into issues that I describe below. This isn't the end though, a new project is coming.


## ⚠️ Note, this repo is inactive 

This project is somewhat of a failed experiment, or at least not usable with my device.

Even though I'm not going to use this anymore, I was able to learn so much from this experience. I took a nice look into MAVLink and Ardupilo, learned about Bluetooth connectivity, and practiced some Android development.

I was able to code and connect all key pieces, but one crucial hardware issue I wasn't able to overcome: the camera stabilitization wasn't good enough to take clear pictures.
I used my spare Moto G Power, which admitedly is not known for having great camera hardware.

Here's one example:

<div align="center">
  
  ![minion-2023-12-02-13-09-12-seq-2-S](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/6904fe28-2242-45f7-bcc6-9d77ecf4c25d)
  
  Trees, kinda. Taken with Moto G Power.
</div>

After the beautiful results you can see above, I installed vibration dampeners built for the purpose of stabilizing camera devices (this is pretty obvious in retrospect!).

<div align="center">
  
  ![dampener](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/492c0056-ed29-49f8-80e9-eeab0d713ec5)
  
  Vibration dampeners.
</div>

I know, the picture above hints at a GoPro, and not my old Moto G Power, but that's because I'm writing in the future :-).

Still, even with the vibration dampeners, I got bad results. I also tried different [CAMERA_MODES](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY()) in CameraX, but no luck. 
Last sample:

<div align="center">
  
  ![minion-2023-12-04-23-03-39-seq-4-S](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/6584c04c-e61f-4b0d-8070-907de7dc79e5)

  Minion selfie, with vibration dampeners, taken from the Moto G Power.
</div>

At this point, I'm not confident that my Moto G Power is capable of this task. I have not tested with higher end Android phones, but its possible that we could see better results. It's also possible there are more things that can be done software-wise.

But I chose instead to pause this effort, and restart with a GoPro instead. I'll start a new repository for that project soon. 

I'll keep this repo around as it can still be used as showcase, I certainly learned a lot during the process. Hopefully some of it can be useful for someone in the future.

Thanks, bye!



----
Below docs serve as archive:

## Overview

<div align="center">
  
![minion-mavlink-bridge](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/72c76cfc-ece5-40a5-a8cc-61fc7acdc0da)

[Diagarm source](https://excalidraw.com/#json=3ylJp-M0NiVo8VZMmTyvd,S0MKMXAFh2uTARwM07sczw)
</div>

There are three main components to this project:
1. `android/`: native Android application that connects to our bluetooth device, gets state updates from the UAV, and can take pictures on command.
2. `arduino/`: a microcontroller directly connected to the UAV's flight controller, communicatating with it via the [MAVLink protocol](https://mavlink.io/en/), and relaying state to the Android app via bluetooth.
3. Flight Controller (no code needed): the drone's flight computer, running the firmware [Ardupilot](https://ardupilot.org/), connected to the arduino board via UART.

## Android App

The app has a simple structure, containing currently a single fragment `HomeFragment` and its `HomeViewModel`, aided with utility classes for handling the [Bluetooth connection](https://github.com/hoffmannmatheus/minion-mavlink-bridge/blob/main/android/app/src/main/java/com/mhsilva/minioncamera/bluetooth/BluetoothHelper.kt) and [`MAVLink`](https://github.com/hoffmannmatheus/minion-mavlink-bridge/blob/main/android/app/src/main/java/com/mhsilva/minioncamera/mavlink/MinionState.kt) logic.
The `HomeFragment` also manages taking pictures using the CameraX API, if instructed to do so via state updates.

Certainly the Bluetooth Low Energy (BLE) logic was where I learned the most when implementing the App. I found [this guide](https://punchthrough.com/android-ble-guide/) to be extremely helpful. I can say that connecting and receiving BLE notifications is working very solidly, and I had no problems while testing onboard the UAV either.

The UI shows some state information about the UAV, the connection status of the BLE, and a counter for how many pictures it took.
Here are sample screenshots for the UI:

connected | lost connection
-|-
![Screenshot_20231203-144121-S](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/dac081ce-a8b1-44db-8c38-d5532c32b82e) | ![Screenshot_20231203-145340-S](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/21e8c66d-37b5-456b-844b-c8d9dbab2abc)

There are many areas of improvment should development continue:
- Permission checking and asking the user for permissions. This is half implemented at the moment.
- Cleanup camera logic. Currently the CameraX implementation is all over the place in the framgent, so very much not testable.
- Testing. Currently there's only a [single test suite](https://github.com/hoffmannmatheus/minion-mavlink-bridge/blob/main/android/app/src/test/java/com/mhsilva/minioncamera/mavlink/MinionStateTest.kt) for the parsing of Bluetooth messages into the minion state class.

## Hardware

I'm using the Arduino Nano 33 BLE board as the interface between my flight controller, a SpeedyBee F403V3. Below is the general setup. There is really a lot more detail that could be added, but for now I'm only describing the key parts of the setup:

### Arduino + Android App

This arduino is setup as a BLE peripheral, and it creates readable / notifiable BLE GATT characteristics. It ended up being quite easy to code this following [Arduino's BLE examples](https://docs.arduino.cc/tutorials/nano-33-ble-sense/ble-device-to-device).

The characteristic is written once a change in the "minion state" is detected. The state is really a small, condensed string created from the data retrieved by the MAVLink heartbeats being received.

The [Minion State format is better described in the code](https://github.com/hoffmannmatheus/minion-mavlink-bridge/blob/main/android/app/src/main/java/com/mhsilva/minioncamera/mavlink/MinionState.kt), but here's the format:
```base_state | mission_seq | mission_state | digicam_command_seq```


### Connection between Arduino + Flight Controller
- UART, using RX/TX. Arduino has this preset as `Serial1`. Using baud rate 57600 as configured from the Flight Controller.
- [MAVLink protocol](https://mavlink.io/en/). I'm using the [official C generated headers](https://github.com/mavlink/c_library_v2/), as a submodule.
  - The Arduino registers as a "camera" device type, sending MAVLink heartbeats at 1hz.
- From the Flight Controller, the correct Serial must be configured in Ardupilot. In my case using `BAUD 57`, and `PROTOCOL 2` (meaning, MAVLink2 -- the version of MAVLink we are using).
  - This post has a similar setup with a lot of detail: https://discuss.ardupilot.org/t/mavlink-and-arduino-step-by-step/25566

<div align="center">
  
![minion-mavlink-bridge(2)](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/ab8377b5-ddb7-4584-b28c-9c5cf26dd730)

Arduino connected to the Flight Controller via UART (TX/RX -> RX/TX).
</div>

### MAVLink camera control and this implementation

This bridge will not setup Arduino as a true "MAVLink Camera", as it does not implement the full [camera device protocol for MAVLink](https://mavlink.io/en/services/camera.html).

Notably, it will not receive MAVLink camera commands like `MAV_CMD_DO_DIGICAM_CONTROL` that may be sent during missions. I wasn't able to make that work yet. 

Instead, this implementation uses Servos controls in order to trigger the camera. As [described in this forum post](https://www.rcgroups.com/forums/showpost.php?p=33408898&postcount=7), I'm connecting an Arduino PWM input to a servo (motor) output of my Flight Controller, and triggering the camera via `DO_SET_SERVO` commands in between waypoint missions. Note that the picture above doesn't show this PWM wire.

More info on the [DO_SET_SERVO here](Info here: https://ardupilot.org/planner/docs/common-mavlink-mission-command-messages-mav_cmd.html#mav-cmd-do-set-servo).


## Bluetooth UUIDs

These are the Services and Characteristics UUIDs being used:
- Arduino client: `1a1a3616-e532-4a4c-87b1-19c4f4ec590b`
- Predefined Notification Descriptor: `00002902-0000-1000-8000-00805f9b34fb`
- Characteristic for the minion state characteristic: `6af662f3-5393-41ed-af8b-02fafe592177`

Online tool to generate UUIDs as needed: https://bleid.netlify.app/

## Useful links

- Excellent and expansive Android BLE article: https://punchthrough.com/android-ble-guide/
- Arduino BLE library docs: https://www.arduino.cc/reference/en/libraries/arduinoble/
- Arduino example on BLE communication: https://docs.arduino.cc/tutorials/nano-33-ble-sense/ble-device-to-device
- Arduino with MAVLink article: https://discuss.ardupilot.org/t/mavlink-and-arduino-step-by-step/25566

