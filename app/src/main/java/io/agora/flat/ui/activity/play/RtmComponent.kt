package io.agora.flat.ui.activity.play

import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.agora.flat.data.MockData
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
    private var adpater: UserVideoAdapter = UserVideoAdapter(ArrayList(), application().rtcEngine())

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        lifecycleScope.launch {
            viewModel.roomUsersMap.collect {
                it.map { entry ->
                    Log.d(TAG, "${entry.key} ${entry.value.name}")
                }
                adpater.setDataSet(ArrayList(it.values))
            }
        }

        lifecycleScope.launch {
            viewModel.roomPlayInfo.collect {
                it?.apply {
                    enterChannel(
                        rtcUID = rtcUID,
                        rtcToken = rtcToken,
                        uuid = roomUUID,
                        rtmToken = rtmToken
                    )
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        application().rtmClient()?.logout(object : ResultCallback<Void> {
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

    private fun enterChannel(rtcUID: Long, rtcToken: String, rtmToken: String, uuid: String) {
        lifecycleScope.launch {
            if (login(rtmToken, MockData.USER_UUID)) {
                channel = joinChannel(uuid)
                if (channel != null) {
                    val members = getMembers()
                    members?.map { it.userId }
                    viewModel.requestRoomUsers(uuid, members?.map { it.userId } ?: emptyList())
                }

                application().rtcEngine()?.joinChannel(
                    rtcToken,
                    uuid,
                    "{}",
                    rtcUID.toInt()
                )
            }
        }
    }

    private suspend fun login(rtmToken: String, userUUID: String): Boolean =
        suspendCoroutine { cont ->
            application().rtmClient()?.login(
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
}
