package io.agora.flat.ui.activity.play

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.R
import io.agora.flat.common.rtc.RTCEventListener
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.databinding.ComponentFullscreenBinding
import io.agora.flat.databinding.ComponentVideoListBinding
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.view.PaddingItemDecoration
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.ui.viewmodel.RtcVideoController
import io.agora.flat.util.dp2px
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RtcComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
    private val fullScreenLayout: FrameLayout,
    private val shareScreenContainer: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtcComponent::class.simpleName

        val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface RtcComponentEntryPoint {
        fun userRepository(): UserRepository
        fun rtcApi(): RtcApi
        fun rtcVideoController(): RtcVideoController
    }

    private lateinit var fullScreenBinding: ComponentFullscreenBinding
    private lateinit var videoListBinding: ComponentVideoListBinding

    private lateinit var userRepository: UserRepository
    private lateinit var rtcApi: RtcApi
    private lateinit var rtcVideoController: RtcVideoController
    private val viewModel: ClassRoomViewModel by activity.viewModels()

    private lateinit var adapter: UserVideoAdapter

    private lateinit var videoAreaAnimator: SimpleAnimator
    private lateinit var fullScreenAnimator: SimpleAnimator

    override fun onCreate(owner: LifecycleOwner) {
        injectApi()
        initView()
        initListener()
        checkPermission(::observeState)
    }

    private fun injectApi() {
        val entryPoint = EntryPointAccessors.fromActivity(activity, RtcComponentEntryPoint::class.java)
        userRepository = entryPoint.userRepository()
        rtcApi = entryPoint.rtcApi()
        rtcVideoController = entryPoint.rtcVideoController()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.videoUsers.collect { users ->
                Log.d(TAG, "videoUsers changed to $users")
                adapter.setDataSet(users)
                // 处理用户进出时的显示
                if (userCallOut != null) {
                    val findUser = users.find { it.userUUID == userCallOut!!.userUUID }
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

        lifecycleScope.launch {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.RtmChannelJoined -> joinRtcChannel()
                    else -> {; }
                }
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

        lifecycleScope.launch {
            viewModel.videoAreaShown.collect { shown ->
                if (shown) {
                    videoAreaAnimator.show()
                } else {
                    videoAreaAnimator.hide()
                }
            }
        }
    }

    private val expandWidth = activity.resources.getDimensionPixelSize(R.dimen.room_class_video_area_width)

    private fun updateVideoContainer(value: Float) {
        val layoutParams = videoListBinding.root.layoutParams
        layoutParams.width = (value * expandWidth).toInt()
        videoListBinding.root.layoutParams = layoutParams
    }

    private fun joinRtcChannel() {
        Log.d(TAG, "call rtc joinChannel")
        viewModel.roomPlayInfo.value?.apply {
            rtcApi.joinChannel(rtcToken, roomUUID, rtcUID)

            rtcVideoController.localUid = rtcUID
            rtcVideoController.shareScreenUid = rtcShareScreen.uid
            rtcVideoController.shareScreenContainer = shareScreenContainer
        }
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

        adapter = UserVideoAdapter(ArrayList(), rtcVideoController)
        adapter.listener = object : UserVideoAdapter.Listener {
            override fun onItemClick(position: Int, view: ViewGroup, rtcUser: RtcUser) {
                start.set(getViewRect(view, fullScreenLayout))
                end.set(0, 0, fullScreenLayout.width, fullScreenLayout.height)

                if (userCallOut == null || userCallOut != rtcUser) {
                    userCallOut = rtcUser
                    showVideoListOptArea(start)
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_VIDEO_OP_CALL_OUT)
                } else {
                    clearCallOutAndNotify()
                }
            }

            override fun onCloseSpeak(position: Int, itemData: RtcUser) {
                viewModel.closeSpeak(itemData.userUUID)
            }
        }

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

                    fullScreenBinding.fullVideoDisableLayout.isVisible = !videoOpen
                    rtcVideoController.updateFullScreenVideo(fullScreenBinding.fullVideoView, rtcUID)
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
                // userCallOut = null
                clearCallOutAndNotify()
            }
        )
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
    private var userCallOut: RtcUser? = null

    private fun updateView(value: Float) {
        val left = start.left + (end.left - start.left) * value
        val right = start.right + (end.right - start.right) * value
        val top = start.top + (end.top - start.top) * value
        val bottom = start.bottom + (end.bottom - start.bottom) * value
        Log.d(TAG, "left:$left,right:$right,top:$top,bottom:$bottom")

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
        rtcApi.removeEventListener(eventListener)
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

        Log.i(TAG, "checkPermission request $permissions")
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            val allGranted = it.mapNotNull { it.key }.size == it.size
            if (allGranted) {
                actionAfterPermission()
            } else {
                activity.showToast("Permission Not Granted")
            }
        }.launch(permissions)
    }

    private fun initListener() {
        rtcApi.addEventListener(eventListener)
    }

    private var eventListener = object : RTCEventListener {
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "onUserOffline uid: $uid reason: $reason")
            lifecycleScope.launch {
                rtcVideoController.handleOffline(uid)
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "onUserJoined uid: $uid elapsed: $elapsed")
            lifecycleScope.launch {
                rtcVideoController.handlerJoined(uid)
            }
        }
    }
}
