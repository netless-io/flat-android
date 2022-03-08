package io.agora.flat.ui.activity.play

import android.content.res.Configuration
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.R
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.di.interfaces.IBoardRoom
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomState
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.isDarkMode
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

class WhiteboardComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = WhiteboardComponent::class.simpleName
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface BoardComponentEntryPoint {
        fun boardRoom(): IBoardRoom
    }

    private lateinit var binding: ComponentWhiteboardBinding
    private val viewModel: ClassRoomViewModel by activity.viewModels()

    private lateinit var boardRoom: IBoardRoom

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        injectApi()
        initView()
        initWhiteboard()
        observeState()
    }

    private fun injectApi() {
        val entryPoint = EntryPointAccessors.fromActivity(activity, BoardComponentEntryPoint::class.java)
        boardRoom = entryPoint.boardRoom()
    }

    private fun initView() {
        binding = ComponentWhiteboardBinding.inflate(activity.layoutInflater, rootView, true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        boardRoom.setDarkMode(activity.isDarkMode())
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            viewModel.roomPlayInfo.filterNotNull().collect {
                boardRoom.join(
                    it.whiteboardRoomUUID,
                    it.whiteboardRoomToken,
                    viewModel.state.value.userUUID,
                    viewModel.state.value.isWritable,
                )
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.roomEvent.collect { event ->
                when (event) {
                    is ClassRoomEvent.NoOptPermission -> {
                        activity.showToast(R.string.class_room_no_operate_permission)
                    }
                    is ClassRoomEvent.InsertImage -> {
                        boardRoom.insertImage(event.imageUrl, event.width, event.height)
                    }
                    is ClassRoomEvent.InsertPpt -> {
                        boardRoom.insertPpt(event.dirPath, event.convertedFiles, event.title)
                    }
                    is ClassRoomEvent.InsertVideo -> {
                        boardRoom.insertVideo(event.videoUrl, event.title)
                    }
                    else -> {; }
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            RoomOverlayManager.observeShowId().collect { areaId ->
                if (areaId != RoomOverlayManager.AREA_ID_FASTBOARD) {
                    boardRoom.hideAllOverlay()
                }

                binding.clickHandleView.show(areaId != RoomOverlayManager.AREA_ID_NO_OVERLAY) {
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.state.filter { it != ClassRoomState.Init }.collect {
                onRoomWritableChange(it.isWritable)
            }
        }
    }

    private fun onRoomWritableChange(writable: Boolean) {
        boardRoom.setWritable(writable)
        val uiSettings = binding.fastboardView.uiSettings
        if (writable) {
            uiSettings.showRoomControllerGroup()
        } else {
            uiSettings.hideRoomControllerGroup()
        }
    }

    private fun initWhiteboard() {
        boardRoom.initSdk(binding.fastboardView)
        boardRoom.setDarkMode(activity.isDarkMode())
        boardRoom.setRoomController(FlatControllerGroup(binding.flatControllerLayout))
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        boardRoom.release()
    }
}
