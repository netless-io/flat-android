package io.agora.flat.ui.activity.play

import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.EntryPointAccessors
import io.agora.flat.common.FlatException
import io.agora.flat.common.RTMListener
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.databinding.ComponentMessageBinding
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.delayAndFinish
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
    private lateinit var binding: ComponentMessageBinding

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val entryPoint = EntryPointAccessors.fromApplication(
            activity.applicationContext,
            ComponentEntryPoint::class.java
        )
        rtmApi = entryPoint.rtmApi()
        kvCenter = entryPoint.kvCenter()
        rtmApi.addFlatRTMListener(flatRTMListener)

        initView()
        loadData()
    }

    private fun initView() {
        binding = ComponentMessageBinding.inflate(activity.layoutInflater, rootView, true)

        binding.messageLv.setListener {
            viewModel.sendChatMessage(it)
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            viewModel.roomPlayInfo.collect {
                it?.apply {
                    enterChannel(channelId = roomUUID, rtmToken = rtmToken)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.state.collect {
                if (it.roomStatus == RoomStatus.Stopped) {
                    activity.delayAndFinish(message = "房间结束，退出中...")
                }

                binding.messageLv.setBan(it.ban)
            }
        }

        lifecycleScope.launch {
            viewModel.messageList.collect {
                binding.messageLv.setMessages(it)
            }
        }

        lifecycleScope.launch {
            viewModel.messageAreaShown.collect {
                binding.root.isVisible = it
                if (it) {
                    viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_MESSAGE)
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

    private val flatRTMListener = object : RTMListener {
        override fun onRTMEvent(event: RTMEvent, senderId: String) {
            Log.d(TAG, "event is $event")
            viewModel.onRTMEvent(event, senderId)
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
                viewModel.notifyRTMChannelJoined()
            } catch (e: FlatException) {
                // showExistDialog()
            }
        }
    }
}