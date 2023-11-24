package com.mhsilva.minioncamera.mavlink

/**
 * MinionState holds all data collected via bluetooth about Minion.
 * Minion is a drone, and the state updates received via BLE are all custom.
 */
data class MinionState(
    val isArmed: Boolean,
    val flightMode: FlightMode,
    val missionState: MissionState,
    val missionSequence: Int,
    val pictureSequence: Int,
) {
    companion object {
        private const val STATE_LIST_SEPARATOR = "|"

        /**
         * Parses the given string into a [MinionState].
         *
         * @param fullStateString The input state string, as sent by the Arduino MAVLink bridge.
         * The expected format is a sequence of state data, separated by a "|" character, in the
         * following order:
         *      base_state | mission_seq | mission_state | digicam_command_seq
         * Where "base state" is a MAVLink heartbeat, and is parsed with [MavMode] flags.
         *
         * For example, the fullStateString "133|10|3|5" renders:
         *      isArmed = true,
         *      flightMode = FlightMode.AUTO,
         *      missionState = MissionState.MISSION_STATE_ACTIVE,
         *      missionSequence = 10,
         *      pictureSequence = 5
         *
         * @return The MinionState if a valid input was passed, null otherwise.
         */
        fun fromString(fullStateString: String): MinionState? {
            val stateStrings = fullStateString.split(STATE_LIST_SEPARATOR)
            if (stateStrings.size != 4) {
                return null
            }

            val isArmed: Boolean
            val flightMode: FlightMode
            val missionState: MissionState
            val missionSequence: Int
            val pictureSequence: Int

            stateStrings[0 /*base_mode*/].toIntOrNull().let { value ->
                if (value != null) {
                    isArmed = value.let {
                        MavMode.MAV_MODE_FLAG_SAFETY_ARMED.matches(it)
                    }
                    flightMode = FlightMode.fromMavModeFlags(value)
                } else {
                    isArmed = false
                    flightMode = FlightMode.UNKNOWN
                }
            }
            stateStrings[1 /*mission_seq*/].toIntOrNull().let { value ->
                missionSequence = value ?: 0
            }
            stateStrings[2 /*mission_state*/].toIntOrNull().let { value ->
                missionState = MissionState.fromValue(value)
            }
            stateStrings[3 /*digicam_command_seq*/].toIntOrNull().let { value ->
                pictureSequence = value ?: 0
            }

            return MinionState(
                isArmed = isArmed,
                flightMode = flightMode,
                missionState = missionState,
                missionSequence = missionSequence,
                pictureSequence = pictureSequence,
            )
        }
    }
}
