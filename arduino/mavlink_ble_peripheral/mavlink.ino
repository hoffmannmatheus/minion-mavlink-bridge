// Definitions
#define START             1
#define MSG_RATE          5       // Hertz
#define AP_SYSID          1       // autopilot system id
#define AP_CMPID          1       // autopilot component id
#define THIS_SYSID        42      // autopilot component id
#define THIS_CMPID        MAV_COMP_ID_CAMERA // autopilot component id
#define MAVLINK_UART_BAUD 57600 // Serial baud rate

// State
unsigned long previous_mavlink_heartbeat_time = 0;  // will store last time MAVLink was transmitted and listened
const int MAVLINK_STREAMS_REQUEST_THRESHOLD = 60;   // # of heartbeats to wait before activating STREAMS from Pixhawk. 60 = one minute.
int mavlink_heartbeats_count = MAVLINK_STREAMS_REQUEST_THRESHOLD;

void mavlinkSetup() {
  Serial1.begin(MAVLINK_UART_BAUD); // UART TX/RX connected to flight controller
}

void mavlinkHeartbeat() {
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

  // STREAMS that can be requested
  /*
   * Definitions are in common.h: enum MAV_DATA_STREAM
   *   
   * MAV_DATA_STREAM_ALL=0, // Enable all data streams
   * MAV_DATA_STREAM_RAW_SENSORS=1, /* Enable IMU_RAW, GPS_RAW, GPS_STATUS packets.
   * MAV_DATA_STREAM_EXTENDED_STATUS=2, /* Enable GPS_STATUS, CONTROL_STATUS, AUX_STATUS
   * MAV_DATA_STREAM_RC_CHANNELS=3, /* Enable RC_CHANNELS_SCALED, RC_CHANNELS_RAW, SERVO_OUTPUT_RAW
   * MAV_DATA_STREAM_RAW_CONTROLLER=4, /* Enable ATTITUDE_CONTROLLER_OUTPUT, POSITION_CONTROLLER_OUTPUT, NAV_CONTROLLER_OUTPUT.
   * MAV_DATA_STREAM_POSITION=6, /* Enable LOCAL_POSITION, GLOBAL_POSITION/GLOBAL_POSITION_INT messages.
   * MAV_DATA_STREAM_EXTRA1=10, /* Dependent on the autopilot
   * MAV_DATA_STREAM_EXTRA2=11, /* Dependent on the autopilot
   * MAV_DATA_STREAM_EXTRA3=12, /* Dependent on the autopilot
   * MAV_DATA_STREAM_ENUM_END=13,
   * 
   * Data in PixHawk available in:
   *  - Battery, amperage and voltage (SYS_STATUS) in MAV_DATA_STREAM_EXTENDED_STATUS
   *  - Gyro info (IMU_SCALED) in MAV_DATA_STREAM_EXTRA1
    https://github.com/Clooney82/MavLink_FrSkySPort/blob/4d7cfdff116db1d8d25e5c82f106d341f4fcfc69/MavLink_FrSkySPort/Mavlink.ino#L103
   */

  // To be setup according to the needed information to be requested from the Pixhawk
  const int  maxStreams = 1;
  const uint8_t MAVStreams[maxStreams] = { MAV_DATA_STREAM_ALL };
    
  for (int i=0; i < maxStreams; i++) {
    /*
     * mavlink_msg_request_data_stream_pack(system_id, component_id, 
     *    &msg, 
     *    target_system, target_component, 
     *    MAV_DATA_STREAM_POSITION, 10000000, 1);
     *    
     * mavlink_msg_request_data_stream_pack(uint8_t system_id, uint8_t component_id, 
     *    mavlink_message_t* msg,
     *    uint8_t target_system, uint8_t target_component, uint8_t req_stream_id, 
     *    uint16_t req_message_rate, uint8_t start_stop)
     * 
     */
    mavlink_msg_request_data_stream_pack(THIS_SYSID, THIS_CMPID, &msg, AP_SYSID, AP_CMPID, MAVStreams[i], MSG_RATE, START);
    uint16_t len = mavlink_msg_to_send_buffer(buf, &msg);
    Serial1.write(buf, len);
    delay(10);
  }
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
            on_mavlink_digicam_command();
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