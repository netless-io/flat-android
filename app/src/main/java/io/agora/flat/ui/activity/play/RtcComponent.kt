package io.agora.flat.ui.activity.play

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.SoundPool
import android.view.DragEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.airbnb.lottie.LottieAnimationView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.Config
import io.agora.flat.R
import io.agora.flat.common.board.UserWindows
import io.agora.flat.common.board.WindowInfo
import io.agora.flat.common.rtc.RtcEvent
import io.agora.flat.data.model.RoomUser
import io.agora.flat.databinding.ComponentFullscreenBinding
import io.agora.flat.databinding.ComponentUserWindowsBinding
import io.agora.flat.databinding.ComponentVideoListBinding
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.event.*
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.manager.WindowsDragManager
import io.agora.flat.ui.view.PaddingItemDecoration
import io.agora.flat.ui.view.UserWindowLayout
import io.agora.flat.ui.viewmodel.RtcVideoController
import io.agora.flat.util.dp2px
import io.agora.flat.util.renderTo
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.min


class RtcComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
    private val fullScreenLayout: FrameLayout,
    private val shareScreenContainer: FrameLayout,
    private val userWindowsLayout: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface RtcComponentEntryPoint {
        fun rtcApi(): RtcApi
        fun rtcVideoController(): RtcVideoController
        fun windowsDragManager(): WindowsDragManager
        fun logger(): Logger
        fun syncedState(): SyncedClassState
    }

    private lateinit var fullScreenBinding: ComponentFullscreenBinding
    private lateinit var videoListBinding: ComponentVideoListBinding

    private lateinit var rtcApi: RtcApi
    private lateinit var rtcVideoController: RtcVideoController
    private lateinit var windowsDragManager: WindowsDragManager
    private lateinit var logger: Logger
    private lateinit var syncedState: SyncedClassState
    private val viewModel: ClassRoomViewModel by activity.viewModels()

    private lateinit var adapter: UserVideoAdapter

    private lateinit var videoAreaAnimator: SimpleAnimator
    private lateinit var fullScreenAnimator: SimpleAnimator
    private lateinit var dragAnimator: SimpleAnimator

    private val soundPool: SoundPool by lazy {
        SoundPool.Builder().apply {
            setMaxStreams(1)
        }.build()
    }

    private val rewardSoundId: Int by lazy {
        soundPool.load(activity.assets.openFd("reward.mp3"), 1)
    }

    override fun onCreate(owner: LifecycleOwner) {
        injectApi()
        initView()
        checkPermission(::observeState)
    }

    private fun injectApi() {
        val entryPoint = EntryPointAccessors.fromActivity(activity, RtcComponentEntryPoint::class.java)
        rtcApi = entryPoint.rtcApi()
        rtcVideoController = entryPoint.rtcVideoController()
        windowsDragManager = entryPoint.windowsDragManager()
        logger = entryPoint.logger()
        syncedState = entryPoint.syncedState()
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            viewModel.videoUsers.collect { users ->
                logger.i("[RTC] videoUsers changed to ${users.size}")
                adapter.updateUsers(users)
                updateUserWindows(users)
                // 处理用户进出时的显示
                if (userCallOut != null) {
                    val findUser = users.find { it.isJoined && it.isOnStage && it.userUUID == userCallOut!!.userUUID }
                    if (findUser == null) {
                        clearCallOutAndNotify()
                        fullScreenAnimator.hide()
                    } else {
                        userCallOut = findUser
                        updateCallOutUser()
                    }
                }

            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.noOptPermission.collect {
                activity.showToast(R.string.class_room_no_operate_permission)
            }
        }

        lifecycleScope.launchWhenResumed {
            RoomOverlayManager.observeShowId().collect { areaId ->
                if (areaId != RoomOverlayManager.AREA_ID_VIDEO_OP_CALL_OUT) {
                    clearCallOut()
                }

                videoListBinding.clickHandleView.show(areaId != RoomOverlayManager.AREA_ID_NO_OVERLAY) {
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.videoAreaShown.collect { shown ->
                if (shown) {
                    videoAreaAnimator.show()
                } else {
                    videoAreaAnimator.hide()
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            rtcApi.observeRtcEvent().collect { event ->
                logger.i("[RTC] event: $event")

                when (event) {
                    is RtcEvent.UserJoined -> lifecycleScope.launch {
                        rtcVideoController.handlerJoined(event.uid)
                    }
                    is RtcEvent.UserOffline -> lifecycleScope.launch {
                        rtcVideoController.handleOffline(event.uid)
                    }
                    is RtcEvent.VolumeIndication -> {
                        adapter.updateVolume(event.speakers)
                    }
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.classroomEvent.collect { event ->
                when (event) {
                    is RewardReceived -> {
                        handleReward(event.userUUID)
                    }
                    else -> {}
                }
            }
        }

        observeUserWindows()
    }

    private var windowsState: UserWindows = UserWindows()

    private fun observeUserWindows() {
        lifecycleScope.launchWhenResumed {
            combine(syncedState.observeUserWindows(), viewModel.videoUsersMap) { userWindows, videoUsers ->
                Pair(userWindows, videoUsers)
            }.collect { pair ->
                windowsState = pair.first
                val videoUsers = pair.second

                fullOnStage = windowsState.grid.isNotEmpty()
                userWindowsBinding.maskView.isVisible = fullOnStage
                animateStateChanged(windowsDragManager.getWindowsMap(), getWindowsUiState(windowsState), videoUsers)
            }
        }
    }

    private fun getWindowsUiState(windows: UserWindows): MutableMap<String, UserWindowUiState> {
        val targetState = mutableMapOf<String, UserWindowUiState>()
        if (windows.grid.isEmpty()) {
            windows.users.forEach {
                targetState[it.key] = it.value.toUserWindowUiState()
            }
        } else {
            val windowsInfo = WindowsDragManager.getMaximizeWindowsInfo(windows.grid.size)
            windows.grid.forEachIndexed { index, uuid ->
                targetState[uuid] = windowsInfo[index].copy(
                    z = index,
                ).toUserWindowUiState()
            }
        }
        return targetState
    }

    private var fullOnStage = false
    private val scaledTouchSlop = ViewConfiguration.get(activity).scaledTouchSlop

    private fun animateStateChanged(
        state: MutableMap<String, UserWindowUiState>,
        targetState: MutableMap<String, UserWindowUiState>,
        videoUsers: Map<String, RoomUser>,
    ) {
        val toRemove = state.keys - targetState.keys
        val toAdd = targetState.keys - state.keys
        val toUpdate = state.keys - toRemove

        for (uuid in toRemove) {
            removeNewUserWindow(uuid)
            adapter.updateItemByUuid(uuid)
        }

        for (uuid in toAdd) {
            videoUsers[uuid]?.let {
                val rect = adapter.findContainerByUuid(uuid)?.let { container ->
                    getViewRect(container, userWindowsBinding.root)
                } ?: Rect(0, 0, 0, 0)
                addNewUserWindow(
                    user = it,
                    windowUiState = UserWindowUiState(
                        centerX = rect.centerX().toFloat(),
                        centerY = rect.centerY().toFloat(),
                        width = rect.width().toFloat(),
                        height = rect.height().toFloat(),
                        index = targetState[uuid]?.index ?: atomIndex.getAndIncrement(),
                    )
                )
                adapter.updateItemByUuid(uuid)
            }
        }

        if (toRemove.isEmpty() && toAdd.isEmpty()) {
            val needAnimate = toUpdate.any {
                val a = state[it]!!
                val b = targetState[it]!!
                abs(a.height - b.height) > scaledTouchSlop ||
                        abs(a.width - b.width) > scaledTouchSlop ||
                        abs(a.centerX - b.centerX) > scaledTouchSlop ||
                        abs(a.centerY - b.centerY) > scaledTouchSlop
            }
            if (!needAnimate) {
                windowsDragManager.setWindowMap(targetState)
                refreshUserWindows()
                return
            }
        }

        val r = Rect()
        dragAnimator = SimpleAnimator(
            onUpdate = { value ->
                val all = toAdd + toUpdate
                for (uuid in all) {
                    val s = state[uuid]?.getRect() ?: Rect(0, 0, 0, 0)
                    val e = targetState[uuid]?.getRect() ?: Rect(0, 0, 0, 0)
                    r.lerp(s, e, value)
                    windowLayoutMap[uuid]?.renderTo(r)
                }
            },
            onShowEnd = {
                windowsDragManager.setWindowMap(targetState)
                refreshUserWindows()
            },
        )
        dragAnimator.show()
    }

    private val expandWidth = activity.resources.getDimensionPixelSize(R.dimen.room_class_video_area_width)

    private fun updateVideoContainer(value: Float) {
        val layoutParams = videoListBinding.root.layoutParams
        layoutParams.width = (value * expandWidth).toInt()
        videoListBinding.root.layoutParams = layoutParams

        updateWindowUsersRect((value * expandWidth).toInt())
    }

    private val onClickListener = View.OnClickListener {
        when (it) {
            fullScreenBinding.fullAudioOpt, videoListBinding.audioOpt -> userCallOut?.run {
                viewModel.enableAudio(!it.isSelected, userUUID)
            }
            fullScreenBinding.fullVideoOpt, videoListBinding.videoOpt -> userCallOut?.run {
                viewModel.enableVideo(!it.isSelected, userUUID)
            }
            fullScreenBinding.exitFullScreen -> {
                fullScreenAnimator.hide()
            }
            videoListBinding.enterFullScreen -> {
                hideVideoListOptArea()
                fullScreenAnimator.show()
            }
        }
    }

    private fun initView() {
        fullScreenBinding = ComponentFullscreenBinding.inflate(activity.layoutInflater, fullScreenLayout, true)
        fullScreenBinding.exitFullScreen.setOnClickListener(onClickListener)
        fullScreenBinding.fullAudioOpt.setOnClickListener(onClickListener)
        fullScreenBinding.fullVideoOpt.setOnClickListener(onClickListener)
        fullScreenBinding.root.setOnClickListener { /* disable click pass through */ }
        fullScreenBinding.root.isVisible = false

        videoListBinding = ComponentVideoListBinding.inflate(activity.layoutInflater, rootView, true)
        videoListBinding.videoOpt.setOnClickListener(onClickListener)
        videoListBinding.audioOpt.setOnClickListener(onClickListener)
        videoListBinding.enterFullScreen.setOnClickListener(onClickListener)

        shareScreenContainer.setOnClickListener { /* disable click pass through */ }
        rtcVideoController.shareScreenContainer = shareScreenContainer

        adapter = UserVideoAdapter(viewModel = viewModel, windowsDragManager = windowsDragManager)
        adapter.setOnItemListener(object : UserVideoAdapter.OnItemListener {
            override fun onStartDrag(view: View, user: RoomUser): Boolean {
                if (!viewModel.canDragUser() || windowsDragManager.isOnBoard(user.userUUID)) return false

                windowsDragManager.startDrag(user.userUUID)
                val shadow = View.DragShadowBuilder(userWindowsBinding.dragViewShadow)
                ViewCompat.startDragAndDrop(view, null, shadow, user, 0)
                return true
            }

            override fun onItemClick(view: View, position: Int) {
                val rtcUser = adapter.getUserItem(position)

                start.set(getViewRect(view, fullScreenLayout))
                end.set(0, 0, fullScreenLayout.width, fullScreenLayout.height)

                if (!viewModel.canShowCallOut(rtcUser.userUUID)) {
                    userCallOut = rtcUser
                    hideVideoListOptArea()
                    fullScreenAnimator.show()
                    return
                }

                if (userCallOut != rtcUser) {
                    userCallOut = rtcUser
                    showVideoListOptArea(start)
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_VIDEO_OP_CALL_OUT)
                } else {
                    clearCallOutAndNotify()
                }
            }

            override fun onItemDoubleClick(view: View, user: RoomUser): Boolean {
                if (!viewModel.canDragUser() || windowsDragManager.isOnBoard(user.userUUID)) return false
                syncedState.maximizeWindows(user.userUUID)
                return true
            }

            override fun onDrag(v: View, position: Int, event: DragEvent): Boolean {
                when (event.action) {
                    DragEvent.ACTION_DRAG_ENDED -> {
                        windowsDragManager.stopDrag(event.result)
                    }
                }
                return true
            }

            override fun onSwitchCamera(userId: String, on: Boolean) {
                viewModel.enableVideo(on, userId)
            }

            override fun onSwitchMic(userId: String, on: Boolean) {
                viewModel.enableAudio(enableAudio = on, uuid = userId)
            }

            override fun onAllowDraw(userUUID: String, allow: Boolean) {
                viewModel.updateAllowDraw(userUUID, allow)
            }

            override fun onMuteAll() {
                viewModel.muteAllMic()
            }

            override fun onSendReward(userUUID: String) {
                handleReward(userUUID)
                viewModel.sendReward(userUUID)
            }

            override fun onRestoreUserWindow() {
                syncedState.removeAllWindow()
            }
        })

        videoListBinding.videoList.layoutManager = LinearLayoutManager(activity)
        videoListBinding.videoList.adapter = adapter
        videoListBinding.videoList.addItemDecoration(PaddingItemDecoration(vertical = activity.dp2px(4)))

        videoAreaAnimator = SimpleAnimator(
            onUpdate = ::updateVideoContainer,
            onShowStart = { videoListBinding.root.isVisible = true },
            onHideEnd = { videoListBinding.root.isVisible = false }
        )

        fullScreenAnimator = SimpleAnimator(
            onUpdate = ::updateView,
            onShowStart = {
                fullScreenBinding.root.isVisible = true
                fullScreenBinding.fullVideoView.isVisible = true
                userCallOut?.run {
                    rtcVideoController.enterFullScreen(rtcUID)
                    rtcVideoController.updateFullScreenVideo(fullScreenBinding.fullVideoView, rtcUID)

                    fullScreenBinding.fullVideoDisableLayout.isVisible = !videoOpen
                    fullScreenBinding.fullScreenAvatar.load(avatarURL) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            },
            onShowEnd = {
                fullScreenBinding.fullVideoOptArea.isVisible = true
            },
            onHideStart = {
                fullScreenBinding.fullVideoOptArea.isVisible = false
            },
            onHideEnd = {
                fullScreenBinding.root.isVisible = false
                fullScreenBinding.fullVideoView.isVisible = false
                fullScreenBinding.fullVideoDisableLayout.isVisible = false
                rtcVideoController.exitFullScreen()
                userCallOut?.run {
                    adapter.updateVideoView(rtcUID)
                }
                clearCallOutAndNotify()
            }
        )
        initUserWindowsLayout()
    }

    private fun getViewRect(view: View, anchorView: View): Rect {
        val array = IntArray(2)
        view.getLocationOnScreen(array)

        val arrayP = IntArray(2)
        anchorView.getLocationOnScreen(arrayP)

        return Rect(
            array[0] - arrayP[0],
            array[1] - arrayP[1],
            array[0] - arrayP[0] + view.width,
            array[1] - arrayP[1] + view.height
        )
    }

    private fun hideVideoListOptArea() {
        videoListBinding.videoListOptArea.isVisible = false
    }

    // show operation float view
    private fun showVideoListOptArea(videoArea: Rect) {
        videoListBinding.videoListOptArea.run {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.topMargin = videoArea.bottom.coerceAtMost(maxTopMargin())
            layoutParams = lp
            isVisible = true
        }

        updateCallOutUser()
    }

    private fun maxTopMargin(): Int {
        return videoListBinding.videoList.height - activity.resources.getDimensionPixelSize(R.dimen.room_class_button_size)
    }

    // 更新显示浮窗及全屏按钮状态
    private fun updateCallOutUser() = userCallOut?.run {
        videoListBinding.videoOpt.isSelected = videoOpen
        videoListBinding.audioOpt.isSelected = audioOpen
        fullScreenBinding.fullVideoOpt.isSelected = videoOpen
        fullScreenBinding.fullAudioOpt.isSelected = audioOpen

        if (fullScreenBinding.fullVideoView.isVisible) {
            fullScreenBinding.fullVideoDisableLayout.isVisible = !videoOpen
        }
    }

    private fun clearCallOut() {
        userCallOut = null
        hideVideoListOptArea()
    }

    private fun clearCallOutAndNotify() {
        clearCallOut()
        RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_VIDEO_OP_CALL_OUT, false)
    }

    private var start = Rect()
    private var end = Rect()
    private var userCallOut: RoomUser? = null

    private fun updateView(value: Float) {
        val left = start.left + (end.left - start.left) * value
        val right = start.right + (end.right - start.right) * value
        val top = start.top + (end.top - start.top) * value
        val bottom = start.bottom + (end.bottom - start.bottom) * value
        logger.d("left:$left,right:$right,top:$top,bottom:$bottom")

        fullScreenBinding.fullVideoView.run {
            val lp = layoutParams as ViewGroup.MarginLayoutParams
            lp.width = (right - left).toInt()
            lp.height = (bottom - top).toInt()
            lp.leftMargin = left.toInt()
            lp.topMargin = top.toInt()
            layoutParams = lp
        }

        fullScreenBinding.fullVideoDisableLayout.run {
            val lp = layoutParams as ViewGroup.MarginLayoutParams
            lp.width = (right - left).toInt()
            lp.height = (bottom - top).toInt()
            lp.leftMargin = left.toInt()
            lp.topMargin = top.toInt()
            layoutParams = lp
        }

        fullScreenBinding.fullScreenAvatar.run {
            val sizeFrom = activity.resources.getDimensionPixelSize(R.dimen.room_class_video_user_avatar_size_normal)
            val sizeTo = activity.resources.getDimensionPixelSize(R.dimen.room_class_video_user_avatar_size_fullscreen)

            val lp = layoutParams as ViewGroup.MarginLayoutParams
            lp.width = (sizeFrom + sizeTo * value).toInt()
            lp.height = (sizeFrom + sizeTo * value).toInt()
            layoutParams = lp
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        rtcApi.leaveChannel()
    }

    private fun checkPermission(actionAfterPermission: () -> Unit) {
        val permissions = REQUESTED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissions.isEmpty()) {
            actionAfterPermission()
            return
        }

        logger.d("[RTC] checkPermission request $permissions")
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            val allGranted = it.mapNotNull { it.key }.size == it.size
            if (allGranted) {
                actionAfterPermission()
            } else {
                activity.showToast("Permission Not Granted")
            }
        }.launch(permissions)
    }


    private lateinit var userWindowsBinding: ComponentUserWindowsBinding
    private lateinit var userWindowsContainer: FrameLayout

    private var videoRect = Rect()
    private var boardRect = Rect()
    private var windowLayoutMap = mutableMapOf<String, UserWindowLayout>()
    private val atomIndex = AtomicInteger(0)

    private val onDragListener = View.OnDragListener { _, event ->
        logger.i("RtcComponent", "onDragListener ${event.action}")
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                val user = adapter.findUserByUuid(windowsDragManager.currentUUID()) ?: return@OnDragListener false
                addNewUserWindow(
                    user, UserWindowUiState(
                        centerX = event.x,
                        centerY = event.y,
                        width = boardRect.width() * Config.defaultWindowScale,
                        height = boardRect.width() * Config.defaultWindowScale * Config.defaultBoardRatio,
                        index = atomIndex.getAndIncrement(),
                    )
                )
            }
            DragEvent.ACTION_DRAG_ENTERED -> {}
            DragEvent.ACTION_DRAG_LOCATION -> {
                updateCenter(windowsDragManager.currentUUID(), event.x, event.y)
                showEnterBoardArea()
            }
            DragEvent.ACTION_DROP -> {
                updateCenter(windowsDragManager.currentUUID(), event.x, event.y)
                clearDragRectShow()
                handleDragOnBoardEnd()
            }
        }
        true
    }

    private fun addNewUserWindow(user: RoomUser, windowUiState: UserWindowUiState) {
        windowsDragManager.setWindowState(user.userUUID, windowUiState)
        val windowContainer = UserWindowLayout(activity).apply {
            setRoomUser(user)
            if (viewModel.canDragUser()) {
                setOnWindowDragListener(object : UserWindowLayout.OnWindowDragListener {
                    override fun onActionStart(uuid: String) {
                        windowsDragManager.startMove(uuid)
                        this@apply.bringToFront()
                    }

                    override fun onWindowScale(uuid: String, scale: Float) {
                        val windowState = windowsDragManager.getWindowState(uuid) ?: return
                        val scale = scale.coerceIn(
                            boardRect.width() * Config.defaultMinWindowScale / windowState.width,
                            boardRect.width() / windowState.width
                        )
                        windowsDragManager.scaleWindow(uuid, scale)
                        windowLayoutMap[uuid]?.renderTo(windowsDragManager.getWindowRect(uuid))
                    }

                    override fun onWindowScaleEnd(uuid: String) {
                        handleWindowDragEnd(uuid)
                    }

                    override fun onWindowMove(uuid: String, dx: Float, dy: Float) {
                        val windowInfo = windowsDragManager.getWindowState(uuid) ?: return
                        updateCenter(uuid, windowInfo.centerX + dx, windowInfo.centerY + dy)
                        showEnterVideoArea()
                    }

                    override fun onWindowMoveEnd(uuid: String) {
                        clearDragRectShow()
                        handleWindowDragEnd(uuid)
                    }

                    override fun onDoubleTap(userId: String): Boolean {
                        if (fullOnStage) {
                            syncedState.normalizeWindows()
                        } else {
                            syncedState.maximizeWindows(userId)
                        }
                        return true
                    }
                })
            }
            setUserWindowListener(object : UserWindowLayout.OnUserWindowListener {
                override fun onUserWindowClick(userWindowLayout: UserWindowLayout) {
                    if (viewModel.canControlDevice(userWindowLayout.getUserUUID())) {
                        userWindowLayout.showDeviceControl()
                    }
                }

                override fun onSwitchCamera(user: RoomUser, on: Boolean) {
                    viewModel.enableVideo(on, user.userUUID)
                }

                override fun onSwitchMic(user: RoomUser, on: Boolean) {
                    viewModel.enableAudio(on, user.userUUID)
                }
            })
        }
        windowLayoutMap[user.userUUID] = windowContainer
        userWindowsContainer.addView(windowContainer)

        windowContainer.renderTo(windowUiState.getRect())
        rtcVideoController.setupUserVideo(windowContainer.getContainer(), user.rtcUID)
    }

    private fun removeNewUserWindow(uuid: String) {
        windowsDragManager.removeWindowState(uuid)
        val remove = windowLayoutMap.remove(uuid)
        userWindowsContainer.removeView(remove)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUserWindowsLayout() {
        userWindowsBinding = ComponentUserWindowsBinding.inflate(activity.layoutInflater, userWindowsLayout, true)
        userWindowsBinding.root.setOnDragListener(onDragListener)
        userWindowsContainer = userWindowsBinding.userWindowsContainer
        userWindowsContainer.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
                return@addOnLayoutChangeListener
            }
            updateWindowUsersRect(expandWidth)
        }
    }

    private fun updateWindowUsersRect(videoWidth: Int) {
        val vw = userWindowsContainer.width - videoWidth
        val vh = userWindowsContainer.height
        if (vw <= 0 || vh <= 0) {
            return
        }

        videoRect = Rect(
            vw,
            0,
            vw + videoWidth,
            vh,
        )

        val bw = min(vw, vh * 16 / 9)
        val bh = min(vh, vw * 9 / 16)
        boardRect = Rect(
            (vw - bw) / 2,
            (vh - bh) / 2,
            (vw + bw) / 2,
            (vh + bh) / 2,
        )

        windowsDragManager.setWindowMap(getWindowsUiState(windowsState))
        refreshUserWindows()
        userWindowsBinding.maskView.renderTo(Rect(0, 0, vw, vh))
    }

    private fun refreshUserWindows() {
        userWindowsContainer.run {
            val windowsMap = windowsDragManager.getWindowsMap()
            windowsMap.entries.sortedBy { it.value.index }.forEach { (uuid, windowUiState) ->
                windowLayoutMap[uuid]?.renderTo(windowUiState.getRect())
                windowLayoutMap[uuid]?.bringToFront()
            }
        }
    }

    private fun updateUserWindows(users: List<RoomUser>) {
        userWindowsContainer.run {
            users.forEach {
                windowLayoutMap[it.userUUID]?.setRoomUser(it)
            }
        }
    }

    private fun handleDragOnBoardEnd(): Boolean {
        val uuid = windowsDragManager.currentUUID()
        val rect = windowsDragManager.getWindowRect(uuid)
        if (boardRect.contains(rect.centerX(), rect.centerY())) {
            if (fullOnStage) {
                syncedState.maximizeWindows(uuid)
            } else {
                animateOnBoard(uuid)
            }
        } else {
            animateOnBoardExit(uuid)
        }
        return true
    }

    private fun animateOnBoard(uuid: String) {
        val from = windowsDragManager.getWindowRect(uuid)
        val to = from.constrainRect(boardRect)

        val r = Rect()
        val animator = SimpleAnimator(
            onUpdate = { value ->
                r.lerp(from, to, value)
                windowLayoutMap[uuid]?.renderTo(r)
            },
            onShowEnd = {
                updateCenter(uuid, to.centerX().toFloat(), to.centerY().toFloat())
                adapter.updateItemByUuid(uuid)
                syncedState.updateNormalWindow(uuid, to.toWindowInfo())
            },
        )
        animator.show()
    }

    private fun animateOnBoardExit(uuid: String) {
        val from = windowsDragManager.getWindowRect(uuid)
        val to = Rect(videoRect.left, videoRect.top, videoRect.left, videoRect.top)
        adapter.findContainerByUuid(uuid)?.let {
            to.set(getViewRect(it, userWindowsContainer))
        }

        val r = Rect()
        val animator = SimpleAnimator(
            onUpdate = { value ->
                r.lerp(from, to, value)
                windowLayoutMap[uuid]?.renderTo(r)
            },
            onShowEnd = {
                removeNewUserWindow(uuid)
                adapter.updateItemByUuid(uuid)
            }
        )
        animator.show()
    }

    private fun handleWindowDragEnd(uuid: String) {
        val rect = windowsDragManager.getWindowRect(uuid)
        if (videoRect.contains(rect.centerX(), rect.centerY())) {
            animateEnterVideoArea(uuid)
        } else if (rect != rect.constrainRect(boardRect)) {
            if (fullOnStage) {
                animateResetMaximize(uuid)
            } else {
                animateOnBoard(uuid)
            }
        } else {
            if (fullOnStage) {
                animateResetMaximize(uuid)
            } else {
                syncedState.updateNormalWindow(uuid, rect.toWindowInfo())
            }
        }
    }

    private fun animateResetMaximize(uuid: String) {
        val from = windowsDragManager.getWindowRect(uuid)
        val to = windowsState.grid.indexOf(uuid).takeIf { it >= 0 }?.let {
            WindowsDragManager.getMaximizeWindowsInfo(windowsState.grid.size)[it].toUserWindowUiState().getRect()
        } ?: return

        val r = Rect()
        val animator = SimpleAnimator(
            onUpdate = { value ->
                r.lerp(from, to, value)
                windowLayoutMap[uuid]?.renderTo(r)
            },
            onShowEnd = {
                windowLayoutMap[uuid]?.renderTo(to)
                updateCenter(uuid, to.centerX().toFloat(), to.centerY().toFloat())
            }
        )
        animator.show()
    }

    private fun animateEnterVideoArea(uuid: String) {
        val from = windowsDragManager.getWindowRect(uuid)
        val to = Rect(videoRect.left, videoRect.top, videoRect.left, videoRect.top)
        adapter.findContainerByUuid(uuid)?.let {
            to.set(getViewRect(it, userWindowsContainer))
        }

        val r = Rect()
        val animator = SimpleAnimator(
            onUpdate = { value ->
                r.lerp(from, to, value)
                windowLayoutMap[uuid]?.renderTo(r)
            },
            onShowEnd = {
                removeNewUserWindow(uuid)
                adapter.updateItemByUuid(uuid)
                if (fullOnStage) {
                    syncedState.removeMaximizeWindow(uuid)
                } else {
                    syncedState.removeNormalWindow(uuid)
                }
            }
        )
        animator.show()
    }

    private fun Rect.toWindowInfo(): WindowInfo {
        return WindowInfo(
            x = (left - boardRect.left).toFloat() / boardRect.width(),
            y = (top - boardRect.top).toFloat() / boardRect.height(),
            width = width().toFloat() / boardRect.width(),
            height = height().toFloat() / boardRect.height(),
            z = atomIndex.getAndIncrement(),
        )
    }

    private fun WindowInfo.toUserWindowUiState(): UserWindowUiState {
        return UserWindowUiState(
            centerX = (x + width / 2) * boardRect.width() + boardRect.left,
            centerY = (y + height / 2) * boardRect.height() + boardRect.top,
            width = width * boardRect.width(),
            height = height * boardRect.height(),
            index = z,
        )
    }

    private fun showEnterVideoArea() {
        userWindowsBinding.dragRectShow.renderTo(videoRect)
        userWindowsBinding.dragRectShow.isVisible = run {
            val rect = windowsDragManager.getWindowRect()
            videoRect.contains(rect.centerX(), rect.centerY())
        }
    }

    private fun showEnterBoardArea() {
        userWindowsBinding.dragRectShow.renderTo(boardRect)
        userWindowsBinding.dragRectShow.isVisible = run {
            val rect = windowsDragManager.getWindowRect()
            boardRect.contains(rect.centerX(), rect.centerY())
        }
    }

    private fun clearDragRectShow() {
        userWindowsBinding.dragRectShow.isVisible = false
    }

    private fun updateCenter(uuid: String, x: Float, y: Float) {
        windowsDragManager.updateWindowCenter(uuid, x, y)
        windowLayoutMap[uuid]?.renderTo(windowsDragManager.getWindowRect(uuid))
    }

    /**
     * Lerp this rect from start to end by value.
     *
     * @param start The start rect.
     * @param end The end rect.
     * @param value The value to lerp by. Must be between 0 and 1.
     */
    private fun Rect.lerp(start: Rect, end: Rect, value: Float) {
        this.set(
            /* left = */ (start.left + (end.left - start.left) * value).toInt(),
            /* top = */ (start.top + (end.top - start.top) * value).toInt(),
            /* right = */ (start.right + (end.right - start.right) * value).toInt(),
            /* bottom = */ (start.bottom + (end.bottom - start.bottom) * value).toInt()
        )
    }

    /**
     * Constrain this rect to be inside the boundary rect.
     * @param boundary The boundary rect.
     * @return The constrained rect.
     */
    private fun Rect.constrainRect(boundary: Rect): Rect {
        return Rect(this).apply {
            val dTop = boundary.top - top
            val dBottom = boundary.bottom - bottom
            val dLeft = boundary.left - left
            val dRight = boundary.right - right

            if (dTop > 0) {
                offset(0, dTop)
            } else if (dBottom < 0) {
                offset(0, dBottom)
            }
            if (dLeft > 0) {
                offset(dLeft, 0)
            } else if (dRight < 0) {
                offset(dRight, 0)
            }
        }
    }

    private fun intersectSize(rect1: Rect, rect2: Rect): Int {
        val intersect = Rect()
        return if (intersect.setIntersect(rect1, rect2)) {
            intersect.width() * intersect.height()
        } else {
            0
        }
    }

    private fun handleReward(userUUID: String) {
        val rewardAnimationView = LottieAnimationView(activity).apply {
            imageAssetsFolder = "lottie/images"
            setAnimation("lottie/reward.json")
        }
        rewardAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val targetContainer = adapter.findContainerByUuid(userUUID)
                val end = if (windowsDragManager.isOnBoard(userUUID)) {
                    windowsDragManager.getWindowRect(userUUID)
                } else {
                    targetContainer?.let { getViewRect(it, userWindowsBinding.root) } ?: Rect()
                }
                val start = getViewRect(rewardAnimationView, userWindowsBinding.root)

                val r = Rect()
                ValueAnimator.ofFloat(0f, 2f).apply {
                    duration = 2000
                    addUpdateListener {
                        val time = it.animatedValue as Float
                        if (time < 0.5f) {
                            r.lerp(start, end, time / 0.5f)
                            rewardAnimationView.renderTo(r)
                        } else {
                            rewardAnimationView.renderTo(end)
                        }
                        if (time > 1.5f && time < 2f) {
                            rewardAnimationView.alpha = 1 - (time - 1.5f) / 0.5f
                        }
                    }
                    addListener(
                        onEnd = {
                            userWindowsBinding.root.removeView(rewardAnimationView)
                        }
                    )
                    start()
                }
            }
        })
        rewardAnimationView.playAnimation()
        userWindowsBinding.root.addView(rewardAnimationView)

        soundPool.play(rewardSoundId, 1f, 1f, 0, 0, 1f)
    }
}
