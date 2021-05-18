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
import dagger.hilt.android.EntryPointAccessors
import io.agora.flat.common.RTCEventListener
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.model.RtcUser
import io.agora.flat.databinding.ComponentFullscreenBinding
import io.agora.flat.databinding.ComponentVideoListBinding
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.ui.viewmodel.RtcVideoController
import io.agora.flat.util.dp2px
import io.agora.flat.util.showToast
import io.agora.rtc.IRtcEngineEventHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RtcComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
    private val fullScreenLayout: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtcComponent::class.simpleName

        val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var fullScreenBinding: ComponentFullscreenBinding
    private lateinit var videoListBinding: ComponentVideoListBinding

    private lateinit var rtcApi: RtcEngineProvider
    private lateinit var appDatabase: AppDatabase
    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private val rtcVideoController: RtcVideoController by activity.viewModels()

    private lateinit var adapter: UserVideoAdapter

    private lateinit var videoAreaAnimator: SimpleAnimator
    private lateinit var fullScreenAnimator: SimpleAnimator

    override fun onCreate(owner: LifecycleOwner) {
        val entryPoint = EntryPointAccessors.fromApplication(
            activity.applicationContext,
            ComponentEntryPoint::class.java
        )
        rtcApi = entryPoint.rtcApi()
        appDatabase = entryPoint.database()

        initView()
        initListener()
        checkPermission(::actionAfterPermission)
    }

    private fun loadData() {
        lifecycleScope.launch {
            viewModel.usersMap.collect { it ->
                Log.d(TAG, "currentUsersMap $it")
                adapter.setDataSet(ArrayList(it.values))
                val findUser = userCallOut?.run {
                    it.values.find { it.userUUID == this.userUUID }
                }
                if (findUser == null) {
                    hideVideoListOptArea()
                    fullScreenAnimator.hide()
                } else {
                    userCallOut = findUser
                    updateCallOutUser()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.RtmChannelJoined -> joinRtcChannel()
                    is ClassRoomEvent.ChangeVideoDisplay -> videoAreaAnimator.switch()
                    is ClassRoomEvent.OperatingAreaShown -> handleAreaShown(it.areaId)
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

        lifecycleScope.launch {
            viewModel.roomConfig.collect {
                rtcApi.rtcEngine().muteLocalAudioStream(!it.enableAudio)
                rtcApi.rtcEngine().muteLocalVideoStream(!it.enableVideo)
            }
        }
    }

    private val expandWidth = activity.dp2px(120)
    private fun updateVideoContainer(value: Float) {
        val width = (value * expandWidth).toInt()

        val layoutParams = rootView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = width
        rootView.layoutParams = layoutParams
    }

    private fun joinRtcChannel() {
        Log.d(TAG, "call rtc joinChannel")
        viewModel.roomPlayInfo.value?.apply {
            rtcApi.rtcEngine().joinChannel(rtcToken, roomUUID, "{}", rtcUID)
            rtcVideoController.setLocalUid(rtcUID)
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
            fullScreenBinding.exitFullScreen -> fullScreenAnimator.hide()

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

        adapter = UserVideoAdapter(ArrayList(), rtcVideoController)
        adapter.listener = UserVideoAdapter.Listener { _, view, user ->
            start.set(getViewRect(view, fullScreenLayout))
            end.set(0, 0, fullScreenLayout.width, fullScreenLayout.height)

            if (userCallOut == null || userCallOut != user) {
                userCallOut = user
                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_VIDEO_OP_CALL_OUT)
                showVideoListOptArea(start)
            } else {
                userCallOut = null
                hideVideoListOptArea()
            }
        }

        videoListBinding.videoList.layoutManager = LinearLayoutManager(activity)
        videoListBinding.videoList.adapter = adapter

        videoAreaAnimator = SimpleAnimator(
            onUpdate = ::updateVideoContainer,
            onShowStart = { rootView.isVisible = true },
            onHideEnd = { rootView.isVisible = false }
        )

        fullScreenAnimator = SimpleAnimator(
            onUpdate = ::updateView,
            onShowStart = {
                fullScreenBinding.root.isVisible = true
                fullScreenBinding.fullVideoView.isVisible = true
                userCallOut?.run {
                    fullScreenBinding.fullVideoDisableLayout.isVisible = !videoOpen
                    rtcVideoController.setupUserVideo(fullScreenBinding.fullVideoView, rtcUID, true)
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

    private fun showVideoListOptArea(videoArea: Rect) {
        videoListBinding.videoListOptArea.run {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.topMargin = videoArea.bottom
            layoutParams = lp
            isVisible = true
        }

        updateCallOutUser()
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

    private fun hideVideoListOptArea() {
        videoListBinding.videoListOptArea.isVisible = false
    }

    private fun handleAreaShown(areaId: Int) {
        if (videoListBinding.videoListOptArea.isVisible) {
            videoListBinding.videoListOptArea.isVisible = (areaId == ClassRoomEvent.AREA_ID_VIDEO_OP_CALL_OUT)
        }
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
    }

    override fun onDestroy(owner: LifecycleOwner) {
        rtcApi.rtcEngine().leaveChannel()
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
        }

        Log.i(TAG, "requestPermissions $permissions")
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            val allGranted = it.mapNotNull { it.key }.size == it.size
            if (allGranted) {
                actionAfterPermission()
            } else {
                activity.showToast("Permission Not Granted")
            }
        }.launch(permissions)
    }

    private fun actionAfterPermission() {
        loadData()
    }

    private fun initListener() {
        rtcApi.addEventListener(eventListener)
    }

    private var eventListener = object : RTCEventListener {
        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            Log.d(TAG, "onFirstRemoteVideoDecoded")
        }

        override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats?) {
            Log.d(TAG, "onLeaveChannel")
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "onJoinChannelSuccess:$channel\t$uid\t$elapsed")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "onUserOffline:$uid $reason")
            lifecycleScope.launch {
                rtcVideoController.releaseVideo(uid)
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "onUserJoined:$uid $elapsed")
            // adapter.onUserJoined(uid)
        }
    }
}
