package io.agora.flat.ui.activity.play

import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.EntryPointAccessors
import io.agora.flat.data.AppDataCenter
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.rtm.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RtmComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtmComponent::class.simpleName
    }

    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private var appDataCenter = AppDataCenter(activity.applicationContext)

    lateinit var rtmApi: RtmEngineProvider

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val entryPoint = EntryPointAccessors.fromApplication(
            activity.applicationContext,
            ComponentEntryPoint::class.java
        )
        rtmApi = entryPoint.rtmApi()

        lifecycleScope.launch {
            viewModel.roomPlayInfo.collect {
                it?.apply {
                    enterChannel(
                        channelId = roomUUID,
                        rtmToken = rtmToken
                    )
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        rtmApi.rtmEngine().logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {

            }

            override fun onFailure(p0: ErrorInfo?) {

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
            message: RtmMessage,
            member: RtmChannelMember
        ) {
            Log.d(TAG, "onMessageReceived ${message.rawMessage}")
        }

        override fun onImageMessageReceived(
            imageMessage: RtmImageMessage,
            member: RtmChannelMember
        ) {
            Log.d(TAG, "onImageMessageReceived")
        }

        override fun onFileMessageReceived(
            fileMessage: RtmFileMessage,
            member: RtmChannelMember
        ) {
            Log.d(TAG, "onFileMessageReceived")
        }

        override fun onMemberJoined(member: RtmChannelMember) {
            Log.d(TAG, "onMemberJoined ${member.userId}")
            viewModel.addRtmMember(member.userId)
        }

        override fun onMemberLeft(member: RtmChannelMember) {
            Log.d(TAG, "onMemberLeft ${member.userId}")
            viewModel.removeRtmMember(member.userId)
        }
    }
    private var channel: RtmChannel? = null

    private fun enterChannel(rtmToken: String, channelId: String) {
        lifecycleScope.launch {
            if (login(rtmToken, appDataCenter.getUserInfo()!!.uuid)) {
                channel = joinChannel(channelId)
                if (channel != null) {
                    viewModel.requestRoomUsers(getMembers()?.map { it.userId } ?: emptyList())
                    Log.d(TAG, "notify rtm joined success")
                    viewModel.onEvent(ClassRoomEvent.RtmChannelJoined)
                }
            }
        }
    }

    private suspend fun login(rtmToken: String, userUUID: String): Boolean =
        suspendCoroutine { cont ->
            rtmApi.rtmEngine().login(
                rtmToken,
                userUUID,
                object : ResultCallback<Void> {
                    override fun onSuccess(p0: Void?) {
                        cont.resumeWith(Result.success(true))
                    }

                    override fun onFailure(e: ErrorInfo?) {
                        cont.resumeWith(Result.success(false))
                    }
                })
        }

    private suspend fun joinChannel(channelId: String): RtmChannel? = suspendCoroutine {
        rtmApi.rtmEngine().apply {
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
        }
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
}
