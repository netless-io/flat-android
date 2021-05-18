package io.agora.flat.ui.activity.play

import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.EntryPointAccessors
import io.agora.flat.common.FlatException
import io.agora.flat.common.FlatRTMListener
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.rtm.ErrorInfo
import io.agora.rtm.ResultCallback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

        rtmApi.addFlatRTMListener(flatRTMListener)
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
        rtmApi.removeFlatRTMListener(flatRTMListener)
        rtmApi.rtmEngine().logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {

            }

            override fun onFailure(p0: ErrorInfo?) {

            }
        })
    }

    private val flatRTMListener = object : FlatRTMListener {
        override fun onRTMEvent(event: RTMEvent, senderId: String) {
            Log.d(TAG, "event is $event")
            viewModel.onRTMEvent(event,senderId)
        }

        override fun onMemberJoined(userId: String, channelId: String) {
            viewModel.addRtmMember(userId)
        }

        override fun onMemberLeft(userId: String, channelId: String) {
            viewModel.removeRtmMember(userId)
        }
    }

    private fun currentUUID(): String {
        return kvCenter.getUserInfo()!!.uuid;
    }

    private fun enterChannel(rtmToken: String, channelId: String) {
        lifecycleScope.launch {
            try {
                rtmApi.initChannel(rtmToken, channelId, currentUUID())
                viewModel.initRoomUsers(rtmApi.getMembers().map { it.userId })
                viewModel.requestChannelStatus()
                Log.d(TAG, "notify rtm joined success")
                viewModel.onEvent(ClassRoomEvent.RtmChannelJoined)
            } catch (e: FlatException) {
                // showExistDialog()
            }
        }
    }
}