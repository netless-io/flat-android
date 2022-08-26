package io.agora.flat.common.board

import android.content.Context
import android.util.Log
import com.herewhite.sdk.domain.ConvertedFiles
import com.herewhite.sdk.domain.RoomPhase
import com.herewhite.sdk.domain.WindowAppParam
import com.herewhite.sdk.domain.WindowPrefersColorScheme
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
import io.agora.board.fast.Fastboard
import io.agora.board.fast.FastboardView
import io.agora.board.fast.model.FastRegion
import io.agora.board.fast.model.FastRoomOptions
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.flat.Constants
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.IBoardRoom
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.util.getAppVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ActivityRetainedScoped
class BoardRoom @Inject constructor(
    @ApplicationContext
    val context: Context,
    val userRepository: UserRepository,
    val syncedClassState: SyncedClassState,
    val appKVCenter: AppKVCenter,
) : IBoardRoom {
    companion object {
        const val TAG = "BoardRoom"
    }

    private lateinit var fastboard: Fastboard
    private var fastboardView: FastboardView? = null
    private var fastRoom: FastRoom? = null
    private var darkMode: Boolean = false
    private var rootRoomController: RoomControllerGroup? = null
    private var boardRoomPhase = MutableStateFlow<BoardRoomPhase>(BoardRoomPhase.Init)
    private var flatNetlessUA = listOf(
        "fastboard/${Fastboard.VERSION}",
        "FLAT/NETLESS@${context.getAppVersion()}"
    )

    override fun initSdk(fastboardView: FastboardView) {
        this.fastboardView = fastboardView
        this.fastboard = fastboardView.fastboard
    }

    override fun setRoomController(rootRoomController: RoomControllerGroup) {
        this.rootRoomController = rootRoomController
        fastRoom?.rootRoomController = rootRoomController
    }

    override fun join(roomUUID: String, roomToken: String, region: String, writable: Boolean) {
        val fastRoomOptions = FastRoomOptions(
            Constants.NETLESS_APP_IDENTIFIER,
            roomUUID,
            roomToken,
            userRepository.getUserUUID(),
            region.toFastRegion(),
            writable
        )
        val sdkConfiguration = fastRoomOptions.sdkConfiguration.apply {
            isLog = true
            isUserCursor = true
            netlessUA = flatNetlessUA
            isEnableSyncedStore = true
        }
        fastRoomOptions.sdkConfiguration = sdkConfiguration

        val roomParams = fastRoomOptions.roomParams.apply {
            windowParams.prefersColorScheme = if (darkMode) {
                WindowPrefersColorScheme.Dark
            } else {
                WindowPrefersColorScheme.Light
            }
            userPayload = UserPayload(
                userId = userRepository.getUserUUID(),
                nickName = userRepository.getUsername(),
                cursorName = userRepository.getUsername(),
            )
            isUseNativeWebSocket = appKVCenter.isNetworkAcceleration()
            disableEraseImage = true
        }
        fastRoomOptions.roomParams = roomParams

        fastRoom = fastboard.createFastRoom(fastRoomOptions)
        fastRoom?.addListener(object : FastRoomListener {
            override fun onRoomPhaseChanged(phase: RoomPhase) {
                Log.d(TAG, "onPhaseChanged:${phase.name}")
                when (phase) {
                    RoomPhase.connecting -> boardRoomPhase.value = BoardRoomPhase.Connecting
                    RoomPhase.connected -> boardRoomPhase.value = BoardRoomPhase.Connected
                    RoomPhase.disconnected -> boardRoomPhase.value = BoardRoomPhase.Disconnected
                    else -> {}
                }
            }

            override fun onRoomReadyChanged(fastRoom: FastRoom) {
                super.onRoomReadyChanged(fastRoom)
                if (syncedClassState is WhiteSyncedState && fastRoom.isReady) {
                    syncedClassState.resetRoom(fastRoom)
                }
            }
        })

        if (rootRoomController != null) {
            fastRoom?.rootRoomController = rootRoomController
            updateRoomController(writable)
        }
        setDarkMode(darkMode)

        fastRoom?.join()
    }

    override fun setDarkMode(dark: Boolean) {
        this.darkMode = dark

        if (::fastboard.isInitialized) {
            val fastStyle = fastboard.fastStyle.apply {
                isDarkMode = dark
            }
            fastRoom?.fastStyle = fastStyle
        }
    }

    override fun release() {
        fastRoom?.destroy()
        fastRoom = null
    }

    override fun setWritable(writable: Boolean) {
        if (fastRoom?.room?.writable != writable) {
            fastRoom?.setWritable(writable)
        }
        fastboardView?.post {
            updateRoomController(writable)
        }
    }

    private fun updateRoomController(writable: Boolean) {
        if (writable) {
            fastRoom?.rootRoomController?.show()
        } else {
            fastRoom?.rootRoomController?.hide()
        }
    }

    override fun hideAllOverlay() {
        fastRoom?.overlayManger?.hideAll()
    }

    override fun insertImage(imageUrl: String, w: Int, h: Int) {
        fastRoom?.insertImage(imageUrl, w, h)
    }

    override fun insertPpt(dir: String, files: ConvertedFiles, title: String) {
        val param = WindowAppParam.createSlideApp(dir, files.scenes, title)
        fastRoom?.room?.addApp(param, null)
    }

    override fun insertProjectorPpt(taskUuid: String, prefixUrl: String, title: String) {
        val param = WindowAppParam.createSlideApp(taskUuid, prefixUrl, title)
        fastRoom?.room?.addApp(param, null)
    }

    override fun insertVideo(videoUrl: String, title: String) {
        val param = WindowAppParam.createMediaPlayerApp(videoUrl, title)
        fastRoom?.room?.addApp(param, null)
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
     *  "avatar":"https://user_avatar_path"
     * }
     */
    private data class UserPayload(
        val userId: String,
        val nickName: String,
        val cursorName: String,
        val avatar: String? = null,
    )

    private fun String.toFastRegion(): FastRegion {
        val region = FastRegion.values().find { it.name.lowercase().replace('_', '-') == this }
        return region ?: FastRegion.CN_HZ
    }
}