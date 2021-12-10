package io.agora.flat.common.board

import android.content.Context
import android.util.Log
import android.webkit.WebView
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.BuildConfig
import io.agora.flat.Constants
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.IBoardRoom
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

@ActivityRetainedScoped
class BoardRoom @Inject constructor(
    @ApplicationContext
    val context: Context,
    val userRepository: UserRepository,
) :
    IBoardRoom {
    companion object {
        const val TAG = "BoardRoom"
    }

    private lateinit var whiteSdk: WhiteSdk
    private var room: Room? = null
    private var sceneState = MutableStateFlow<BoardSceneState?>(null)
    private var memberState = MutableStateFlow<MemberState?>(null)
    private var undoRedoState = MutableStateFlow(UndoRedoState(0, 0))
    private var boardRoomPhase = MutableStateFlow<BoardRoomPhase>(BoardRoomPhase.Init)

    private var joinRoomCallback = object : Promise<Room> {
        override fun then(room: Room) {
            Log.i(TAG, "join room success")
            this@BoardRoom.room = room
            memberState.value = room.roomState.memberState
            sceneState.value = room.roomState.sceneState.toBoardSceneState()
            updateSerialization(room.writable)
        }

        override fun catchEx(t: SDKError) {
            Log.e(TAG, "join room error $t")
            boardRoomPhase.value = BoardRoomPhase.Error(t.toString())
        }
    }

    private var roomListener = object : RoomListener {
        override fun onPhaseChanged(phase: RoomPhase) {
            Log.d(TAG, "onPhaseChanged:${phase.name}")
            when (phase) {
                RoomPhase.connecting -> boardRoomPhase.value = BoardRoomPhase.Connecting
                RoomPhase.connected -> boardRoomPhase.value = BoardRoomPhase.Connected
                RoomPhase.reconnecting -> {; }
                RoomPhase.disconnecting -> {; }
                RoomPhase.disconnected -> boardRoomPhase.value = BoardRoomPhase.Disconnected
            }
        }

        override fun onDisconnectWithError(e: Exception) {
            Log.e(TAG, "onDisconnectWithError:${e.message}")
            boardRoomPhase.value = BoardRoomPhase.Error(e.toString())
        }

        override fun onKickedWithReason(reason: String) {
            Log.i(TAG, "onKickedWithReason:${reason}")
        }

        override fun onRoomStateChanged(modifyState: RoomState) {
            Log.d(TAG, "onRoomStateChanged:${modifyState}")
            if (modifyState.memberState != null) {
                memberState.value = room!!.roomState.memberState
            }
            if (modifyState.sceneState != null) {
                sceneState.value = room!!.roomState.sceneState.toBoardSceneState()
            }
        }

        override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
            Log.d(TAG, "onCanUndoStepsUpdate:${canUndoSteps}")
            undoRedoState.value = undoRedoState.value.copy(undoCount = canUndoSteps)
        }

        override fun onCanRedoStepsUpdate(canRedoSteps: Long) {
            Log.d(TAG, "onCanRedoStepsUpdate:${canRedoSteps}")
            undoRedoState.value = undoRedoState.value.copy(redoCount = canRedoSteps)
        }

        override fun onCatchErrorWhenAppendFrame(userId: Long, error: Exception?) {
            Log.w(TAG, "onCatchErrorWhenAppendFrame${error}")
            boardRoomPhase.value = BoardRoomPhase.Error("$userId $error")
        }
    }

    override fun initSdk(whiteboardView: WhiteboardView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        val configuration = WhiteSdkConfiguration(Constants.NETLESS_APP_IDENTIFIER, true)
        configuration.isUserCursor = true
        configuration.useMultiViews = true

        whiteSdk = WhiteSdk(whiteboardView, context, configuration)
        whiteSdk.setCommonCallbacks(object : CommonCallback {
            override fun onMessage(message: JSONObject) {
                Log.i(TAG, message.toString())
            }

            override fun sdkSetupFail(error: SDKError) {
                Log.e(TAG, "sdkSetupFail $error")
                boardRoomPhase.value = BoardRoomPhase.Error(error.toString())
            }

            override fun throwError(args: Any) {
                Log.e(TAG, "throwError $args")
                boardRoomPhase.value = BoardRoomPhase.Error("throwError $args")
            }
        })
    }

    override fun join(roomUUID: String, roomToken: String, userId: String, writable: Boolean) {
        val roomParams = RoomParams(roomUUID, roomToken, userId).apply {
            val styleMap = HashMap<String, String>()
            styleMap["bottom"] = "30px"
            styleMap["right"] = "44px"
            styleMap["position"] = "fixed"

            windowParams = WindowParams().setChessboard(false).setDebug(true).setCollectorStyles(styleMap)
            isWritable = writable
            isDisableNewPencil = false
            userPayload = UserPayload(
                userId = userRepository.getUserUUID(),
                nickName = userRepository.getUsername(),
                cursorName = userRepository.getUsername()
            )
        }
        whiteSdk.joinRoom(roomParams, roomListener, joinRoomCallback)
    }

    override fun release() {
        whiteSdk.releaseRoom()
        room?.disconnect()
    }

    override fun deleteSelection() {
        room?.deleteOperation()
    }

    override fun setWritable(writable: Boolean) {
        room?.setWritable(writable, object : Promise<Boolean> {
            override fun then(isWritable: Boolean) {
                Log.i(TAG, "set writable result $isWritable")
                updateSerialization(isWritable)
            }

            override fun catchEx(error: SDKError) {
                Log.e(TAG, "set writable error")
            }
        })
    }

    override fun setViewMode(viewMode: ViewMode) {
        room?.setViewMode(viewMode)
    }

    override fun resetView() {
        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                val scene = sceneState.scenes[sceneState.index]
                if (scene.ppt != null) {
                    room?.scalePptToFit()
                } else {
                    room?.moveCamera(CameraConfig().apply {
                        scale = 1.0
                        centerX = 0.0
                        centerY = 0.0
                        animationMode = AnimationMode.Continuous
                    })
                }
            }

            override fun catchEx(t: SDKError?) {
                Log.e(TAG, "reset view error $t")
            }
        })
    }

    override fun setAppliance(name: String) {
        room?.memberState = MemberState().apply {
            currentApplianceName = name
        }
    }

    override fun setStrokeColor(color: IntArray) {
        room?.memberState = room?.memberState?.apply {
            strokeColor = color
        }
    }

    override fun setStrokeWidth(width: Double) {
        room?.memberState = room?.memberState?.apply {
            strokeWidth = width
        }
    }

    override fun undo() {
        room?.undo()
    }

    override fun redo() {
        room?.redo()
    }

    override fun startPage() {
        room?.setSceneIndex(0, null)
    }

    override fun prevPage() {
        room?.pptPreviousStep()
    }

    override fun nextPage() {
        room?.pptNextStep()
    }

    override fun finalPage() {
        room?.sceneState?.apply {
            room?.setSceneIndex(scenes.size - 1, null)
        }
    }

    override fun addSlideToNext() {
        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                val sceneDir = sceneState.scenePath.substringBeforeLast('/')
                val targetIndex = sceneState.index + 1

                val s = Scene(UUID.randomUUID().toString())
                room?.putScenes(sceneDir, arrayOf(s), targetIndex)
                room?.setSceneIndex(targetIndex, null)

                val scenes = sceneState.scenes.toMutableList()
                scenes.add(targetIndex, s)
                val list = scenes.filterNotNull().map {
                    SceneItem(sceneDir + "/" + it.name, it.ppt?.preview)
                }

                this@BoardRoom.sceneState.value = BoardSceneState(list, targetIndex)
            }

            override fun catchEx(t: SDKError?) {

            }
        })
    }

    override fun deleteCurrentSlide() {
        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                val sceneList = sceneState.scenes.toMutableList()
                val sceneDir = sceneState.scenePath.substringBeforeLast('/')

                room?.removeScenes(sceneState.scenePath)
                sceneList.removeAt(sceneState.index)

                val list = sceneList.filterNotNull().map {
                    SceneItem(sceneDir + "/" + it.name, it.ppt?.preview)
                }

                this@BoardRoom.sceneState.value = BoardSceneState(list, sceneState.index)
            }

            override fun catchEx(t: SDKError?) {

            }
        })
    }

    override fun refreshSceneState() {
        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                this@BoardRoom.sceneState.value = sceneState.toBoardSceneState()
            }

            override fun catchEx(t: SDKError?) {

            }
        })
    }

    override fun cleanScene(retainPpt: Boolean) {
        room?.cleanScene(retainPpt)
    }

    override fun setSceneIndex(index: Int) {
        room?.setSceneIndex(index, null)
    }

    override fun insertImage(imageUrl: String, w: Int, h: Int) {
        val uuid = UUID.randomUUID().toString()
        room?.insertImage(ImageInformation().apply {
            this.uuid = uuid
            width = w.toDouble()
            height = h.toDouble()
            centerX = 0.0
            centerY = 0.0
        })
        room?.completeImageUpload(uuid, imageUrl)
    }

    override fun insertPpt(dir: String, files: ConvertedFiles, title: String) {
        val param = WindowAppParam.createSlideApp(dir, files.scenes, title)
        room?.addApp(param, null)
    }

    override fun insertVideo(videoUrl: String, title: String) {
        val param = WindowAppParam.createMediaPlayerApp(videoUrl, title)
        room?.addApp(param, null)
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

    private fun updateSerialization(writable: Boolean) {
        if (writable) {
            room?.disableSerialization(false)
        }
    }

    private fun SceneState.toBoardSceneState(): BoardSceneState {
        val sceneDir = this.scenePath.substringBeforeLast('/')
        val list = this.scenes.filterNotNull().map {
            SceneItem(sceneDir + "/" + it.name, it.ppt?.preview)
        }
        return BoardSceneState(list, this.index)
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