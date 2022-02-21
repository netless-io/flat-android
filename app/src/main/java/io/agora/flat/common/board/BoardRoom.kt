package io.agora.flat.common.board

import android.content.Context
import android.util.Log
import com.herewhite.sdk.RoomParams
import com.herewhite.sdk.WhiteSdkConfiguration
import com.herewhite.sdk.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastSdk
import io.agora.board.fast.FastboardView
import io.agora.board.fast.model.FastRoomOptions
import io.agora.board.fast.model.FastSdkOptions
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.flat.Constants
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.IBoardRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@ActivityRetainedScoped
class BoardRoom @Inject constructor(
    @ApplicationContext
    val context: Context,
    val userRepository: UserRepository,
    val appKVCenter: AppKVCenter,
) : IBoardRoom {
    companion object {
        const val TAG = "BoardRoom"
    }

    private lateinit var fastSdk: FastSdk
    private var fastboardView: FastboardView? = null
    private var fastRoom: FastRoom? = null
    private var darkMode: Boolean = false
    private var sceneState = MutableStateFlow<BoardSceneState?>(null)
    private var memberState = MutableStateFlow<MemberState?>(null)
    private var undoRedoState = MutableStateFlow(UndoRedoState(0, 0))
    private var boardRoomPhase = MutableStateFlow<BoardRoomPhase>(BoardRoomPhase.Init)

    override fun initSdk(fastboardView: FastboardView) {
        this.fastboardView = fastboardView

        val configuration = WhiteSdkConfiguration(Constants.NETLESS_APP_IDENTIFIER).apply {
            isUserCursor = true
            useMultiViews = true
            isLog = true
        }
        val options = FastSdkOptions(Constants.NETLESS_APP_IDENTIFIER)
        options.configuration = configuration
        fastSdk = fastboardView.getFastSdk(options)
    }

    override fun setRoomController(rootRoomController: RoomControllerGroup) {
        this.fastboardView?.rootRoomController = rootRoomController
    }

    override fun join(roomUUID: String, roomToken: String, userId: String, writable: Boolean) {
        val roomParams = RoomParams(roomUUID, roomToken, userId).apply {
            val styleMap = hashMapOf(
                "bottom" to "30px",
                "right" to "44px",
                "position" to "fixed",
            )
            windowParams = WindowParams()
                .setChessboard(false)
                .setDebug(true)
                .setCollectorStyles(styleMap)
                .setContainerSizeRatio(9.0f / 16)
            windowParams.setPrefersColorScheme(if (darkMode) WindowPrefersColorScheme.Dark else WindowPrefersColorScheme.Light)

            isWritable = writable
            isDisableNewPencil = false
            userPayload = UserPayload(
                userId = userRepository.getUserUUID(),
                nickName = userRepository.getUsername(),
                cursorName = userRepository.getUsername()
            )
            isUseNativeWebSocket = appKVCenter.isNetworkAcceleration()
        }
        val options = FastRoomOptions(roomUUID, roomToken, userId, writable).apply {
            this.roomParams = roomParams
        }

        fastSdk.setErrorHandler { error ->
            boardRoomPhase.value = BoardRoomPhase.Error(error.message ?: "no error message")
        }

        fastSdk.setRoomPhaseHandler { phase ->
            Log.d(TAG, "onPhaseChanged:${phase.name}")
            when (phase) {
                RoomPhase.connecting -> boardRoomPhase.value = BoardRoomPhase.Connecting
                RoomPhase.connected -> boardRoomPhase.value = BoardRoomPhase.Connected
                RoomPhase.disconnected -> boardRoomPhase.value = BoardRoomPhase.Disconnected
            }
        }

        fastRoom = fastSdk.joinRoom(options)
    }

    override fun setDarkMode(dark: Boolean) {
        this.darkMode = dark

        if (::fastSdk.isInitialized) {
            val fastStyle = fastSdk.fastStyle.apply {
                isDarkMode = dark
            }
            fastSdk.fastStyle = fastStyle
        }
    }

    override fun release() {
        fastSdk.destroy()
    }

    override fun setWritable(writable: Boolean) {
        fastRoom?.setWritable(writable)
    }

    override fun setDeviceInputEnable(enable: Boolean) {

    }

    override fun hideAllOverlay() {
        fastSdk.overlayManger.hideAll()
    }

    override fun insertImage(imageUrl: String, w: Int, h: Int) {
        fastRoom?.insertImage(imageUrl, w, h)
    }

    override fun insertPpt(dir: String, files: ConvertedFiles, title: String) {
        val param = WindowAppParam.createSlideApp(dir, files.scenes, title)
        fastRoom?.room?.addApp(param, null)
    }

    override fun insertVideo(videoUrl: String, title: String) {
        val param = WindowAppParam.createMediaPlayerApp(videoUrl, title)
        fastRoom?.room?.addApp(param, null)
    }

    override fun observeSceneState(): Flow<BoardSceneState> {
        return sceneState.asStateFlow().filterNotNull()
    }

    override fun observeMemberState(): Flow<MemberState> {
        return memberState.asStateFlow().filterNotNull()
    }

    override fun observeUndoRedoState(): Flow<UndoRedoState> {
        return undoRedoState.asStateFlow()
    }

    override fun observeRoomPhase(): Flow<BoardRoomPhase> {
        return boardRoomPhase.asStateFlow()
    }

    /**
     * payload example
     * {
     *  "uid":"3e092001-eb7e-4da5-a715-90452fde3194",
     *  "nickName":"aderan",
     *  "userId":"3e092001-eb7e-4da5-a715-90452fde3194",
     *  "cursorName":"aderan"
     * }
     */
    private data class UserPayload(val userId: String, val nickName: String, val cursorName: String)
}