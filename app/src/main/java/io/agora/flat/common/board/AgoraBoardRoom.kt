package io.agora.flat.common.board

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.herewhite.sdk.domain.*
import com.herewhite.sdk.domain.WindowPrefersColorScheme.Dark
import com.herewhite.sdk.domain.WindowPrefersColorScheme.Light
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
import io.agora.board.fast.Fastboard
import io.agora.board.fast.FastboardView
import io.agora.board.fast.extension.FastResource
import io.agora.board.fast.model.FastAppliance
import io.agora.board.fast.model.FastRegion
import io.agora.board.fast.model.FastRoomOptions
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.FlatBoardException
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.BoardRoom
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.util.dp
import io.agora.flat.util.getAppVersion
import io.agora.flat.util.px2dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ActivityRetainedScoped
class AgoraBoardRoom @Inject constructor(
    val userRepository: UserRepository,
    val syncedClassState: SyncedClassState,
    val appKVCenter: AppKVCenter,
    val logger: Logger
) : BoardRoom {
    private lateinit var fastboard: Fastboard
    private lateinit var fastboardView: FastboardView
    private var fastRoom: FastRoom? = null
    private var darkMode: Boolean = false
    private var rootRoomController: RoomControllerGroup? = null
    private var boardRoomPhase = MutableStateFlow<BoardRoomPhase>(BoardRoomPhase.Init)
    private val activityContext: Context by lazy { fastboardView.context }
    private val flatNetlessUA: List<String> by lazy {
        listOf(
            "fastboard/${Fastboard.VERSION}",
            "FLAT/NETLESS@${activityContext.getAppVersion()}",
        )
    }

    override fun initSdk(fastboardView: FastboardView) {
        this.fastboardView = fastboardView
        this.fastboard = fastboardView.fastboard
    }

    override fun setRoomController(rootRoomController: RoomControllerGroup) {
        this.rootRoomController = rootRoomController
        fastRoom?.rootRoomController = rootRoomController
    }

    override suspend fun join(
        roomUUID: String,
        roomToken: String,
        region: String,
        writable: Boolean
    ): Boolean = suspendCoroutine { cont ->
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
            windowParams.prefersColorScheme = if (darkMode) Dark else Light
            windowParams.collectorStyles = getCollectorStyle()
            windowParams.scrollVerticalOnly = true
            windowParams.stageStyle = "box-shadow: 0 0 0"

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
                logger.d("[BOARD] room phase change to ${phase.name}")
                when (phase) {
                    RoomPhase.connecting -> boardRoomPhase.value = BoardRoomPhase.Connecting
                    RoomPhase.connected -> boardRoomPhase.value = BoardRoomPhase.Connected
                    RoomPhase.disconnected -> boardRoomPhase.value = BoardRoomPhase.Disconnected
                    else -> {}
                }
            }

            override fun onRoomReadyChanged(fastRoom: FastRoom) {
                if (syncedClassState is WhiteSyncedState && fastRoom.isReady) {
                    syncedClassState.resetRoom(fastRoom)
                }
            }
        })

        if (rootRoomController != null) {
            fastRoom?.rootRoomController = rootRoomController
            updateRoomController(writable)
        }

        val fastResource = object : FastResource() {
            override fun createApplianceBackground(darkMode: Boolean): Drawable? {
                return ContextCompat.getDrawable(activityContext, R.drawable.ic_class_room_icon_bg)
            }

            override fun getIconColor(darkMode: Boolean): ColorStateList? {
                return ContextCompat.getColorStateList(activityContext, R.color.color_class_room_icon)
            }

            override fun getLayoutBackground(darkMode: Boolean): Drawable? {
                return ContextCompat.getDrawable(activityContext, R.drawable.shape_gray_border_round_8_bg)
            }
        }
        fastRoom?.setResource(fastResource)
        setDarkMode(darkMode)

        fastRoom?.join {
            cont.resume(true)
        }
    }

    private fun getCollectorStyle(): HashMap<String, String> {
        val styleMap = HashMap<String, String>()
        styleMap["top"] = "${activityContext.dp(R.dimen.flat_gap_2_0)}px"
        styleMap["right"] = "${activityContext.dp(R.dimen.flat_gap_2_0)}px"
        styleMap["width"] = "${activityContext.dp(R.dimen.room_class_button_area_size)}px"
        styleMap["height"] = "${activityContext.dp(R.dimen.room_class_button_area_size)}px"
        styleMap["position"] = "fixed"
        styleMap["border-radius"] = "8px"
        styleMap["border"] = "1px solid rgba(0,0,0,.15)"
        return styleMap
    }

    override fun setDarkMode(dark: Boolean) {
        this.darkMode = dark

        if (::fastboard.isInitialized) {
            val fastStyle = fastboard.fastStyle.apply { isDarkMode = dark }
            fastRoom?.fastStyle = fastStyle
        }
    }

    override fun release() {
        fastRoom?.destroy()
        fastRoom = null
    }

    override suspend fun setWritable(writable: Boolean): Boolean = suspendCoroutine {
        if (fastRoom?.isWritable == writable) {
            it.resume(writable)
            return@suspendCoroutine
        }
        fastRoom?.room?.setWritable(writable, object : Promise<Boolean> {
            override fun then(success: Boolean) {
                logger.d("[BoardRoom] set writable result $success")
                it.resume(success)
            }

            override fun catchEx(t: SDKError) {
                logger.w("[BoardRoom] set writable error ${t.jsStack}")
                it.resumeWithException(t)
            }
        }) ?: it.resumeWithException(FlatBoardException("[BoardRoom] room not ready"))
    }

    override suspend fun setAllowDraw(allow: Boolean) {
        fastRoom?.room?.disableDeviceInputs(!allow)
        fastboardView.post {
            updateRoomController(allow)
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
        val scale = fastRoom?.room?.roomState?.cameraState?.scale ?: 1.0
        // Images are limited to a maximum of 1 / 4
        val limitWidth = (activityContext.px2dp(fastboardView.width) / scale / 4).toInt()

        val targetW: Int
        val targetH: Int
        if (w > limitWidth) {
            targetW = limitWidth
            targetH = limitWidth * h / w
        } else {
            targetW = w
            targetH = h
        }

        fastRoom?.insertImage(imageUrl, targetW, targetH)
        // switch to SELECTOR when insertImage
        fastRoom?.setAppliance(FastAppliance.SELECTOR)
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
        fastRoom?.insertVideo(videoUrl, title)
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