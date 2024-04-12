package io.agora.flat.common.board

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomPhase
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.Scene
import com.herewhite.sdk.domain.WindowAppParam
import com.herewhite.sdk.domain.WindowPrefersColorScheme.Dark
import com.herewhite.sdk.domain.WindowPrefersColorScheme.Light
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.board.fast.FastException
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
import io.agora.board.fast.Fastboard
import io.agora.board.fast.FastboardView
import io.agora.board.fast.extension.FastResource
import io.agora.board.fast.model.FastAppliance
import io.agora.board.fast.model.FastRegion
import io.agora.board.fast.model.FastRoomOptions
import io.agora.board.fast.model.FastUserPayload
import io.agora.board.fast.ui.RoomControllerGroup
import io.agora.flat.R
import io.agora.flat.common.FlatBoardException
import io.agora.flat.data.AppEnv
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
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ActivityRetainedScoped
class AgoraBoardRoom @Inject constructor(
    val userRepository: UserRepository,
    val syncedClassState: SyncedClassState,
    val appKVCenter: AppKVCenter,
    val appEnv: AppEnv,
    val logger: Logger,
) : BoardRoom {
    private lateinit var fastboard: Fastboard
    private lateinit var fastboardView: FastboardView

    private var fastRoom: FastRoom? = null
    private var darkMode: Boolean = false
    private var rootRoomController: RoomControllerGroup? = null
    private var boardPhase = MutableStateFlow<BoardPhase>(BoardPhase.Init)
    private var boardError = MutableStateFlow<BoardError?>(null)
    private val activityContext: Context by lazy { fastboardView.context }
    private val flatNetlessUA: List<String> by lazy {
        listOf(
            "fastboard/${Fastboard.VERSION}",
            "FLAT/NETLESS@${activityContext.getAppVersion()}",
        )
    }

    override fun setupView(fastboardView: FastboardView) {
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
    ) {
        val fastRoomOptions = FastRoomOptions(
            appEnv.whiteAppId,
            roomUUID,
            roomToken,
            userRepository.getUserUUID(),
            region.toFastRegion(),
            writable
        ).apply {
            userPayload = FastUserPayload(userRepository.getUsername())
        }

        fastRoomOptions.sdkConfiguration = fastRoomOptions.sdkConfiguration.apply {
            isLog = true
            netlessUA = flatNetlessUA
            isEnableSyncedStore = true
        }

        fastRoomOptions.roomParams = fastRoomOptions.roomParams.apply {
            windowParams.prefersColorScheme = if (darkMode) Dark else Light
            windowParams.collectorStyles = getCollectorStyle()
            windowParams.scrollVerticalOnly = true
            windowParams.stageStyle = "box-shadow: 0 0 0"

            disableEraseImage = true
        }
        fastRoom = fastboard.createFastRoom(fastRoomOptions)

        fastRoom?.addListener(object : FastRoomListener {
            override fun onRoomPhaseChanged(phase: RoomPhase) {
                logger.i("[BOARD] room phase change to ${phase.name}")
                when (phase) {
                    RoomPhase.connecting -> boardPhase.value = BoardPhase.Connecting
                    RoomPhase.connected -> boardPhase.value = BoardPhase.Connected
                    RoomPhase.disconnected -> boardPhase.value = BoardPhase.Disconnected
                    else -> {}
                }
            }

            override fun onRoomReadyChanged(fastRoom: FastRoom) {
                logger.i("[BOARD] room ready changed ${fastRoom.isReady}")
                if (syncedClassState is WhiteSyncedState && fastRoom.isReady) {
                    syncedClassState.resetRoom(fastRoom)
                }
            }

            override fun onFastError(error: FastException) {
                if (error.code == FastException.ROOM_KICKED) {
                    boardError.value = BoardError.Kicked
                }
            }
        })

        rootRoomController?.let {
            fastRoom?.rootRoomController = it
            updateRoomController(writable)
        }

        val fastResource = object : FastResource() {
            override fun getBackgroundColor(darkMode: Boolean): Int {
                return ContextCompat.getColor(
                    activityContext,
                    if (darkMode) R.color.flat_gray_7 else R.color.flat_blue_0
                )
            }

            override fun getBoardBackgroundColor(darkMode: Boolean): Int {
                return ContextCompat.getColor(activityContext, R.color.flat_day_night_background)
            }

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
        fastRoom?.join()
        fastboard.setWhiteboardRatio(null)
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
        logger.i("[BOARD] set dark mode $dark, fastboard ${::fastboard.isInitialized}")
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
        logger.i("[BoardRoom] set writable $writable, when isWritable ${fastRoom?.isWritable}")
        if (fastRoom?.isWritable == writable) {
            it.resume(writable)
            return@suspendCoroutine
        }
        fastRoom?.room?.setWritable(writable, object : Promise<Boolean> {
            override fun then(success: Boolean) {
                logger.i("[BoardRoom] set writable result $success")
                it.resume(success)
            }

            override fun catchEx(t: SDKError) {
                logger.w("[BoardRoom] set writable error ${t.jsStack}")
                it.resumeWithException(t)
            }
        }) ?: it.resumeWithException(FlatBoardException("[BoardRoom] room not ready"))
    }

    override suspend fun setAllowDraw(allow: Boolean) {
        logger.i("[BoardRoom] set allow draw $allow, when isWritable ${fastRoom?.isWritable}")
        if (fastRoom?.isWritable == true) {
            fastRoom?.room?.disableOperations(!allow)
            fastRoom?.room?.disableWindowOperation(!allow)
        }
        fastboardView.post { updateRoomController(allow) }
    }

    private fun updateRoomController(allow: Boolean) {
        if (allow) {
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
        // Images are limited to a maximum of 0.4 times the width of the screen.
        val limitWidth = (activityContext.px2dp(fastboardView.width) / scale * 0.4).toInt()

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

    override fun insertPpt(dir: String, scenes: List<Scene>, title: String) {
        val param = WindowAppParam.createSlideApp(dir, scenes.toTypedArray(), title)
        fastRoom?.room?.addApp(param, null)
    }

    override fun insertProjectorPpt(taskUuid: String, prefixUrl: String, title: String) {
        val param = WindowAppParam.createSlideApp(taskUuid, prefixUrl, title)
        fastRoom?.room?.addApp(param, null)
    }

    override fun insertVideo(videoUrl: String, title: String) {
        fastRoom?.insertVideo(videoUrl, title)
    }

    override fun insertApp(kind: String) {
        fastRoom?.room?.addApp(WindowAppParam(kind, null, null), null)
    }

    override fun observeRoomPhase(): Flow<BoardPhase> {
        return boardPhase.asStateFlow()
    }

    override fun observeRoomError(): Flow<BoardError> {
        return boardError.asStateFlow().filterNotNull()
    }

    private fun String.toFastRegion(): FastRegion {
        val region = FastRegion.values().find { it.name.lowercase().replace('_', '-') == this }
        return region ?: FastRegion.CN_HZ
    }
}