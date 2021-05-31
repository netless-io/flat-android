package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.agora.flat.databinding.ComponentToolBinding
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.view.InviteDialog
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.dp2px
import io.agora.flat.util.showDebugToast
import io.agora.flat.util.showToast
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
                    is ClassRoomEvent.OperatingAreaShown -> handleAreaShown(it.areaId)
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
        if (areaId != ClassRoomEvent.AREA_ID_SETTING) {
            hideSettingLayout()
        }
    }

    private fun hideSettingLayout() {
        binding.settingLayout.isVisible = false
        binding.setting.isSelected = false
    }

    private fun showSettingLayout() {
        binding.settingLayout.isVisible = true
        binding.setting.isSelected = true
    }

    private fun initView() {
        binding = ComponentToolBinding.inflate(activity.layoutInflater, rootView, true)

        val map: Map<View, (View) -> Unit> = mapOf(
            binding.message to { activity.showDebugToast("Not Supported Yet") },
            binding.cloudservice to { activity.showDebugToast("Not Supported Yet") },
            binding.invite to {
                showInviteDialog()
                binding.invite.isSelected = true
                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_INVITE_DIALOG)
            },
            binding.setting to {
                if (binding.settingLayout.isVisible) {
                    hideSettingLayout()
                } else {
                    showSettingLayout()
                }
                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_SETTING)
            },
            binding.collapse to { toolAnimator.hide() },
            binding.expand to { toolAnimator.show() },
            binding.exit to { activity.finish() },
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
            hideSettingLayout()
        }

        binding.switchVideo.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableVideo(isChecked)
        }

        binding.switchAudio.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAudio(isChecked)
        }
    }

    private fun showInviteDialog() {
        val state = viewModel.state.value
        val inviteTitle = "${state.currentUserName} 邀请你加入 Flat 房间"
        val roomTime = "${FlatFormatter.date(state.beginTime)} ${
            FlatFormatter.timeDuring(
                state.beginTime,
                state.endTime
            )
        }"

        val copyText = """
                            |$inviteTitle
                            |房间主题：${state.title}
                            |开始时间：${roomTime}
                            |房间号：${state.roomUUID}
                            |打开（没有安装的话请先下载并安装）并登录 Flat，点击加入房间，输入房间号即可加入和预约
                        """.trimMargin()

        val dialog = InviteDialog()
        dialog.arguments = Bundle().apply {
            putString(InviteDialog.INVITE_TITLE, inviteTitle)
            putString(InviteDialog.ROOM_TITLE, state.title)
            putString(InviteDialog.ROOM_NUMBER, state.roomUUID.substringAfterLast("-"))
            putString(InviteDialog.ROOM_TIME, roomTime)
        }
        dialog.setListener(object : InviteDialog.Listener {
            override fun onCopy() {
                viewModel.onCopyText(copyText)
                activity.showToast("复制成功")
            }

            override fun onHide() {
                binding.invite.isSelected = false
            }
        })
        dialog.show(activity.supportFragmentManager, "InviteDialog")
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
