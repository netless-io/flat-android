package io.agora.flat.ui.activity.play

import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import dagger.hilt.android.EntryPointAccessors
import io.agora.flat.common.FlatException
import io.agora.flat.common.toFlatException
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.rtm.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RtmComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtmComponent::class.simpleName
    }

    private val viewModel: ClassRoomViewModel by activity.viewModels()

    private lateinit var rtmApi: RtmEngineProvider
    private lateinit var kvCenter: AppKVCenter

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val entryPoint = EntryPointAccessors.fromApplication(
            activity.applicationContext,
            ComponentEntryPoint::class.java
        )
        rtmApi = entryPoint.rtmApi()
        kvCenter = entryPoint.kvCenter()

        lifecycleScope.launch {
            viewModel.roomPlayInfo.collect {
                it?.apply {
                    enterChannel(channelId = roomUUID, rtmToken = rtmToken)
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

    private var messageListener = object : RtmChannelListenerAdapter {
        override fun onMemberJoined(member: RtmChannelMember) {
            Log.d(TAG, "onMemberJoined ${member.userId}")
            viewModel.addRtmMember(member.userId)
        }

        override fun onMemberLeft(member: RtmChannelMember) {
            Log.d(TAG, "onMemberLeft ${member.userId}")
            viewModel.removeRtmMember(member.userId)
        }
    }

    private var commandListener = object : RtmChannelListenerAdapter {
        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            when (val event = RTMEvent.parseRTMEvent(message.text)) {

            }
        }
    }

    private lateinit var channelMessage: RtmChannel
    private lateinit var channelCommand: RtmChannel

    private fun enterChannel(rtmToken: String, channelId: String) {
        lifecycleScope.launch {
            try {
                login(rtmToken, kvCenter.getUserInfo()!!.uuid)
                channelMessage = joinChannel(channelId, messageListener)
                channelCommand = joinChannel(channelId + "commands", commandListener)

                Log.d(TAG, "notify rtm joined success")
                viewModel.requestRoomUsers(getMembers().map { it.userId })
                viewModel.onEvent(ClassRoomEvent.RtmChannelJoined)
            } catch (e: FlatException) {
                // showExistDialog()
            }
        }
    }

    private suspend fun login(rtmToken: String, userUUID: String): Boolean =
        suspendCoroutine { cont ->
            rtmApi.rtmEngine().login(rtmToken, userUUID, object : ResultCallback<Void?> {
                override fun onSuccess(v: Void?) {
                    cont.resume(true)
                }

                override fun onFailure(e: ErrorInfo) {
                    cont.resumeWithException(e.toFlatException())
                }
            })
        }

    private suspend fun joinChannel(channelId: String, listener: RtmChannelListener): RtmChannel = suspendCoroutine {
        rtmApi.rtmEngine().run {
            val channel = createChannel(channelId, listener)
            channel.join(object : ResultCallback<Void?> {
                override fun onSuccess(v: Void?) {
                    Log.d(TAG, "join onSuccess")
                    it.resume(channel)
                }

                override fun onFailure(e: ErrorInfo) {
                    Log.d(TAG, "join onFailure")
                    it.resumeWithException(e.toFlatException())
                }
            })
        }
    }

    private suspend fun getMembers(): List<RtmChannelMember> = suspendCoroutine { cont ->
        channelMessage.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(members: List<RtmChannelMember>) {
                Log.d(TAG, "member $members")
                cont.resume(members)
            }

            override fun onFailure(e: ErrorInfo) {
                Log.d(TAG, "onFailure $e")
                cont.resume(listOf())
            }
        })
    }

    private suspend fun sendMessage(msg: String): List<RtmChannelMember>? = suspendCoroutine { cont ->
        run {
            val message = rtmApi.rtmEngine().createMessage()
            message.text = msg

            channelMessage.sendMessage(message, object : ResultCallback<Void> {
                override fun onSuccess(v: Void) {

                }

                override fun onFailure(errorIn: ErrorInfo) {

                }
            })
        }
    }

    private suspend fun sendCommand(event: RTMEvent): List<RtmChannelMember>? = suspendCoroutine { cont ->
        run {
            val message = rtmApi.rtmEngine().createMessage()
            message.text = Gson().toJson(event)

            channelCommand.sendMessage(message, object : ResultCallback<Void> {
                override fun onSuccess(v: Void) {

                }

                override fun onFailure(errorIn: ErrorInfo) {

                }
            })
        }
    }

    interface RtmChannelListenerAdapter : RtmChannelListener {
        override fun onMemberCountUpdated(count: Int) {
            Log.d(TAG, "onMemberCountUpdated")
        }

        override fun onAttributesUpdated(attributes: MutableList<RtmChannelAttribute>) {
            Log.d(TAG, "onAttributesUpdated")
        }

        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            Log.d(TAG, "onMessageReceived ${message.text}")
            Log.d(TAG, "onMessageReceived ${member.userId}")
            Log.d(TAG, "onMessageReceived ${member.channelId}")
        }

        override fun onImageMessageReceived(imageMessage: RtmImageMessage, member: RtmChannelMember) {
            Log.d(TAG, "onImageMessageReceived")
        }

        override fun onFileMessageReceived(fileMessage: RtmFileMessage, member: RtmChannelMember) {
            Log.d(TAG, "onFileMessageReceived")
        }

        override fun onMemberJoined(member: RtmChannelMember) {
            Log.d(TAG, "onMemberJoined ${member.userId}")
        }

        override fun onMemberLeft(member: RtmChannelMember) {
            Log.d(TAG, "onMemberLeft ${member.userId}")
        }
    }
}