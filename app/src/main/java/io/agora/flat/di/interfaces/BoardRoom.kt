package io.agora.flat.di.interfaces

import com.herewhite.sdk.domain.ConvertedFiles
import com.herewhite.sdk.domain.Scene
import io.agora.board.fast.FastboardView
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.flat.common.board.BoardError
import io.agora.flat.common.board.BoardPhase
import kotlinx.coroutines.flow.Flow

interface BoardRoom {
    fun setupView(fastboardView: FastboardView)

    fun setDarkMode(dark: Boolean)

    fun setRoomController(rootRoomController: RoomControllerGroup)

    suspend fun join(roomUUID: String, roomToken: String, region: String, writable: Boolean)

    fun release()

    suspend fun setWritable(writable: Boolean): Boolean

    suspend fun setAllowDraw(allow: Boolean)

    fun hideAllOverlay()

    // courseware
    fun insertImage(imageUrl: String, w: Int, h: Int)

    fun insertPpt(dir: String, scenes: List<Scene>, title: String)

    fun insertProjectorPpt(taskUuid: String, prefixUrl: String, title: String)

    fun insertVideo(videoUrl: String, title: String)

    fun insertApp(kind: String)

    fun observeRoomPhase(): Flow<BoardPhase>

    fun observeRoomError(): Flow<BoardError>
}