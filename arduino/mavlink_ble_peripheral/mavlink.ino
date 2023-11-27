/*
  MAVLink 2 logic and UART (Serial1, TX/RX) connection.

  This file provides the means to receive and parse MAVLink data. This Arduino device is 
  confiured to act like a "camera" type device (see THIS_CMPID in the definitions below).
  It sends MAVLink heartbeat messages at 1Hz, requests data streams, and can parse messages.

  We heavily depend on the MAVLink 2 C library: https://github.com/mavlink/c_library_v2
  And the MAVLink protocol documentation: https://mavlink.io/ 
*/

// Definitions
#define MAVLINK_HEARTBEAT_INTERVAL  1000               // Milliseconds
#define AP_SYSID                    1                  // autopilot (flight controller) system id
#define AP_CMPID                    1                  // autopilot (flight controller) component id
#define THIS_SYSID                  42                 // This (arduino) system id
#define THIS_CMPID                  MAV_COMP_ID_CAMERA // This (arduino) component id
#define MAVLINK_UART_BAUD           57600              // Serial baud rate
#define MSG_RATE                    5                  // Hertz

// State
unsigned long previous_mavlink_heartbeat_time = 0;
const int MAVLINK_STREAMS_REQUEST_THRESHOLD = 60;  // # of heartbeats to wait before activating STREAMS from Pixhawk. 60 = one minute.
int mavlink_heartbeats_count = MAVLINK_STREAMS_REQUEST_THRESHOLD;

void mavlinkSetup() {
  Serial1.begin(MAVLINK_UART_BAUD); // UART TX/RX connected to flight controller
}

void mavlinkLoop() {
  unsigned long current_time = millis();
  if (current_time - previous_mavlink_heartbeat_time >= MAVLINK_HEARTBEAT_INTERVAL) {
    previous_mavlink_heartbeat_time = 
    current_time;
    mavlink_heartbeats_count++;

    // MAVLink config
    int type = MAV_TYPE_CAMERA;                     // Type. Alternative: MAV_TYPE_GENERIC, MAV_TYPE_LOG
    uint8_t autopilot_type = MAV_AUTOPILOT_INVALID; // Invalid for none-flight controllers
    uint8_t system_mode = MAV_MODE_PREFLIGHT;       // Flight mode, booting up
    uint32_t custom_mode = 0;                       // Custom mode, can be defined by user
    uint8_t system_state = MAV_STATE_STANDBY;       // System ready for flight

    // Pack the message, copy to buffer, and write to the UART
    mavlink_message_t msg;
    uint8_t buf[MAVLINK_MAX_PACKET_LEN];
    mavlink_msg_heartbeat_pack(THIS_SYSID, THIS_CMPID, &msg, type, autopilot_type, system_mode, custom_mode, system_state);
    uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
    Serial1.write(buf, len);

    if (mavlink_heartbeats_count >= MAVLINK_STREAMS_REQUEST_THRESHOLD) {
      // Request streams from the Flight Controller
      request_mavlink_data();
      mavlink_heartbeats_count = 0;
    }
  }

  read_mavlink_uart();
}

void request_mavlink_data() {
  mavlink_message_t msg;
  uint8_t buf[MAVLINK_MAX_PACKET_LEN];
  mavlink_msg_request_data_stream_pack(THIS_SYSID, THIS_CMPID, &msg, AP_SYSID, AP_CMPID, MAV_DATA_STREAM_ALL, MSG_RATE, 1);
  uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
  Serial1.write(buf, len);
}

void ack_command(uint16_t command_to_ack, uint8_t sender_sysid, uint8_t sender_compid) {
  mavlink_message_t msg;
  uint8_t buf[MAVLINK_MAX_PACKET_LEN];
  mavlink_msg_command_ack_pack(THIS_SYSID, THIS_CMPID, &msg, command_to_ack, MAV_RESULT_ACCEPTED, 100, 0, sender_sysid, sender_compid);

	uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
  Serial1.write(buf, len);
}

void read_mavlink_uart() {
  mavlink_message_t msg;
  mavlink_status_t status;

  while(Serial1.available() > 0) {
    uint8_t c = Serial1.read();

    if(mavlink_parse_char(MAVLINK_COMM_0, c, &msg, &status)) {
      
      // Serial.print("MavLink got new message: "); // Debug only, very chatty
      // Serial.println(msg.msgid);

      // Handle message
      switch(msg.msgid) {
        case MAVLINK_MSG_ID_HEARTBEAT: { // #0: Heartbeat
          mavlink_heartbeat_t hb;
          mavlink_msg_heartbeat_decode(&msg, &hb);

          // Note! This logic assumes we are connected to a quadcopter. We need to filter 
          // hearbeats by type, since GCS and other MavLink device heartbeats will be received.
          if (hb.type == MAV_TYPE_QUADROTOR) {
            on_mavlink_base_state_update(String(hb.base_mode));
          }
        }
        break;

        case MAVLINK_MSG_ID_MISSION_CURRENT : { // #42
          mavlink_mission_current_t mission_current;
          mavlink_msg_mission_current_decode(&msg, &mission_current);
          String sequence = String(mission_current.seq);
          String mission_state = String(mission_current.mission_state);
          on_mavlink_mission_state_update(sequence, mission_state);
        }
        break;

        case MAVLINK_MSG_ID_COMMAND_LONG: {  // #76  (replaces deprecated #11)
          mavlink_command_long_t command_long;
          mavlink_msg_command_long_decode(&msg, &command_long);
          Serial.print("on_command_long: ");
          Serial.println(command_long.command);
          if (command_long.command == MAV_CMD_DO_DIGICAM_CONTROL) {
            on_trigger_camera();
            ack_command(command_long.command, msg.sysid, msg.compid);
          }
        }
        break;

        case MAVLINK_MSG_ID_CAMERA_TRIGGER: {  // #112
          mavlink_camera_trigger_t camera_trigger;
          mavlink_msg_camera_trigger_decode(&msg, &camera_trigger);
          String sequence = String(camera_trigger.seq);
          Serial.print("MAVLINK_MSG_ID_CAMERA_TRIGGER: ");
          Serial.println(camera_trigger.seq);
        }
        break;
        
       default:
          break;
      }
    }
  }
}