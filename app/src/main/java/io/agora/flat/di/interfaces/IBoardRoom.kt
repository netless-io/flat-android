package io.agora.flat.di.interfaces

import com.herewhite.sdk.domain.ConvertedFiles
import com.herewhite.sdk.domain.MemberState
import io.agora.board.fast.FastboardView
import io.agora.flat.common.board.BoardRoomPhase
import io.agora.flat.common.board.BoardSceneState
import io.agora.flat.common.board.UndoRedoState
import kotlinx.coroutines.flow.Flow

interface IBoardRoom {
    fun initSdk(fastboardView: FastboardView)

    fun setDarkMode(dark: Boolean)

    fun join(roomUUID: String, roomToken: String, userId: String, writable: Boolean)

    fun release()

    fun setWritable(writable: Boolean)

    fun setDeviceInputEnable(enable: Boolean)

    fun hideAllOverlay()

    // courseware
    fun insertImage(imageUrl: String, w: Int, h: Int)

    fun insertPpt(dir: String, files: ConvertedFiles, title: String)

    fun insertVideo(videoUrl: String, title: String)

    fun observeSceneState(): Flow<BoardSceneState>
    fun observeMemberState(): Flow<MemberState>
    fun observeUndoRedoState(): Flow<UndoRedoState>
    fun observeRoomPhase(): Flow<BoardRoomPhase>
}