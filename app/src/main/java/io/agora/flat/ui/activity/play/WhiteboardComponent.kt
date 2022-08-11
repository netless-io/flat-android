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
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.di.interfaces.IBoardRoom
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.isDarkMode

class WhiteboardComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {

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
        viewModel.onWhiteboardInit()

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
    }

    private fun initWhiteboard() {
        boardRoom.initSdk(binding.fastboardView)
        boardRoom.setRoomController(FlatControllerGroup(binding.flatControllerLayout))
        boardRoom.setDarkMode(activity.isDarkMode())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        boardRoom.release()
    }
}
