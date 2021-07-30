package io.agora.flat.ui.activity.playback

import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.agora.flat.databinding.ComponentReplayToolBinding
import io.agora.flat.ui.activity.play.BaseComponent
import io.agora.flat.ui.view.ReplayExitDialog
import io.agora.flat.ui.viewmodel.ReplayViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReplayToolComponent(
    activity: ReplayActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    private lateinit var toolBinding: ComponentReplayToolBinding
    private val viewModel: ReplayViewModel by activity.viewModels()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        observeData()
    }

    private fun initView() {
        toolBinding = ComponentReplayToolBinding.inflate(activity.layoutInflater, rootView, true)

        toolBinding.message.setOnClickListener {
            toolBinding.message.isSelected = !toolBinding.message.isSelected
            toolBinding.messageLv.isVisible = toolBinding.message.isSelected
        }

        toolBinding.exit.setOnClickListener {
            showExitDialog()
        }
        toolBinding.messageLv.setEditable(false)
    }

    private fun showExitDialog() {
        val dialog = ReplayExitDialog().apply {
            setListener(object : ReplayExitDialog.Listener {
                override fun onClose() {
                }

                override fun onLeftButtonClick() {
                }

                override fun onRightButtonClick() {
                    activity?.finish()
                }
            })
        }
        dialog.show(activity.supportFragmentManager, "ReplayExitDialog")
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.state.collect {
                toolBinding.messageLv.setMessages(it.messages)
            }
        }
    }
}
