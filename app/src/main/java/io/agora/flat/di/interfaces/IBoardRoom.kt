package io.agora.flat.di.interfaces

import com.herewhite.sdk.domain.ConvertedFiles
import io.agora.board.fast.FastboardView
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.flat.common.board.BoardRoomPhase
import kotlinx.coroutines.flow.Flow

interface IBoardRoom {
    fun initSdk(fastboardView: FastboardView)

    fun setDarkMode(dark: Boolean)

    fun setRoomController(rootRoomController: RoomControllerGroup)

    fun join(roomUUID: String, roomToken: String, region: String, writable: Boolean)

    fun release()

    suspend fun setWritable(writable: Boolean): Boolean

    fun setAllowDraw(allow: Boolean)

    fun hideAllOverlay()

    // courseware
    fun insertImage(imageUrl: String, w: Int, h: Int)

    fun insertPpt(dir: String, files: ConvertedFiles, title: String)

    fun insertProjectorPpt(taskUuid: String, prefixUrl: String, title: String)

    fun insertVideo(videoUrl: String, title: String)

    fun observeRoomPhase(): Flow<BoardRoomPhase>
}