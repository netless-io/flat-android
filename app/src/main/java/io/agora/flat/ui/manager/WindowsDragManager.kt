package io.agora.flat.ui.manager

import android.app.Activity
import android.graphics.Rect
import android.widget.FrameLayout
import dagger.hilt.android.scopes.ActivityScoped
import io.agora.flat.common.board.WhiteSyncedState
import io.agora.flat.common.board.WindowInfo
import io.agora.flat.ui.activity.play.UserWindowUiState
import io.agora.flat.common.rtc.RtcVideoController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.sqrt

@ActivityScoped
class WindowsDragManager @Inject constructor(
    val activity: Activity,
    val rtcVideoController: RtcVideoController,
    val syncedState: WhiteSyncedState,
) {
    companion object {
        val EMPTY_RECT = Rect()

        fun getMaximizeWindowsInfo(size: Int): Array<WindowInfo> {
            if (size == 0) return emptyArray()
            val result = mutableListOf<WindowInfo>()

            val col = ceil(sqrt(size.toDouble())).toInt()
            val row = ceil(size.toDouble() / col).toInt()

            val h = 1f / col
            val w = 1f / col
            val offsetY = (1f - row * h) / 2
            var lx: Float
            var ly: Float
            for (i in 0 until row) {
                ly = offsetY + i * h
                val curCol = if (i == row - 1) size - i * col else col
                val offsetX = (col - curCol) * w / 2
                for (j in 0 until curCol) {
                    lx = offsetX + j * w
                    result.add(WindowInfo(lx, ly, 0, w, h))
                }
            }

            return result.toTypedArray()
        }
    }

    private var _dragState = MutableStateFlow(DragState.Start)

    private val scope = MainScope()
    private var uuid: String = ""
    private var windowsMap = mutableMapOf<String, UserWindowUiState>()

    init {
        scope.launch {
            syncedState.observeUserWindows().collect { userWindows ->

            }
        }
    }

    fun observeDragState() = _dragState.asStateFlow()

    fun startDrag(uuid: String) {
        this.uuid = uuid
        _dragState.value = DragState.Forward
    }

    fun stopDrag(done: Boolean) {
        this.uuid = ""
        _dragState.value = if (done) DragState.End else DragState.Start
    }

    fun startMove(uuid: String) {
        this.uuid = uuid
        _dragState.value = DragState.Reverse
    }

    fun stopMove(done: Boolean) {
        this.uuid = ""
        _dragState.value = if (done) DragState.Start else DragState.End
    }

    fun isOnBoard(uuid: String): Boolean {
        return windowsMap.containsKey(uuid)
    }

    /**
     * 设置用户视频列表视频
     */
    fun setupUserVideo(container: FrameLayout, uid: Int) {
        rtcVideoController.setupUserVideo(container, uid)
    }

    fun currentUUID(): String {
        return uuid
    }

    fun setWindowState(uuid: String, windowUiState: UserWindowUiState) {
        windowsMap[uuid] = windowUiState
    }

    fun scaleWindow(uuid: String, scale: Float) {
        val windowState = windowsMap[uuid] ?: return
        windowsMap[uuid] = windowState.copy(
            width = windowState.width * scale,
            height = windowState.height * scale,
        )
    }

    fun updateWindowCenter(uuid: String, x: Float, y: Float) {
        val windowInfo = windowsMap[uuid] ?: return
        windowsMap[uuid] = windowInfo.copy(
            centerX = x,
            centerY = y,
        )
    }

    fun getWindowState(uuid: String): UserWindowUiState? {
        return windowsMap[uuid]
    }

    fun getWindowRect(uuid: String = currentUUID()): Rect {
        return windowsMap[uuid]?.getRect() ?: EMPTY_RECT
    }

    fun removeWindowState(uuid: String) {
        windowsMap.remove(uuid)
    }

    fun getWindowsMap(): MutableMap<String, UserWindowUiState> {
        return windowsMap
    }

    fun setWindowMap(targetState: MutableMap<String, UserWindowUiState>) {
        windowsMap = targetState
    }
}

enum class DragState {
    Start,
    Forward,
    Reverse,
    End,
}