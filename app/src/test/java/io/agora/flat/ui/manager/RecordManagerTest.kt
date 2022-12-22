package io.agora.flat.ui.manager

import io.agora.flat.data.model.RoomUser
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordManagerTest {

    @Test
    fun `the owner should be the first`() {
        val users = listOf(
            RoomUser("1", 1, isOnStage = true, isOwner = false),
            RoomUser("6", 6, isOnStage = true, isOwner = false),
            RoomUser("2", 2, isOnStage = true, isOwner = true),
            RoomUser("3", 3, isOnStage = false, isOwner = false),
            RoomUser("4", 4, isOnStage = false, isOwner = false),
            RoomUser("5", 5, isOnStage = true, isOwner = false),
        )
        val result = RecordManager.filterOnStages(users)
        assertTrue(result.size == 4)
        assertTrue(result[0].isOwner)
        assertArrayEquals(result.map { it.rtcUID }.toIntArray(), intArrayOf(2, 1, 5, 6))
    }
}