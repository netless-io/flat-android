package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.R
import io.agora.flat.common.FlatException
import io.agora.flat.common.rtm.RTMListener
import io.agora.flat.data.model.RTMEvent
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.databinding.ComponentMessageBinding
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.ui.view.MessageListView
import io.agora.flat.ui.view.RoomExitDialog
import io.agora.flat.ui.viewmodel.*
import io.agora.flat.util.delayAndFinish
import io.agora.rtm.ErrorInfo
import io.agora.rtm.ResultCallback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class RtmComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtmComponent::class.simpleName
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface RtmComponentEntryPoint {
        fun userRepository(): UserRepository
        fun rtmApi(): RtmEngineProvider
    }

    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private val messageViewModel: MessageViewModel by activity.viewModels()

    private lateinit var userRepository: UserRepository
    private lateinit var rtmApi: RtmEngineProvider
    private lateinit var binding: ComponentMessageBinding

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        val entryPoint = EntryPointAccessors.fromActivity(activity, RtmComponentEntryPoint::class.java)
        userRepository = entryPoint.userRepository()
        rtmApi = entryPoint.rtmApi()
        rtmApi.addRtmListener(flatRTMListener)

        initView()
        loadData()
    }

    private fun initView() {
        binding = ComponentMessageBinding.inflate(activity.layoutInflater, rootView, true)

        binding.messageLv.setListener(object : MessageListView.Listener {
            override fun onSendMessage(msg: String) {
                messageViewModel.sendChatMessage(msg)
            }

            override fun onLoadMore() {
                messageViewModel.loadHistoryMessage()
            }
        })
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
            viewModel.state.filter { it != ClassRoomState.Init }.collect {
                if (it.roomStatus == RoomStatus.Stopped) {
                    showRoomExitDialog(activity.getString(R.string.exit_room_stopped_message))
                }
                binding.messageLv.setBan(it.ban)
            }
        }

        lifecycleScope.launch {
            messageViewModel.messageLoading.collect {
                binding.messageLv.showLoading(it)
            }
        }

        lifecycleScope.launch {
            messageViewModel.messageUpdate.collect {
                when (it.updateOp) {
                    MessagesUpdate.APPEND -> binding.messageLv.addMessagesAtTail(it.messages)
                    MessagesUpdate.PREPEND -> binding.messageLv.addMessagesAtHead(it.messages)
                }
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
        rtmApi.removeRtmListener(flatRTMListener)
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

        override fun onRemoteLogin() {
            showRoomExitDialog(activity.getString(R.string.exit_remote_login_message))
        }
    }

    private fun showRoomExitDialog(message: String) {
        val dialog = RoomExitDialog().apply {
            arguments = Bundle().apply {
                putString(RoomExitDialog.MESSAGE, message)
            }
        }
        dialog.setListener { activity.delayAndFinish(250) }
        dialog.show(activity.supportFragmentManager, "RoomExitDialog")
    }

    private fun enterChannel(rtmToken: String, channelId: String) {
        lifecycleScope.launch {
            try {
                rtmApi.initChannel(rtmToken, channelId, userRepository.getUserUUID())
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