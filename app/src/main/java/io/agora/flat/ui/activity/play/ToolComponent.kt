package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.agora.flat.R
import io.agora.flat.databinding.ComponentToolBinding
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.view.InviteDialog
import io.agora.flat.ui.view.OwnerExitDialog
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.dp2px
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

    private lateinit var cloudStorageAdapter: CloudStorageAdapter

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
                    is ClassRoomEvent.StartRoomResult -> {
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.roomConfig.collect {
                binding.switchVideo.isChecked = it.enableVideo
                binding.switchAudio.isChecked = it.enableAudio
            }
        }

        lifecycleScope.launch {
            viewModel.state.collect {
                binding.roomStart.isVisible = it.showStartButton
            }
        }

        lifecycleScope.launch {
            viewModel.cloudStorageFiles.collect {
                cloudStorageAdapter.setDataSet(it)
            }
        }

        lifecycleScope.launch {
            viewModel.messageAreaShown.collect {
                binding.message.isSelected = it
            }
        }
    }

    private fun handleAreaShown(areaId: Int) {
        if (areaId != ClassRoomEvent.AREA_ID_SETTING) {
            hideSettingLayout()
        }

        if (areaId != ClassRoomEvent.AREA_ID_CLOUD_STORAGE) {
            hideCloudStorageLayout()
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

    private fun hideCloudStorageLayout() {
        binding.layoutCloudStorage.root.isVisible = false
        binding.cloudservice.isSelected = false
    }

    private fun showCloudStorageLayout() {
        binding.layoutCloudStorage.root.isVisible = true
        binding.cloudservice.isSelected = true
    }

    private fun initView() {
        binding = ComponentToolBinding.inflate(activity.layoutInflater, rootView, true)

        val map: Map<View, (View) -> Unit> = mapOf(
            binding.message to {
                val shown = !viewModel.messageAreaShown.value
                viewModel.setMessageAreaShown(shown)
            },
            binding.cloudservice to {
                if (binding.layoutCloudStorage.root.isVisible) {
                    hideCloudStorageLayout()
                } else {
                    showCloudStorageLayout()
                    viewModel.requestCloudStorageFiles()
                }
                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_CLOUD_STORAGE)
            },
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
            binding.exit to { handleExit() },
            binding.roomStart to {
                lifecycleScope.launch {
                    if (viewModel.startRoomClass()) {
                        activity.showToast(R.string.room_class_start_class_success)
                        binding.roomStart.isVisible = false
                    } else {
                        activity.showToast(R.string.room_class_start_class_fail)
                    }
                }
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
            hideSettingLayout()
        }

        binding.switchVideo.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableVideo(isChecked)
        }

        binding.switchAudio.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAudio(isChecked)
        }

        cloudStorageAdapter = CloudStorageAdapter()
        cloudStorageAdapter.setOnItemClickListener {
            viewModel.insertCourseware(it)
            hideCloudStorageLayout()
        }
        binding.layoutCloudStorage.cloudStorageList.adapter = cloudStorageAdapter
        binding.layoutCloudStorage.cloudStorageList.layoutManager = LinearLayoutManager(activity)
    }

    private fun handleExit() {
        if (viewModel.state.value.needOwnerExitDialog) {
            showOwnerExitDialog()
        } else {
            activity.finish()
            // showAudienceExitDialog()
        }
    }

    private fun showOwnerExitDialog() {
        val dialog = OwnerExitDialog()
        dialog.setListener(object : OwnerExitDialog.Listener {
            override fun onClose() {

            }

            // ????????????
            override fun onLeftButtonClick() {
                activity.finish()
            }

            // ????????????
            override fun onRightButtonClick() {
                lifecycleScope.launch {
                    if (viewModel.stopRoomClass()) {
                        activity.finish()
                    } else {
                        activity.showToast(R.string.room_class_stop_class_fail)
                    }
                }
            }

        })
        dialog.show(activity.supportFragmentManager, "OwnerExitDialog")
        viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_OWNER_EXIT_DIALOG)
    }

    private fun showAudienceExitDialog() {

    }

    private fun showInviteDialog() {
        val state = viewModel.state.value
        val inviteTitle = "${state.currentUserName} ??????????????? Flat ??????"
        val roomTime = "${FlatFormatter.date(state.beginTime)} ${
            FlatFormatter.timeDuring(
                state.beginTime,
                state.endTime
            )
        }"

        val copyText = """
                            |$inviteTitle
                            |???????????????${state.title}
                            |???????????????${roomTime}
                            |????????????${state.roomUUID}
                            |???????????????????????????????????????????????????????????? Flat????????????????????????????????????????????????????????????
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
                activity.showToast("????????????")
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
