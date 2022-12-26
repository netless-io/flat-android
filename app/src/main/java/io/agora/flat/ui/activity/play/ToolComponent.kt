package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.R
import io.agora.flat.data.AppEnv
import io.agora.flat.databinding.ComponentToolBinding
import io.agora.flat.di.interfaces.BoardRoom
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.view.InviteDialog
import io.agora.flat.ui.view.OwnerExitDialog
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class ToolComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ToolComponentEntryPoint {
        fun boardRoom(): BoardRoom
        fun appEnv(): AppEnv
    }

    private lateinit var binding: ComponentToolBinding
    private lateinit var toolAnimator: SimpleAnimator

    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private lateinit var boardRoom: BoardRoom
    private lateinit var appEnv: AppEnv
    private lateinit var userListAdapter: UserListAdapter

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        injectApi()
        initView()
        observeState()
    }

    private fun injectApi() {
        val entryPoint = EntryPointAccessors.fromActivity(activity, ToolComponentEntryPoint::class.java)
        boardRoom = entryPoint.boardRoom()
        appEnv = entryPoint.appEnv()
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            RoomOverlayManager.observeShowId().collect { areaId ->
                if (areaId != RoomOverlayManager.AREA_ID_SETTING) {
                    hideSettingLayout()
                }
                binding.cloudservice.isSelected = areaId == RoomOverlayManager.AREA_ID_CLOUD_STORAGE
                if (areaId != RoomOverlayManager.AREA_ID_USER_LIST) {
                    hideUserListLayout()
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.students.collect {
                userListAdapter.setData(it)

                binding.layoutUserList.studentSize.text = activity.getString(
                    R.string.user_list_student_size_format,
                    "${it.size}"
                )

                val handUpCount = it.count { user -> user.isRaiseHand }
                binding.userlistDot.isVisible = handUpCount > 0
                binding.layoutUserList.handupSize.text = activity.getString(
                    R.string.user_list_student_size_format,
                    "$handUpCount/${it.size}"
                )
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.teacher.collect {
                binding.layoutUserList.teacherAvatar.load(it?.avatarURL) {
                    crossfade(true)
                    placeholder(R.drawable.ic_class_room_user_avatar)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.recordState.collect { recordState ->
                val isRecording = recordState != null
                binding.startRecord.isVisible = !isRecording
                binding.stopRecord.isVisible = isRecording
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.state.filterNotNull().collect {
                binding.recordLayout.isVisible = it.isOwner
                binding.cloudservice.isVisible = it.allowDraw

                binding.handupLayout.isVisible = it.shouldShowRaiseHand
                binding.handup.isSelected = it.isRaiseHand

                binding.layoutSettings.switchVideo.isEnabled = it.isOnStage
                binding.layoutSettings.switchAudio.isEnabled = it.isOnStage

                binding.layoutSettings.switchVideo.isChecked = it.videoOpen
                binding.layoutSettings.switchAudio.isChecked = it.audioOpen

                binding.layoutUserList.teacherName.text =
                    activity.getString(R.string.user_list_teacher_name_format, it.ownerName)
                binding.layoutUserList.stageOffAll.isVisible = it.isOwner
                binding.layoutUserList.muteMicAll.isVisible = it.isOwner
            }
        }

        lifecycleScope.launch {
            viewModel.messageAreaShown.collect {
                binding.message.isSelected = it
            }
        }

        lifecycleScope.launch {
            viewModel.messageCount.collect { count ->
                binding.messageDot.isVisible = count > 0 &&
                        RoomOverlayManager.getShowId() != RoomOverlayManager.AREA_ID_MESSAGE
            }
        }
    }

    private fun hideSettingLayout() {
        binding.layoutSettings.settingLayout.isVisible = false
        binding.setting.isSelected = false
    }

    private fun showSettingLayout() {
        binding.layoutSettings.settingLayout.isVisible = true
        binding.setting.isSelected = true
    }

    private fun hideUserListLayout() {
        binding.layoutUserList.root.isVisible = false
        binding.userlist.isSelected = false
    }

    private val expectedUserListWidth = activity.resources.getDimensionPixelSize(R.dimen.room_class_user_list_width)
    private val panelMargin = activity.resources.getDimensionPixelSize(R.dimen.room_class_panel_margin_horizontal)

    private fun showUserListLayout() {
        binding.layoutUserList.root.isVisible = true
        binding.userlist.isSelected = true

        // resize for small size devices
        val limitedWidth = binding.root.width - 2 * panelMargin
        if (expectedUserListWidth > limitedWidth) {
            val layoutParams = binding.layoutUserList.root.layoutParams
            layoutParams.width = limitedWidth
            binding.layoutUserList.root.layoutParams = layoutParams
        }
    }

    private fun initView() {
        binding = ComponentToolBinding.inflate(activity.layoutInflater, rootView, true)

        val map: Map<View, (View) -> Unit> = mapOf(
            binding.message to {
                binding.messageDot.isVisible = false
                val shown = RoomOverlayManager.getShowId() != RoomOverlayManager.AREA_ID_MESSAGE
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_MESSAGE, shown)
            },
            binding.cloudservice to {
                val targetShow = RoomOverlayManager.getShowId() != RoomOverlayManager.AREA_ID_CLOUD_STORAGE
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_CLOUD_STORAGE, targetShow)
            },
            binding.userlist to {
                with(binding.layoutUserList.root) {
                    if (isVisible) {
                        hideUserListLayout()
                    } else {
                        showUserListLayout()
                    }
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_USER_LIST, isVisible)
                }
            },
            binding.invite to {
                showInviteDialog()
                binding.invite.isSelected = true
            },
            binding.setting to {
                with(binding.layoutSettings.settingLayout) {
                    if (isVisible) {
                        hideSettingLayout()
                    } else {
                        showSettingLayout()
                    }
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_SETTING, isVisible)
                }
            },
            binding.collapse to { toolAnimator.hide() },
            binding.expand to { toolAnimator.show() },
            binding.layoutSettings.exit to { handleExit() },

            binding.startRecord to {
                lifecycleScope.launch {
                    binding.startRecord.isEnabled = false
                    binding.startRecord.alpha = 0.2f
                    viewModel.startRecord()
                    activity.showToast(R.string.record_started_toast)
                    binding.startRecord.alpha = 1f
                    binding.startRecord.isEnabled = true
                }
            },
            binding.stopRecord to {
                lifecycleScope.launch {
                    binding.stopRecord.isEnabled = false
                    binding.stopRecord.alpha = 0.2f
                    viewModel.stopRecord()
                    activity.showToast(R.string.record_stopped_toast)
                    binding.stopRecord.alpha = 1f
                    binding.stopRecord.isEnabled = true
                }
            },
            binding.handup to {
                viewModel.raiseHand()
            }
        )

        map.forEach { (view, action) -> view.setOnClickListener { action(it) } }

        toolAnimator = SimpleAnimator(
            onUpdate = ::onUpdateTool,
            onShowEnd = {
                binding.collapse.visibility = View.VISIBLE
                binding.expand.visibility = View.INVISIBLE
                resetToolsLayoutParams()
            },
            onHideEnd = {
                binding.collapse.visibility = View.INVISIBLE
                binding.expand.visibility = View.VISIBLE
            }
        )

        binding.layoutSettings.switchVideoArea.isChecked = viewModel.videoAreaShown.value
        binding.layoutSettings.switchVideoArea.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVideoAreaShown(isChecked)
            hideSettingLayout()
        }

        binding.layoutSettings.switchVideo.setOnCheckedChangeListener { it, isChecked ->
            if (it.isPressed) {
                viewModel.enableVideo(isChecked)
            }
        }

        binding.layoutSettings.switchAudio.setOnCheckedChangeListener { it, isChecked ->
            if (it.isPressed) {
                viewModel.enableAudio(isChecked)
            }
        }
        binding.layoutSettings.close.setOnClickListener {
            hideSettingLayout()
            RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_SETTING, false)
        }
        binding.layoutSettings.root.setOnClickListener {
            // block event
        }

        userListAdapter = UserListAdapter(viewModel)
        binding.layoutUserList.userList.adapter = userListAdapter
        binding.layoutUserList.userList.layoutManager = LinearLayoutManager(activity)
        binding.layoutUserList.close.setOnClickListener {
            hideUserListLayout()
            RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_USER_LIST, false)
        }
        binding.layoutUserList.root.setOnClickListener {
            // block event
        }
        binding.layoutUserList.stageOffAll.setOnClickListener {
            viewModel.stageOffAll()
        }
        binding.layoutUserList.muteMicAll.setOnClickListener {
            viewModel.muteAllMic()
        }
    }

    private fun handleExit() {
        val state = viewModel.state.value ?: return
        if (state.shouldShowExitDialog) {
            showOwnerExitDialog()
        } else {
            updateRoomsAndFinish()
            // showAudienceExitDialog()
        }
    }

    private fun showOwnerExitDialog() {
        val dialog = OwnerExitDialog()
        dialog.setListener(object : OwnerExitDialog.Listener {
            override fun onClose() {

            }

            // 挂起房间
            override fun onLeftButtonClick() {
                updateRoomsAndFinish()
            }

            // 结束房间
            override fun onRightButtonClick() {
                lifecycleScope.launch {
                    if (viewModel.stopClass()) {
                        updateRoomsAndFinish()
                    } else {
                        activity.showToast(R.string.room_class_stop_class_fail)
                    }
                }
            }

            override fun onDismiss() {
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_OWNER_EXIT_DIALOG, false)
            }
        })
        dialog.show(activity.supportFragmentManager, "OwnerExitDialog")
        RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_OWNER_EXIT_DIALOG, true)

        activity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun updateRoomsAndFinish() {
        viewModel.sendGlobalEvent(RoomsUpdated)
        activity.finish()
    }

    private fun showAudienceExitDialog() {

    }

    private fun showInviteDialog() {
        val inviteInfo = viewModel.getInviteInfo() ?: return
        val inviteTitle = activity.getString(R.string.invite_title_format, inviteInfo.username)
        val inviteLink = inviteInfo.link
        val datetime = "${FlatFormatter.date(inviteInfo.beginTime)} ${
            FlatFormatter.timeDuring(inviteInfo.beginTime, inviteInfo.endTime)
        }"
        val roomTitle = inviteInfo.roomTitle
        val roomUuid = inviteInfo.roomUuid

        val inviteText = activity.getString(
            R.string.invite_text_format,
            inviteInfo.username,
            roomTitle,
            datetime,
            roomUuid,
            inviteLink
        )

        val dialog = InviteDialog().apply {
            arguments = Bundle().apply {
                putString(InviteDialog.INVITE_TITLE, inviteTitle)
                putString(InviteDialog.ROOM_NUMBER, roomUuid)
                putString(InviteDialog.ROOM_TITLE, roomTitle)
                putString(InviteDialog.ROOM_TIME, datetime)
            }
        }
        dialog.setListener(object : InviteDialog.Listener {
            override fun onCopy() {
                viewModel.setClipboard(inviteText)
                activity.showToast(R.string.copy_success)
            }

            override fun onHide() {
                binding.invite.isSelected = false
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_INVITE_DIALOG, false)
            }
        })
        dialog.show(activity.supportFragmentManager, "InviteDialog")
        RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_INVITE_DIALOG, true)
    }

    private val itemSize = activity.resources.getDimensionPixelSize(R.dimen.room_class_button_area_size)

    private val collapseHeight = itemSize
    private val expandHeight: Int
        get() {
            val visibleCount = binding.extTools.children.count { it.isVisible }
            return itemSize * visibleCount
        }

    private fun onUpdateTool(value: Float) {
        val layoutParams = binding.extTools.layoutParams
        layoutParams.height = collapseHeight + (value * (expandHeight - collapseHeight)).toInt()
        binding.extTools.layoutParams = layoutParams
    }

    // TODO free layoutParams height for visible change of items
    private fun resetToolsLayoutParams() {
        val layoutParams = binding.extTools.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.extTools.layoutParams = layoutParams
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleExit()
        }
    }
}
