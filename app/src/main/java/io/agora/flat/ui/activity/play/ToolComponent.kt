package io.agora.flat.ui.activity.play

import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.agora.flat.databinding.ComponentToolBinding
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.dp2px
import io.agora.flat.util.showDebugToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ToolComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = ToolComponent::class.simpleName
    }

    private lateinit var binding: ComponentToolBinding
    private lateinit var toolAnimator: SimpleAnimator

    private val viewModel: ClassRoomViewModel by activity.viewModels()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        initView()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.OperationAreaShown -> handleAreaShown(it.areaId)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.roomConfig.collect {
                binding.switchVideo.isChecked = it.enableVideo
                binding.switchAudio.isChecked = it.enableAudio
            }
        }
    }

    private fun handleAreaShown(areaId: Int) {
        if (binding.settingLayout.isVisible) {
            binding.settingLayout.isVisible = (areaId == ClassRoomEvent.AREA_ID_SETTING)
        }
    }

    private fun initView() {
        binding = ComponentToolBinding.inflate(activity.layoutInflater, rootView, true)

        val map: Map<View, (View) -> Unit> = mapOf(
            binding.message to {},
            binding.cloudservice to { activity.showDebugToast("uploadFile") },
            // binding.video to { viewModel.changeVideoDisplay() },
            binding.invite to { activity.showDebugToast("show invite dialog") },
            binding.setting to {
                binding.settingLayout.apply {
                    visibility = if (visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
                viewModel.onOperationAreaShown(ClassRoomEvent.AREA_ID_SETTING)
            },
            binding.collapse to { toolAnimator.hide() },
            binding.expand to { toolAnimator.show() },
            binding.exit to {
                activity.finish()
            }
        )

        map.forEach { (view, action) -> view.setOnClickListener { action(it) } }

        toolAnimator = SimpleAnimator(
            onUpdate = ::onUpdateTool,
            onShowEnd = {
                binding.collapse.visibility = View.VISIBLE
                binding.expand.visibility = View.INVISIBLE
            },
            onHideEnd = {
                binding.collapse.visibility = View.INVISIBLE
                binding.expand.visibility = View.VISIBLE
            }
        )

        binding.switchVideoArea.isChecked = viewModel.videoAreaShown.value
        binding.switchVideoArea.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVideoShown(isChecked)
            binding.settingLayout.visibility = View.GONE
        }

        binding.switchVideo.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableVideo(isChecked)
        }

        binding.switchAudio.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAudio(isChecked)
        }
    }

    private val expandHeight = activity.dp2px(128)
    private val collapseHeight = activity.dp2px(32)

    private fun onUpdateTool(value: Float) {
        val layoutParams = binding.extTools.layoutParams
        layoutParams.height = collapseHeight + (value * (expandHeight - collapseHeight)).toInt()
        binding.extTools.layoutParams = layoutParams
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
    }
}