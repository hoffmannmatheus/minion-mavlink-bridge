package com.mhsilva.minioncamera.mavlink

import org.junit.Test

import org.junit.Assert.*

class MinionStateTest {
    @Test
    fun `test fromString defaults`() {
        assertEquals(
            MinionState(
                isArmed = false,
                flightMode = FlightMode.UNKNOWN,
                missionState = MissionState.MISSION_STATE_UNKNOWN,
                missionSequence = 0,
                pictureSequence = 0
            ),
            MinionState.fromString("|||")
        )
        assertEquals(
            MinionState(
                isArmed = false,
                flightMode = FlightMode.UNKNOWN,
                missionState = MissionState.MISSION_STATE_UNKNOWN,
                missionSequence = 0,
                pictureSequence = 0
            ),
            MinionState.fromString("a|b|c|d") // invalid integers will turn into defaults
        )
    }

    @Test
    fun `test fromString valid states`() {
        assertEquals(
            MinionState(
                isArmed = false,
                flightMode = FlightMode.MANUAL,
                missionState = MissionState.MISSION_STATE_UNKNOWN,
                missionSequence = 0,
                pictureSequence = 0
            ),
            MinionState.fromString("81|0|0|0")
        )
        assertEquals(
            MinionState(
                isArmed = true,
                flightMode = FlightMode.AUTO,
                missionState = MissionState.MISSION_STATE_ACTIVE,
                missionSequence = 10,
                pictureSequence = 5
            ),
            MinionState.fromString("133|10|3|5")
        )
    }

    @Test
    fun `test fromString empty and invalid states`() {
        assertNull(MinionState.fromString(""))
        assertNull(MinionState.fromString("invalid"))
        assertNull(MinionState.fromString("||||"))
    }
}