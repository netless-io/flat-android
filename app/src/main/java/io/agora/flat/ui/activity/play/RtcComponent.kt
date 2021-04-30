package io.agora.flat.ui.activity.play

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.EntryPointAccessors
import io.agora.flat.R
import io.agora.flat.common.EventHandler
import io.agora.flat.data.model.RtcUser
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
    val rootFullVideo: FrameLayout
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtcComponent::class.simpleName
    }

    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var rtcApi: RtcEngineProvider
    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private val rtcVideoController: RtcVideoController by activity.viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adpater: UserVideoAdapter

    private lateinit var videoAreaAnimator: SimpleAnimator
    private lateinit var fullScreenAnimator: SimpleAnimator

    override fun onCreate(owner: LifecycleOwner) {
        val entryPoint = EntryPointAccessors.fromApplication(
            activity.applicationContext,
            ComponentEntryPoint::class.java
        )
        rtcApi = entryPoint.rtcApi()

        initView()
        initListener()
        checkPermission(::actionAfterPermission)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }

    private fun loadData() {
        lifecycleScope.launch {
            viewModel.currentUsersMap.collect {
                Log.d(TAG, "currentUsersMap $it")
                adpater.setDataSet(ArrayList(it.values))
            }
        }

        lifecycleScope.launch {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.RtmChannelJoined -> {
                        joinRtcChannel()
                    }
                    is ClassRoomEvent.ChangeVideoDisplay -> videoAreaAnimator.switch()
                    else -> {
                    }
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
            rtcApi.rtcEngine().joinChannel(
                rtcToken,
                roomUUID,
                "{}",
                rtcUID
            )
            rtcVideoController.setLocalUid(rtcUID)
        }
    }

    private fun initView() {
        fullVideoView = rootFullVideo.findViewById(R.id.fullVideoView) as FrameLayout
        fullVideoView.setOnClickListener {
            fullScreenAnimator.hide()
        }

        adpater = UserVideoAdapter(ArrayList(), rtcVideoController)
        adpater.listener = UserVideoAdapter.Listener { _, view, rtcUser ->
            user = rtcUser
            start.set(getViewRect(view, rootFullVideo))
            end.set(0, 0, rootFullVideo.width, rootFullVideo.height)
            fullScreenAnimator.show()
        }

        recyclerView = RecyclerView(activity)
        rootView.addView(recyclerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adpater

        // TODO mute for dev
        rtcApi.rtcEngine().muteLocalAudioStream(true)

        videoAreaAnimator = SimpleAnimator(
            onUpdate = ::updateVideoContainer,
            onShowStart = { rootView.visibility = View.VISIBLE },
            onHideEnd = { rootView.visibility = View.GONE }
        )

        fullScreenAnimator = SimpleAnimator(
            onUpdate = ::updateView,
            onShowStart = {
                fullVideoView.visibility = View.VISIBLE
                rtcVideoController.setupUserVideo(fullVideoView, user.rtcUID, true)
            },
            onHideEnd = {
                fullVideoView.visibility = View.GONE
                rtcVideoController.exitFullScreen()
                adpater.updateVideoView(user.rtcUID)
            }
        )
    }

    private fun getViewRect(view: ViewGroup, anchorView: View): Rect {
        var array = IntArray(2)
        view.getLocationOnScreen(array)

        var arrayP = IntArray(2)
        anchorView.getLocationOnScreen(arrayP)

        return Rect(
            array[0] - arrayP[0],
            array[1] - arrayP[1],
            array[0] - arrayP[0] + view.width,
            array[1] - arrayP[1] + view.height
        )
    }

    lateinit var fullVideoView: FrameLayout
    private var start = Rect();
    private var end = Rect();
    lateinit var user: RtcUser

    private fun updateView(value: Float) {
        val left = start.left + (end.left - start.left) * value
        val right = start.right + (end.right - start.right) * value
        val top = start.top + (end.top - start.top) * value
        val bottom = start.bottom + (end.bottom - start.bottom) * value

        Log.d(TAG, "left:$left,right:$right,top:$top,bottom:$bottom")

        val layoutParams = fullVideoView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = (right - left).toInt()
        layoutParams.height = (bottom - top).toInt()
        layoutParams.leftMargin = left.toInt()
        layoutParams.topMargin = top.toInt()
        fullVideoView.layoutParams = layoutParams
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
            // adpater.onUserJoined(uid)
        }
    }
}
