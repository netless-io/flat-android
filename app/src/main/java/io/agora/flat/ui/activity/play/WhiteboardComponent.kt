package io.agora.flat.ui.activity.play

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.R
import io.agora.flat.R.id.fast_tools_addition_layout
import io.agora.flat.data.model.WindowAppItem
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.di.interfaces.BoardRoom
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.util.getViewRect
import io.agora.flat.util.isDarkMode
import io.agora.flat.util.isTabletMode

class WhiteboardComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface BoardComponentEntryPoint {
        fun boardRoom(): BoardRoom
    }

    private lateinit var binding: ComponentWhiteboardBinding
    private lateinit var boardRoom: BoardRoom

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
            RoomOverlayManager.observeShowId().collect { areaId ->
                if (areaId != RoomOverlayManager.AREA_ID_FASTBOARD) {
                    boardRoom.hideAllOverlay()
                }

                binding.windowAppsLayout.root.isVisible = areaId == RoomOverlayManager.AREA_ID_APPS

                binding.clickHandleView.show(areaId != RoomOverlayManager.AREA_ID_NO_OVERLAY) {
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY)
                }
            }
        }
    }

    private fun initWhiteboard() {
        boardRoom.setupView(binding.fastboardView)
        boardRoom.setRoomController(FlatControllerGroup(binding.flatControllerLayout))
        boardRoom.setDarkMode(activity.isDarkMode())

        if (activity.isTabletMode()) {
            initAdditionLayout(binding.flatControllerLayout)
        }
    }

    private fun initAdditionLayout(bindView: View) {
        val additionLayout = bindView.findViewById<ViewGroup>(fast_tools_addition_layout)
        val view = activity.layoutInflater.inflate(R.layout.layout_flat_tools_additions, additionLayout, true)
        view.setOnClickListener {
            if (RoomOverlayManager.getShowId() == RoomOverlayManager.AREA_ID_APPS) {
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY)
            } else {
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_APPS)
            }
        }
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
                return@addOnLayoutChangeListener
            }
            updateToolboxMarginBottom(binding.root.height - view.getViewRect(binding.root).bottom)
        }

        val windowAppAdapter = WindowAppAdapter(WindowAppItem.apps)
        windowAppAdapter.setOnItemClickListener { _, itemData ->
            RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY)
            boardRoom.insertApp(itemData.kind)
        }
        binding.windowAppsLayout.windowAppList.adapter = windowAppAdapter
        binding.windowAppsLayout.windowAppList.layoutManager = GridLayoutManager(activity, 4)
    }

    private fun updateToolboxMarginBottom(bottom: Int) {
        val layoutParams = binding.windowAppsLayout.root.layoutParams as MarginLayoutParams
        layoutParams.bottomMargin = bottom
        binding.windowAppsLayout.root.layoutParams = layoutParams
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        boardRoom.release()
    }
}
