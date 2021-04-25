package io.agora.flat.ui.activity.play

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.common.EventHandler
import io.agora.flat.data.model.RtcUser
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.showToast
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.video.VideoCanvas
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

    private val viewModel: ClassRoomViewModel by activity.viewModels()

    private lateinit var recyclerView: RecyclerView
    private var adpater: UserVideoAdapter = UserVideoAdapter(ArrayList(), application().rtcEngine())

    override fun onCreate(owner: LifecycleOwner) {
        initView()
        initListener()
        checkPermission(::actionAfterPermission)
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
                    else -> {
                    }
                }
            }
        }
    }

    private fun joinRtcChannel() {
        Log.d(TAG, "call rtc joinChannel")
        viewModel.roomPlayInfo.value?.apply {
            application().rtcEngine().joinChannel(
                rtcToken,
                roomUUID,
                "{}",
                rtcUID
            )
            adpater.setLocalUid(rtcUID)
        }
    }

    private fun initView() {
        fullVideoView = rootFullVideo.findViewById(R.id.fullVideoView) as FrameLayout
        fullVideoView.setOnClickListener {
            exitFullScreen()
        }

        recyclerView = RecyclerView(activity)
        rootView.addView(recyclerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adpater

        adpater.listener = object : UserVideoAdapter.Listener {
            override fun onFullScreen(position: Int, startView: ViewGroup, rtcUser: RtcUser) {
                this@RtcComponent.rtcUser = rtcUser

                startRect.set(getViewRect(startView, rootFullVideo))
                endRect.set(0, 0, rootFullVideo.width, rootFullVideo.height)

                enterFullScreen()
            }
        }

        // TODO mute for dev
        application().rtcEngine().muteLocalAudioStream(true)
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
    private var startRect = Rect();
    private var endRect = Rect();
    lateinit var rtcUser: RtcUser

    private fun exitFullScreen() {
        val animator = ValueAnimator.ofFloat(1F, 0F)
        animator.duration = 300
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            updateView(value)
        }
        animator.addListener(onEnd = {
            updateView(0f)
            application().rtcEngine().setupLocalVideo(VideoCanvas(null, 0, rtcUser.rtcUID))
            fullVideoView.removeAllViews()
            fullVideoView.visibility = View.GONE
            adpater.setFullScreenUid(0)
        })
        animator.start()
    }

    private fun enterFullScreen() {
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.duration = 300
        animator.addUpdateListener {
            updateView(it.animatedValue as Float)
        }
        animator.addListener(
            onEnd = {
                updateView(1f)
            }, onStart = {
                fullVideoView.visibility = View.VISIBLE
                adpater.setupUserVideo(fullVideoView, rtcUser.rtcUID)
            })
        animator.start()
    }

    private fun updateView(value: Float) {
        val left = startRect.left + (endRect.left - startRect.left) * value
        val right = startRect.right + (endRect.right - startRect.right) * value
        val top = startRect.top + (endRect.top - startRect.top) * value
        val bottom = startRect.bottom + (endRect.bottom - startRect.bottom) * value

        Log.d(TAG, "left:$left,right:$right,top:$top,bottom:$bottom")

        val layoutParams = fullVideoView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = (right - left).toInt()
        layoutParams.height = (bottom - top).toInt()
        layoutParams.leftMargin = left.toInt()
        layoutParams.topMargin = top.toInt()
        fullVideoView.layoutParams = layoutParams
    }

    override fun onDestroy(owner: LifecycleOwner) {
        application().rtcEngine().leaveChannel()
        application().removeEventHandler(eventHandler)
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
        application().registerEventHandler(eventHandler)
    }

    private var eventHandler = object : EventHandler {
        override fun onFirstRemoteVideoDecoded(
            uid: Int,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
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
            application().rtcEngine().setupRemoteVideo(VideoCanvas(null, 0, uid))
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "onUserJoined:$uid $elapsed")
            // adpater.onUserJoined(uid)
        }
    }
}
