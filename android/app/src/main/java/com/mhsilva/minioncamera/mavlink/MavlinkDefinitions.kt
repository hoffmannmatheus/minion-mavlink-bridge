package com.mhsilva.minioncamera.mavlink

/**
 * Simplified flight mode definition. Since we are getting flight modes from heartbeat messages
 * only, we don't know the specific flight mode, only auto (in a mission, holding position, etc) or
 * manual (remote control, stabilize, etc). See [MavMode].
 */
enum class FlightMode {
    MANUAL,
    AUTO,
    UNKNOWN;

    companion object {
        fun fromMavModeFlags(flags: Int) = when {
            MavMode.MAV_MODE_FLAG_GUIDED_ENABLED.matches(flags)
                    || MavMode.MAV_MODE_FLAG_AUTO_ENABLED.matches(flags) -> AUTO
            MavMode.MAV_MODE_FLAG_STABILIZE_ENABLED.matches(flags)
                    || MavMode.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED.matches(flags) -> MANUAL
            else ->UNKNOWN
        }
    }
}

/**
 * MAVLink Mission State, as defined in:
 * https://mavlink.io/en/messages/common.html#MISSION_STATE
 */
enum class MissionState(private val value: Int) {
    MISSION_STATE_UNKNOWN(0),
    MISSION_STATE_NO_MISSION(1),
    MISSION_STATE_NOT_STARTED(2),
    MISSION_STATE_ACTIVE(3),
    MISSION_STATE_PAUSED(4),
    MISSION_STATE_COMPLETE(5);

    companion object {
        fun fromValue(value: Int?) = MissionState.values().firstOrNull {
            it.value == value
        } ?: MISSION_STATE_UNKNOWN
    }
}

/**
 * MAVLink "base_mode" flag definitions, use with heartbeat messages. Values defined in:
 * https://mavlink.io/en/messages/common.html#MAV_MODE_FLAG
 */
enum class MavMode(private val flag: Int) {
    MAV_MODE_FLAG_SAFETY_ARMED(128),
    MAV_MODE_FLAG_MANUAL_INPUT_ENABLED(64),
    MAV_MODE_FLAG_STABILIZE_ENABLED(16),
    MAV_MODE_FLAG_GUIDED_ENABLED(8),
    MAV_MODE_FLAG_AUTO_ENABLED(4);

    fun matches(value: Int) = value and flag > 0
}