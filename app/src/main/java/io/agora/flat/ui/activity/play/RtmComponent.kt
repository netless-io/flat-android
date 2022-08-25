package io.agora.flat.ui.activity.play

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
import io.agora.flat.common.FlatException
import io.agora.flat.databinding.ComponentMessageBinding
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.ui.view.MessageListView
import io.agora.flat.ui.viewmodel.MessageViewModel
import io.agora.flat.ui.viewmodel.MessagesUpdate
import io.agora.flat.util.KeyboardHeightProvider
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking

class RtmComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface RtmComponentEntryPoint {
        fun rtmApi(): RtmApi
    }

    private val messageViewModel: MessageViewModel by activity.viewModels()
    private var keyboardHeightProvider: KeyboardHeightProvider? = null

    private lateinit var rtmApi: RtmApi
    private lateinit var binding: ComponentMessageBinding

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        injectApi()
        initView()
        loadData()
    }

    private fun injectApi() {
        val entryPoint = EntryPointAccessors.fromActivity(activity, RtmComponentEntryPoint::class.java)
        rtmApi = entryPoint.rtmApi()
    }

    private fun initView() {
        binding = ComponentMessageBinding.inflate(activity.layoutInflater, rootView, true)
        binding.root.isVisible = false

        binding.messageLv.setListener(object : MessageListView.Listener {
            override fun onSendMessage(msg: String) {
                messageViewModel.sendChatMessage(msg)
            }

            override fun onMute(muted: Boolean) {
                messageViewModel.muteChat(muted)
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
        lifecycleScope.launchWhenResumed {
            messageViewModel.messageUiState.filterNotNull().collect {
                binding.messageLv.showBanBtn(it.isOwner)
                binding.messageLv.setBan(it.ban, it.isOwner)
                binding.messageLv.showLoading(it.loading)

                binding.root.isVisible = it.messageAreaShown
            }
        }

        lifecycleScope.launchWhenResumed {
            messageViewModel.messageUpdate.collect {
                when (it.updateOp) {
                    MessagesUpdate.APPEND -> binding.messageLv.addMessagesAtTail(it.messages)
                    MessagesUpdate.PREPEND -> binding.messageLv.addMessagesAtHead(it.messages)
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        keyboardHeightProvider?.dismiss()
        runBlocking {
            try {
                rtmApi.logout()
            } catch (e: FlatException) {

            }
        }
    }
}