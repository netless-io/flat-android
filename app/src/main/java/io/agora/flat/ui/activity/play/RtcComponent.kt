package io.agora.flat.ui.activity.play

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.Constants
import io.agora.flat.MainApplication
import io.agora.flat.common.EventHandler
import io.agora.flat.data.MockData
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtm.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RtcComponent(
    val activity: ClassRoomActivity,
    val rootView: FrameLayout,
) : LifecycleOwner {
    companion object {
        val TAG = RtcComponent::class.simpleName
    }

    private val viewModel: ClassRoomViewModel

    private val PERMISSION_REQ_ID = 22

    // App 运行时确认麦克风和摄像头设备的使用权限。
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var recyclerView: RecyclerView
    private var adpater: UserVideoAdapter = UserVideoAdapter(ArrayList(), application().rtcEngine())

    init {
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
            checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
            checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)
        ) {
            initEngineAndJoinChannel();
        }
        viewModel = ViewModelProvider(activity).get(ClassRoomViewModel::class.java)
        lifecycleScope.launch {
            viewModel.roomUsersMap.collect {
                it.map { entry ->
                    Log.d(TAG, "${entry.key} ${entry.value.name}")
                }
                adpater.setDataSet(ArrayList(it.values))
            }
        }
        recyclerView = RecyclerView(activity)
        rootView.addView(recyclerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adpater
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        Log.i(TAG, "checkSelfPermission $permission $requestCode")
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                requestCode
            )
            return false
        }
        return true
    }

    private fun initEngineAndJoinChannel() {
        application().registerEventHandler(object : EventHandler {
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
                adpater.notifyDataSetChanged()
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.d(TAG, "onUserOffline:$uid $reason")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.d(TAG, "onUserJoined:$uid $elapsed")
            }
        })
    }

    private var rtmChannelListener = object : RtmChannelListener {
        override fun onMemberCountUpdated(p0: Int) {
            Log.d(TAG, "onMemberCountUpdated")
        }

        override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {
            Log.d(TAG, "onAttributesUpdated")
        }

        override fun onMessageReceived(
            p0: RtmMessage?,
            p1: RtmChannelMember?
        ) {
            Log.d(TAG, "onMessageReceived")
        }

        override fun onImageMessageReceived(
            p0: RtmImageMessage?,
            p1: RtmChannelMember?
        ) {
            Log.d(TAG, "onImageMessageReceived")
        }

        override fun onFileMessageReceived(
            p0: RtmFileMessage?,
            p1: RtmChannelMember?
        ) {
            Log.d(TAG, "onFileMessageReceived")
        }

        override fun onMemberJoined(p0: RtmChannelMember?) {
            Log.d(TAG, "onMemberJoined")
        }

        override fun onMemberLeft(p0: RtmChannelMember?) {
            Log.d(TAG, "onMemberLeft")
        }
    }
    private var channel: RtmChannel? = null

    fun enterChannel(rtcUID: Long, rtcToken: String, rtmToken: String, uuid: String) {
        lifecycleScope.launch {
            if (login(rtmToken, MockData.USER_UUID)) {
                channel = joinChannel(uuid)
                if (channel != null) {
                    val members = getMembers()
                    members?.map { it.userId }
                    viewModel.requestRoomUsers(uuid, members?.map { it.userId } ?: emptyList())
                }

                application().rtcEngine()?.joinChannel(
                    rtcToken, uuid,
                    "{}}",
                    rtcUID.toInt()
                )
            }
        }
    }

    private suspend fun login(rtmToken: String, userUUID: String): Boolean =
        suspendCoroutine { cont ->
            application().rtmClient()?.login(
                rtmToken,
                MockData.USER_UUID,
                object : ResultCallback<Void> {
                    override fun onSuccess(p0: Void?) {
                        cont.resumeWith(Result.success(true))
                    }

                    override fun onFailure(e: ErrorInfo?) {
                        Log.d(TAG, "onFailure $e")
                        cont.resumeWith(Result.success(false))
                    }
                })
        }

    private suspend fun joinChannel(channelId: String): RtmChannel? = suspendCoroutine {
        application().rtmClient()?.apply {
            val channel = createChannel(channelId, rtmChannelListener)
            channel.join(object : ResultCallback<Void> {
                override fun onSuccess(p0: Void?) {
                    Log.d(TAG, "join onSuccess")
                    it.resume(channel)
                }

                override fun onFailure(p0: ErrorInfo?) {
                    Log.d(TAG, "join onFailure")
                    it.resumeWith(Result.success(null))
                }
            })
        } ?: it.resume(null)
    }

    private suspend fun getMembers(): List<RtmChannelMember>? = suspendCoroutine { cont ->
        channel?.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(members: List<RtmChannelMember>?) {
                members?.forEach {
                    Log.d(TAG, "member ${it.channelId} ${it.userId}")
                }
                cont.resume(members)
            }

            override fun onFailure(e: ErrorInfo?) {
                Log.d(TAG, "onFailure $e")
                cont.resume(null)
            }
        })
    }

    private fun application(): MainApplication {
        return activity.application as MainApplication
    }

    fun onActivityDestroy() {
        application().rtmClient()?.logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {

            }

            override fun onFailure(p0: ErrorInfo?) {

            }
        })
        application().rtcEngine()?.leaveChannel()
    }

    override fun getLifecycle(): Lifecycle {
        return activity.lifecycle
    }
}
