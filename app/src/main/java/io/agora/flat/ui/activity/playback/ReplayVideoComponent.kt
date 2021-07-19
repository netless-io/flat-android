package io.agora.flat.ui.activity.playback

import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.agora.flat.databinding.ComponentReplayVideoBinding
import io.agora.flat.ui.activity.play.BaseComponent
import io.agora.flat.ui.viewmodel.ReplayViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReplayVideoComponent(
    activity: ReplayActivity,
    rootView: FrameLayout
) : BaseComponent(activity, rootView) {
    private lateinit var binding: ComponentReplayVideoBinding
    private val viewModel: ReplayViewModel by activity.viewModels()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        observeData()
    }

    private fun initView() {
        binding = ComponentReplayVideoBinding.inflate(activity.layoutInflater, rootView, true)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.state.collect {
                it.recordInfo?.run {

                }
            }
        }
    }
}
