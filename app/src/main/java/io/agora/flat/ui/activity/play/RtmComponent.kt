package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
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
import io.agora.flat.data.repository.MiscRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.databinding.ComponentMessageBinding
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.ui.view.MessageListView
import io.agora.flat.ui.view.RoomExitDialog
import io.agora.flat.ui.viewmodel.ClassRoomState
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.ui.viewmodel.MessageViewModel
import io.agora.flat.ui.viewmodel.MessagesUpdate
import io.agora.flat.util.KeyboardHeightProvider
import io.agora.flat.util.delayAndFinish
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        fun miscRepository(): MiscRepository
        fun rtmApi(): RtmApi
    }

    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private val messageViewModel: MessageViewModel by activity.viewModels()
    private var keyboardHeightProvider: KeyboardHeightProvider? = null

    private lateinit var userRepository: UserRepository
    private lateinit var miscRepository: MiscRepository
    private lateinit var rtmApi: RtmApi
    private lateinit var binding: ComponentMessageBinding

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        val entryPoint = EntryPointAccessors.fromActivity(activity, RtmComponentEntryPoint::class.java)
        userRepository = entryPoint.userRepository()
        miscRepository = entryPoint.miscRepository()
        rtmApi = entryPoint.rtmApi()

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

        keyboardHeightProvider = KeyboardHeightProvider(activity)
            .setHeightListener(object : KeyboardHeightProvider.HeightListener {
                private var originBottomMargin: Int? = null
                override fun onHeightChanged(height: Int) {
                    if (originBottomMargin == null && binding.messageLv.isVisible) {
                        originBottomMargin =
                            (binding.messageLv.layoutParams as ConstraintLayout.LayoutParams).bottomMargin
                    }
                    if (originBottomMargin != null) {
                        val lp = binding.messageLv.layoutParams as ConstraintLayout.LayoutParams
                        lp.bottomMargin = height + originBottomMargin!!
                        binding.messageLv.postDelayed({
                            binding.messageLv.layoutParams = lp
                        }, 100)
                    }
                }
            })

        lateStartKeyboardHeightProvider()
    }

    private fun lateStartKeyboardHeightProvider() {
        lateinit var onWindowFocusChangeListener: ViewTreeObserver.OnWindowFocusChangeListener
        onWindowFocusChangeListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                keyboardHeightProvider?.start()
                binding.root.viewTreeObserver.removeOnWindowFocusChangeListener(onWindowFocusChangeListener)
            }
        }
        binding.root.viewTreeObserver.addOnWindowFocusChangeListener(onWindowFocusChangeListener)
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
            viewModel.messageAreaShown.collect { areaShown ->
                binding.root.isVisible = areaShown
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        keyboardHeightProvider?.dismiss()
        runBlocking {
            try {
                rtmApi.logout()
                rtmApi.removeRtmListener(flatRTMListener)
            } catch (e: FlatException) {
            }
        }
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
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
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
                rtmApi.addRtmListener(flatRTMListener)
                rtmApi.initChannel(rtmToken, channelId, userRepository.getUserUUID())
                viewModel.initChannelStatus()
                viewModel.notifyRTMChannelJoined()
                Log.d(TAG, "notify rtm joined success")
            } catch (e: FlatException) {
                miscRepository.logError(e.toString())
                showRoomExitDialog(e.toString())
            }
        }
    }
}