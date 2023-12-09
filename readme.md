
# Minion MAVLink camera

An Android phone as a UAV (unmanned aerial vehicle) camera that can take pictures autonomously.

![PXL_20231110_212310584_S](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/8cde429c-507e-4e9d-b986-32259579e030)
<div align="center">
  The Minion Mk3, running Ardupilot in position hold mode.
</div>

## Objective

My ida is to make a virtual balcony, 100ft up above my house, so I can get pictures of the sunset. So the objective is to take pictures of the landscape above my home, from a specific altitute, position and orientation, consistently across different days.

In theory, this can be accomplished by creating a waypoint mission that the UAV can follow, which includes an instruction to take a picture at the right time. Once setup, I would simply need to get the UAV in the air, and let it execute mission.

In practice, I ran into issues that I describe below. This isn't the end though!


## ⚠️ Note, this project is on halt

This project is somewhat of a failed experiment, or at least not in a usable state. 

I was able to code and connect all key pieces, but one crucial hardware issue I wasn't able to overcome: the camera stabilitization wasn't good enough to take clear pictures.
I used my spare Moto G Power, which admitedly is not known for having great camera hardware.

Here's one example:

<div align="center">
  
  ![minion-2023-12-02-13-09-12-seq-2-S](https://github.com/hoffmannmatheus/minion-mavlink-bridge/assets/889815/6904fe28-2242-45f7-bcc6-9d77ecf4c25d)
  
  Trees, kinda. Taken with Moto G Power.
</div>

After the beautiful results you can see above, I installed vibration dampaners built for the purpose of stabilizing camera devices (this is pretty obvious in retrospect!).

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

At this point, I'm willing to throw money at the problem, since there exists a plethora of better camers fit for purpose with image stabilization.
I have not tested with higher end Android phones, but its possible that we could see better results. It's also possible there are more things that can be done softweare-wise.

But I chose instead to pause this effort, and restart with a GoPro instead. These cameras are built for applications like this, and after some testing, the results are worlds appart. I'll start another repo for that project though. 

I'll keep this project around as it can still be used as showcase, and maybe the Arduino code especially might be useful for other people.

Thanks, bye!



----
Below docs serve as archive:

## Overview
There are three main components to this project:
1. `android/`: native Android application that connects to our bluetooth device, gets state updates from the UAV, and can take pictures on command.
2. `arduino/`: a microcontroller directly connected to the UAV's flight controller, communicatating with it via the [MAVLink protocol](https://mavlink.io/en/), and relaying state to the Android app via bluetooth.
3. UAV: the drone, sporting a flight controller running [Ardupilot](https://ardupilot.org/), connected to the arduino board via UART.


## Hardware


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

## MAVLink camera control and this implementation

This bridge will not setup Arduino as a true "MAVLink Camera", as it does not implement the full [camera device protocol for MAVLink](https://mavlink.io/en/services/camera.html).

Notably, it will not receive MAVLink camera commands like `MAV_CMD_DO_DIGICAM_CONTROL` that may be sent during missions. I wasn't able to make that work yet. 

Instead, this implementation uses Servos controls in order to trigger the camera. As [described in this forum post](https://www.rcgroups.com/forums/showpost.php?p=33408898&postcount=7), I'm connecting an Arduino PWM input to a servo (motor) output of my Flight Controller, and triggering the camera via `DO_SET_SERVO` commands in between waypoint missions. 

More info on the [`DO_SET_SERVO` here](Info here: https://ardupilot.org/planner/docs/common-mavlink-mission-command-messages-mav_cmd.html#mav-cmd-do-set-servo).

## Useful links

- Excellent and expansive Android BLE article: https://punchthrough.com/android-ble-guide/
- Arduino BLE library docs: https://www.arduino.cc/reference/en/libraries/arduinoble/
- Arduino example on BLE communication: https://docs.arduino.cc/tutorials/nano-33-ble-sense/ble-device-to-device
- Arduino with MAVLink article: https://discuss.ardupilot.org/t/mavlink-and-arduino-step-by-step/25566

