package io.agora.flat.di.interfaces

import com.herewhite.sdk.WhiteboardView
import com.herewhite.sdk.domain.ConvertedFiles
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.ViewMode
import io.agora.flat.common.board.BoardSceneState
import io.agora.flat.common.board.UndoRedoState
import kotlinx.coroutines.flow.Flow

interface BoardRoomApi {
    fun initSdk(whiteboardView: WhiteboardView)

    fun join(roomUUID: String, roomToken: String, userId: String)

    fun release()

    fun setWritable(writable: Boolean)

    fun setViewMode(viewMode: ViewMode)

    fun resetView()

    // memberstate
    fun setAppliance(name: String)
    fun setStrokeColor(color: IntArray)
    fun setStrokeWidth(toDouble: Double)
    fun deleteSelection()

    // canvas
    fun undo()
    fun redo()

    // slide
    fun startPage()
    fun finalPage()
    fun prevPage()
    fun nextPage()
    fun addSlideToNext()
    fun deleteCurrentSlide()
    fun refreshSceneState()
    fun cleanScene(retainPpt: Boolean = true)
    fun setSceneIndex(index:Int)

    // courseware
    fun insertImage(imageUrl: String, w: Int, h: Int)

    fun insertPpt(dir: String, files: ConvertedFiles, title: String)

    fun insertVideo(videoUrl: String, title: String)

    fun observerSceneState(): Flow<BoardSceneState>
    fun observerMemberState(): Flow<MemberState>
    fun observerUndoRedoState(): Flow<UndoRedoState>
}