package io.agora.flat.ui.activity.play

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herewhite.sdk.domain.Appliance
import com.herewhite.sdk.domain.ConvertedFiles
import com.herewhite.sdk.domain.MemberState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.agora.flat.R
import io.agora.flat.common.board.BoardSceneState
import io.agora.flat.common.board.SceneItem
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.databinding.LayoutScenePreviewBinding
import io.agora.flat.di.interfaces.BoardRoomApi
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.view.PaddingItemDecoration
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomState
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.dp2px
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

class WhiteboardComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
    private val scenePreview: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = WhiteboardComponent::class.simpleName
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface BoardComponentEntryPoint {
        fun boardRoom(): BoardRoomApi
    }

    private lateinit var binding: ComponentWhiteboardBinding
    private lateinit var scenePreviewBinding: LayoutScenePreviewBinding
    private val viewModel: ClassRoomViewModel by activity.viewModels()

    private lateinit var boardRoom: BoardRoomApi
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var applianceAdapter: ApplianceAdapter
    private lateinit var slideAdapter: SceneAdapter
    private lateinit var slideAnimator: SimpleAnimator

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
        scenePreviewBinding = LayoutScenePreviewBinding.inflate(activity.layoutInflater, scenePreview, true)

        val map: Map<View, (View) -> Unit> = mapOf(
            binding.undo to { boardRoom.undo() },
            binding.redo to { boardRoom.redo() },
            binding.pageStart to { boardRoom.startPage() },
            binding.pagePrev to { boardRoom.prevPage() },
            binding.pageNext to { boardRoom.nextPage() },
            binding.pageEnd to { boardRoom.finalPage() },
            binding.reset to { boardRoom.resetView() },
            binding.showScenes to { previewSlide() },

            binding.tools to {
                with(binding.appliancesLayout) {
                    isVisible = !isVisible
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_APPLIANCE, isVisible)
                }
            },
            binding.toolsSubPaint to {
                with(binding.toolsSubLayout) {
                    isVisible = !isVisible
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_PAINT, isVisible)
                }
            },
            binding.toolsSubDelete to { boardRoom.deleteSelection() },
            // slide
            scenePreviewBinding.root to { ; },
            scenePreviewBinding.sceneAdd to { boardRoom.addSlideToNext() },
            scenePreviewBinding.sceneDelete to { boardRoom.deleteCurrentSlide() },
            scenePreviewBinding.sceneCover to { slideAnimator.hide() },

            binding.handup to { viewModel.sendRaiseHand() },
        )
        map.forEach { (view, action) -> view.setOnClickListener { action(it) } }

        applianceAdapter = ApplianceAdapter(ApplianceItem.appliancesPhone)
        applianceAdapter.setOnItemClickListener(::onApplianceItemSelected)
        binding.applianceRecyclerView.adapter = applianceAdapter
        binding.applianceRecyclerView.layoutManager = GridLayoutManager(activity, 4)

        colorAdapter = ColorAdapter(ColorItem.colors)
        colorAdapter.setOnItemClickListener(::onColorSelected)
        binding.colorRecyclerView.adapter = colorAdapter
        binding.colorRecyclerView.layoutManager = GridLayoutManager(activity, 4)

        binding.seeker.setOnStrokeChangedListener { boardRoom.setStrokeWidth(it.toDouble()) }

        slideAdapter = SceneAdapter()
        slideAdapter.setOnItemClickListener(object : SceneAdapter.OnItemClickListener {
            override fun onItemClick(index: Int, item: SceneItem) {
                boardRoom.setSceneIndex(index)
            }
        })
        with(scenePreviewBinding) {
            sceneRecyclerView.adapter = slideAdapter
            sceneRecyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            sceneRecyclerView.addItemDecoration(PaddingItemDecoration(horizontal = activity.dp2px(8)))
        }

        slideAnimator = SimpleAnimator(
            onUpdate = ::updateSlide,
            onShowStart = {
                scenePreview.isVisible = true
            },
            onHideEnd = {
                scenePreview.isVisible = false
            }
        )

        updateRoomWritable(false)
    }

    private fun onApplianceItemSelected(item: ApplianceItem) {
        when (item) {
            ApplianceItem.OTHER_CLEAR -> {
                binding.appliancesLayout.isVisible = false
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_APPLIANCE, false)

                boardRoom.cleanScene()
            }
            else -> {
                binding.appliancesLayout.isVisible = false
                RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_APPLIANCE, false)

                boardRoom.setAppliance(item.type)
                updateAppliance(viewModel.state.value.isWritable, item.type)
            }
        }
    }

    private fun onColorSelected(item: ColorItem) {
        binding.toolsSubLayout.isVisible = false
        binding.toolsSubPaint.setImageResource(item.drawableRes)

        RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_PAINT, false)
        boardRoom.setStrokeColor(item.color)
    }

    private fun updateAppliance(isWritable: Boolean, appliance: String) {
        binding.tools.setImageResource(ApplianceItem.drawableResOf(appliance))
        binding.tools.isSelected = true

        when (appliance) {
            Appliance.SELECTOR -> {
                binding.toolsSub.isVisible = isWritable
                binding.toolsSubDelete.isVisible = true
                binding.toolsSubPaint.isVisible = false
            }
            Appliance.LASER_POINTER, Appliance.ERASER, Appliance.HAND, Appliance.CLICKER -> {
                binding.toolsSub.isVisible = false
            }
            else -> {
                binding.toolsSub.isVisible = isWritable
                binding.toolsSubDelete.isVisible = false
                binding.toolsSubPaint.isVisible = true
            }
        }
    }

    private val previewWidth = activity.dp2px(128)

    private fun updateSlide(value: Float) {
        val layoutParams = scenePreviewBinding.scenePreview.layoutParams
        layoutParams.height = (previewWidth * value).toInt()
        scenePreviewBinding.scenePreview.layoutParams = layoutParams

        scenePreviewBinding.sceneCover.alpha = value
    }

    private fun previewSlide() {
        slideAnimator.show()
        boardRoom.refreshSceneState()
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            viewModel.roomPlayInfo.filterNotNull().collect {
                boardRoom.join(
                    it.whiteboardRoomUUID,
                    it.whiteboardRoomToken,
                    viewModel.state.value.userUUID,
                    viewModel.state.value.isWritable
                )
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.NoOptPermission -> activity.showToast(R.string.class_room_no_operate_permission)
                    is ClassRoomEvent.InsertImage -> insertImage(it.imageUrl, it.width, it.height)
                    is ClassRoomEvent.InsertPpt -> insertPpt(it.dirPath, it.convertedFiles, it.title)
                    is ClassRoomEvent.InsertVideo -> insertVideo(it.videoUrl, it.title)
                    else -> {; }
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            RoomOverlayManager.observeShowId().collect { areaId ->
                if (areaId != RoomOverlayManager.AREA_ID_APPLIANCE) {
                    binding.appliancesLayout.isVisible = false
                }

                if (areaId != RoomOverlayManager.AREA_ID_PAINT) {
                    binding.toolsSubLayout.isVisible = false
                }

                binding.clickHandleView.show(areaId != RoomOverlayManager.AREA_ID_NO_OVERLAY) {
                    RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.state.filter { it != ClassRoomState.Init }.collect {
                onRoomWritableChange(it.isWritable)

                binding.handup.isVisible = it.showRaiseHand
                binding.handup.isSelected = it.isRaiseHand
            }
        }

        lifecycleScope.launchWhenResumed {
            boardRoom.observerSceneState().collect { sceneState ->
                slideAdapter.setDataSetAndIndex(sceneState.scenes, sceneState.index)
                updatePageIndicate(sceneState)
            }
        }

        lifecycleScope.launchWhenResumed {
            boardRoom.observerMemberState().collect { memberState ->
                val colorItem = ColorItem.of(memberState.strokeColor)
                if (colorItem != null) {
                    updateMemberState(memberState)
                } else {
                    boardRoom.setStrokeColor(ColorItem.colors[0].color)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            boardRoom.observerUndoRedoState().collect {
                binding.undo.isEnabled = it.undoCount != 0L
                binding.redo.isEnabled = it.redoCount != 0L
            }
        }
    }

    private fun insertImage(imageUrl: String, w: Int, h: Int) {
        boardRoom.insertImage(imageUrl, w, h)
    }

    private fun insertPpt(dir: String, convertedFiles: ConvertedFiles, title: String) {
        boardRoom.insertPpt(dir, convertedFiles, title)
    }

    private fun insertVideo(videoUrl: String, title: String) {
        boardRoom.insertVideo(videoUrl, title)
    }

    private fun onRoomWritableChange(writable: Boolean) {
        boardRoom.setWritable(writable)
        updateRoomWritable(writable)
    }

    private fun updateRoomWritable(writable: Boolean) {
        Log.d(TAG, "updateRoomWritable $writable")
        binding.boardToolsLayout.isVisible = writable
        // binding.showScenes.isVisible = writable
        // binding.pageIndicateLy.isVisible = writable
        binding.undoRedoLayout.isVisible = writable
    }

    private fun initWhiteboard() {
        boardRoom.initSdk(binding.whiteboardView)
    }

    private fun updateMemberState(memberState: MemberState) {
        with(memberState) {
            updateAppliance(viewModel.state.value.isWritable, currentApplianceName)
            applianceAdapter.setCurrentAppliance(ApplianceItem.of(currentApplianceName))

            binding.seeker.setStrokeWidth(strokeWidth.toInt())

            val colorItem = ColorItem.of(strokeColor) ?: ColorItem.colors[0]
            binding.toolsSubPaint.setImageResource(colorItem.drawableRes)
            colorAdapter.setCurrentColor(colorItem.color)
        }
    }

    private fun updatePageIndicate(sceneState: BoardSceneState) {
        sceneState.run {
            val currentDisplay = index + 1
            val lastDisplay = scenes.size
            binding.pageIndicate.text = "${currentDisplay}/${lastDisplay}"
            binding.pagePrev.isEnabled = currentDisplay != 1
            binding.pageNext.isEnabled = currentDisplay != lastDisplay
            binding.pageStart.isEnabled = currentDisplay != 1
            binding.pageEnd.isEnabled = currentDisplay != lastDisplay
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        boardRoom.release()
    }
}