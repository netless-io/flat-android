package io.agora.flat.common.rtm

import io.agora.flat.data.model.RoomStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassRtmEventTest {
    @Test
    fun `check_parse_room_event`() {
        val json =
            "{\"t\":\"update-room-status\",\"v\":{\"roomUUID\":\"9be7d709-de2e-4a30-8fb5-0e3c345dc917\",\"status\":\"Stopped\"}}"
        val event = ClassRtmEvent.parse(json)
        assertTrue(
            event == RoomStateEvent(
                roomUUID = "9be7d709-de2e-4a30-8fb5-0e3c345dc917",
                status = RoomStatus.Stopped
            )
        )
    }

}