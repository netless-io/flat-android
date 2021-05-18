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
import io.agora.flat.common.EventHandler
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
    private val rootFullVideo: FrameLayout,
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
    private lateinit var database: AppDatabase
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
        database = entryPoint.database()

        initView()
        initListener()
        checkPermission(::actionAfterPermission)
    }

    private fun loadData() {
        lifecycleScope.launch {
            viewModel.usersMap.collect { it ->
                Log.d(TAG, "currentUsersMap $it")
                adapter.setDataSet(ArrayList(it.values))
                // TODO Update When CallOut Rtc Changed
            }
        }

        lifecycleScope.launch {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.RtmChannelJoined -> joinRtcChannel()
                    is ClassRoomEvent.ChangeVideoDisplay -> videoAreaAnimator.switch()
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
            fullScreenBinding.fullAudioOpt -> {
                viewModel.enableVideo(!fullScreenBinding.fullAudioOpt.isSelected)
            }
            fullScreenBinding.fullVideoOpt -> {
                viewModel.enableVideo(!fullScreenBinding.fullVideoOpt.isSelected)
            }
            fullScreenBinding.exitFullScreen -> fullScreenAnimator.hide()

            videoListBinding.videoOpt -> {
                viewModel.enableVideo(!videoListBinding.videoOpt.isSelected)
            }
            videoListBinding.audioOpt -> {
                viewModel.enableAudio(!videoListBinding.audioOpt.isSelected)
            }
            videoListBinding.enterFullScreen -> {
                hideVideoListOptArea()
                fullScreenAnimator.show()
            }
        }
    }

    private fun initView() {
        fullScreenBinding = ComponentFullscreenBinding.inflate(activity.layoutInflater, rootFullVideo, true)
        fullScreenBinding.exitFullScreen.setOnClickListener(onClickListener)
        fullScreenBinding.fullAudioOpt.setOnClickListener(onClickListener)
        fullScreenBinding.fullVideoOpt.setOnClickListener(onClickListener)

        videoListBinding = ComponentVideoListBinding.inflate(activity.layoutInflater, rootView, true)
        videoListBinding.videoOpt.setOnClickListener(onClickListener)
        videoListBinding.audioOpt.setOnClickListener(onClickListener)
        videoListBinding.enterFullScreen.setOnClickListener(onClickListener)

        adapter = UserVideoAdapter(ArrayList(), rtcVideoController)
        adapter.listener = UserVideoAdapter.Listener { _, view, rtcUser ->
            user = rtcUser
            start.set(getViewRect(view, rootFullVideo))
            end.set(0, 0, rootFullVideo.width, rootFullVideo.height)

            if (videoListBinding.videoListOptArea.isVisible) {
                hideVideoListOptArea()
            } else {
                showVideoListOptArea(start)
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
                fullScreenBinding.fullVideoView.isVisible = true
                rtcVideoController.setupUserVideo(fullScreenBinding.fullVideoView, user.rtcUID, true)
            },
            onShowEnd = {
                fullScreenBinding.fullVideoOptArea.isVisible = true
            },
            onHideStart = {
                fullScreenBinding.fullVideoOptArea.isVisible = false
            },
            onHideEnd = {
                fullScreenBinding.fullVideoView.isVisible = false
                rtcVideoController.exitFullScreen()
                adapter.updateVideoView(user.rtcUID)
            }
        )
    }

    private fun getViewRect(view: ViewGroup, anchorView: View): Rect {
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
        val layoutParams = videoListBinding.videoListOptArea.layoutParams as FrameLayout.LayoutParams
        layoutParams.topMargin = videoArea.bottom
        videoListBinding.videoListOptArea.layoutParams = layoutParams

        videoListBinding.videoListOptArea.isVisible = true
        // TODO Need Update When Remote Change
        videoListBinding.videoOpt.isSelected = user.videoOpen
        videoListBinding.audioOpt.isSelected = user.audioOpen
        fullScreenBinding.fullVideoOpt.isSelected = user.videoOpen
        fullScreenBinding.fullAudioOpt.isSelected = user.audioOpen
    }

    private fun hideVideoListOptArea() {
        videoListBinding.videoListOptArea.isVisible = false
    }


    private var start = Rect()
    private var end = Rect()
    private lateinit var user: RtcUser

    private fun updateView(value: Float) {
        val left = start.left + (end.left - start.left) * value
        val right = start.right + (end.right - start.right) * value
        val top = start.top + (end.top - start.top) * value
        val bottom = start.bottom + (end.bottom - start.bottom) * value

        Log.d(TAG, "left:$left,right:$right,top:$top,bottom:$bottom")

        val layoutParams = fullScreenBinding.fullVideoView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = (right - left).toInt()
        layoutParams.height = (bottom - top).toInt()
        layoutParams.leftMargin = left.toInt()
        layoutParams.topMargin = top.toInt()
        fullScreenBinding.fullVideoView.layoutParams = layoutParams
    }

    override fun onDestroy(owner: LifecycleOwner) {
        rtcApi.rtcEngine().leaveChannel()
        rtcApi.removeEventHandler(eventHandler)
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
        rtcApi.registerEventHandler(eventHandler)
    }

    private var eventHandler = object : EventHandler {
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
