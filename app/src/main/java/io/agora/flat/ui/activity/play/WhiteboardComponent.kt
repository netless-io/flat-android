package io.agora.flat.ui.activity.play

import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.flat.BuildConfig
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.databinding.LayoutScenePreviewBinding
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.view.PaddingItemDecoration
import io.agora.flat.ui.view.StrokeSeeker
import io.agora.flat.ui.viewmodel.ClassRoomEvent
import io.agora.flat.ui.viewmodel.ClassRoomState
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.dp2px
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class WhiteboardComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
    private val scenePreview: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = WhiteboardComponent::class.simpleName
    }

    private lateinit var binding: ComponentWhiteboardBinding
    private lateinit var scenePreviewBinding: LayoutScenePreviewBinding

    private lateinit var whiteSdk: WhiteSdk
    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private var room: Room? = null
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var applianceAdapter: ApplianceAdapter
    private lateinit var slideAdapter: SceneAdapter
    private lateinit var slideAnimator: SimpleAnimator

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        initWhiteboard()
        observeState()
    }

    private fun initView() {
        binding = ComponentWhiteboardBinding.inflate(activity.layoutInflater, rootView, true)
        scenePreviewBinding = LayoutScenePreviewBinding.inflate(activity.layoutInflater, scenePreview, true)
        val map: Map<View, (View) -> Unit> = mapOf(
            binding.undo to { room?.undo() },
            binding.redo to { room?.redo() },
            binding.pageStart to { room?.setSceneIndex(0, null) },
            binding.pagePreview to { room?.pptPreviousStep() },
            binding.pageNext to { room?.pptNextStep() },
            binding.pageEnd to {
                room?.sceneState?.apply {
                    room?.setSceneIndex(scenes.size - 1, null)
                }
            },
            binding.reset to {
                room?.getSceneState(object : Promise<SceneState> {
                    override fun then(sceneState: SceneState) {
                        val scene = sceneState.scenes[sceneState.index]
                        if (scene.ppt != null) {
                            room?.scalePptToFit()
                        } else {
                            room?.moveCamera(CameraConfig().apply {
                                scale = 1.0
                                centerX = 0.0
                                centerY = 0.0
                                animationMode = AnimationMode.Continuous
                            })
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                    }
                })
            },
            binding.showScenes to { previewSlide() },

            binding.tools to {
                with(binding.toolsLayout) { isVisible = !isVisible }
                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_APPLIANCE)
            },
            binding.toolsSubPaint to {
                with(binding.toolsSubLayout) { isVisible = !isVisible }
                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_PAINT)
            },
            binding.toolsSubDelete to {
                room?.deleteOperation()
            },
            // slide
            scenePreviewBinding.root to {

            },
            scenePreviewBinding.sceneAdd to {
                addSlideToNext()
            },
            scenePreviewBinding.sceneDelete to {
                deleteCurrentSlide()
            },
            scenePreviewBinding.sceneCover to {
                slideAnimator.hide()
            },
            binding.handup to {
                viewModel.sendRaiseHand()
            }
        )

        map.forEach { (view, action) -> view.setOnClickListener { action(it) } }

        applianceAdapter = ApplianceAdapter(ApplianceItem.appliancesPhone)
        applianceAdapter.setOnItemClickListener {
            when (it) {
                ApplianceItem.OTHER_CLEAR -> {
                    binding.toolsLayout.visibility = View.GONE
                    room?.cleanScene(true)
                }
                else -> {
                    setAppliance(it.type)
                    onSelectAppliance(it)
                }
            }
        }
        binding.applianceRecyclerView.adapter = applianceAdapter
        binding.colorRecyclerView.layoutManager = GridLayoutManager(activity, 4)

        colorAdapter = ColorAdapter(ColorItem.colors)
        colorAdapter.setOnItemClickListener(object : ColorAdapter.OnItemClickListener {
            override fun onColorSelected(item: ColorItem) {
                binding.toolsSubLayout.isVisible = false
                room?.memberState = room?.memberState?.apply {
                    strokeColor = item.color
                }
                binding.toolsSubPaint.setImageResource(item.drawableRes)
            }
        })
        binding.colorRecyclerView.adapter = colorAdapter
        binding.colorRecyclerView.layoutManager = GridLayoutManager(activity, 4)
        binding.seeker.setOnStrokeChangedListener(object : StrokeSeeker.OnStrokeChangedListener {
            override fun onStroke(width: Int) {
                room?.memberState = room?.memberState?.apply {
                    strokeWidth = width.toDouble()
                }
            }
        })

        slideAdapter = SceneAdapter()
        slideAdapter.setOnItemClickListener(object : SceneAdapter.OnItemClickListener {
            override fun onItemClick(index: Int, item: SceneItem) {
                room?.setSceneIndex(index, null)
            }
        })
        scenePreviewBinding.sceneRecyclerView.adapter = slideAdapter
        scenePreviewBinding.sceneRecyclerView.layoutManager =
            LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        scenePreviewBinding.sceneRecyclerView.addItemDecoration(PaddingItemDecoration(horizontal = activity.dp2px(8)))

        slideAnimator = SimpleAnimator(
            onUpdate = ::updateSlide,
            onShowStart = {
                scenePreview.isVisible = true
            },
            onHideEnd = {
                scenePreview.isVisible = false
            }
        )

        setViewWritable(false)
    }

    private fun onSelectAppliance(appliance: ApplianceItem) {
        binding.toolsLayout.isVisible = false
        updateAppliance(viewModel.state.value.isWritable, appliance.type)
    }

    private fun setAppliance(type: String) {
        room?.memberState = MemberState().apply {
            currentApplianceName = type
        }
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

        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                val sceneDir = sceneState.scenePath.substringBeforeLast('/')
                val list = sceneState.scenes.filterNotNull().map {
                    SceneItem(sceneDir + "/" + it.name, it.ppt?.preview)
                }
                slideAdapter.setDataSetAndIndex(list, sceneState.index)
            }

            override fun catchEx(t: SDKError?) {
            }
        })
    }

    private var targetIndex: Int = 0

    private fun addSlideToNext() {
        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                val sceneList = sceneState.scenes.toMutableList()
                val sceneDir = sceneState.scenePath.substringBeforeLast('/')
                targetIndex = sceneState.index + 1

                val scene = Scene(UUID.randomUUID().toString())
                room?.putScenes(sceneDir, arrayOf(scene), targetIndex)
                room?.setSceneIndex(targetIndex, null)

                sceneList.add(targetIndex, scene)
                val list = sceneList.filterNotNull().map {
                    SceneItem(sceneDir + "/" + it.name, it.ppt?.preview)
                }
                slideAdapter.setDataSetAndIndex(list, targetIndex)
            }

            override fun catchEx(t: SDKError?) {
            }
        })
    }

    private fun deleteCurrentSlide() {
        room?.getSceneState(object : Promise<SceneState> {
            override fun then(sceneState: SceneState) {
                val sceneList = sceneState.scenes.toMutableList()
                val sceneDir = sceneState.scenePath.substringBeforeLast('/')

                room?.removeScenes(sceneState.scenePath)
                sceneList.removeAt(sceneState.index)

                val list = sceneList.filterNotNull().map {
                    SceneItem(sceneDir + "/" + it.name, it.ppt?.preview)
                }
                slideAdapter.setDataSetAndIndex(list, sceneState.index)
            }

            override fun catchEx(t: SDKError?) {
            }
        })
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.roomPlayInfo.filterNotNull().collect {
                join(it.whiteboardRoomUUID, it.whiteboardRoomToken)
            }
        }

        lifecycleScope.launch {
            viewModel.roomEvent.collect {
                when (it) {
                    is ClassRoomEvent.OperatingAreaShown -> handleAreaShown(it.areaId)
                    is ClassRoomEvent.NoOptPermission -> activity.showToast(R.string.class_room_no_operate_permission)
                    is ClassRoomEvent.InsertImage -> insertImage(it.imageUrl, it.width, it.height)
                    is ClassRoomEvent.InsertPpt -> insertPpt(it.dirPath, it.convertedFiles, it.title)
                    is ClassRoomEvent.InsertVideo -> insertVideo(it.videoUrl, it.title)
                    else -> {; }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.state.filter { it != ClassRoomState.Init }.collect {
                setRoomWritable(it.isWritable)

                binding.handup.isVisible = it.showRaiseHand
                binding.handup.isSelected = it.isRaiseHand

                room?.setViewMode(it.viewMode)
            }
        }
    }

    private fun insertImage(imageUrl: String, w: Int, h: Int) {
        val uuid = UUID.randomUUID().toString()
        room?.insertImage(ImageInformation().apply {
            this.uuid = uuid
            width = w.toDouble()
            height = h.toDouble()
            centerX = 0.0
            centerY = 0.0
        })
        room?.completeImageUpload(uuid, imageUrl)
    }

    private fun insertPpt(dirpath: String, convertedFiles: ConvertedFiles, title: String) {
        val param = WindowAppParam.createDocsViewerApp(dirpath, convertedFiles.scenes, title)
        room?.addApp(param, null)
    }

    private fun insertVideo(videoUrl: String, title: String) {
        val param = WindowAppParam.createMediaPlayerApp(videoUrl, title)
        room?.addApp(param, null)
    }

    private fun setRoomWritable(writable: Boolean) {
        room ?: return
        room?.setWritable(writable, object : Promise<Boolean> {
            override fun then(result: Boolean) {
                if (result) {
                    room?.disableSerialization(false)
                }
            }

            override fun catchEx(error: SDKError) {
            }
        })
        setViewWritable(writable)
    }

    private fun setViewWritable(writable: Boolean) {
        binding.tools.isVisible = writable
        binding.toolsSub.isVisible = writable

        // TODO
        // binding.showScenes.isVisible = writable
        // binding.pageIndicateLy.isVisible = writable
        binding.undoRedoLayout.isVisible = writable
    }

    private fun handleAreaShown(areaId: Int) {
        if (areaId != ClassRoomEvent.AREA_ID_APPLIANCE) {
            binding.toolsLayout.isVisible = false
        }
        if (areaId != ClassRoomEvent.AREA_ID_PAINT) {
            binding.toolsSubLayout.isVisible = false
        }
    }

    private fun initWhiteboard() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        val configuration = WhiteSdkConfiguration(Constants.NETLESS_APP_IDENTIFIER, true)
        configuration.isUserCursor = true
        configuration.isEnableSyncedStore = true

        whiteSdk = WhiteSdk(binding.whiteboardView, activity, configuration)
        whiteSdk.setCommonCallbacks(object : CommonCallbacks {
            override fun urlInterrupter(sourceUrl: String): String {
                return sourceUrl
            }

            override fun onMessage(message: JSONObject) {
                Log.d(TAG, message.toString())
            }

            override fun sdkSetupFail(error: SDKError) {
                Log.e(TAG, "sdkSetupFail $error")
            }

            override fun throwError(args: Any) {
                Log.e(TAG, "throwError $args")
            }

            override fun onPPTMediaPlay() {
                Log.d(TAG, "onPPTMediaPlay")
            }

            override fun onPPTMediaPause() {
                Log.d(TAG, "onPPTMediaPause")
            }
        })
    }

    private var roomListener = object : RoomListener {
        override fun onPhaseChanged(phase: RoomPhase) {
            Log.d(TAG, "onPhaseChanged:${phase.name}")
            when (phase) {
                RoomPhase.connecting -> {
                }
                RoomPhase.connected -> {
                }
                RoomPhase.reconnecting -> {
                }
                RoomPhase.disconnecting -> {
                }
                RoomPhase.disconnected -> {
                }
            }
        }

        override fun onDisconnectWithError(e: Exception?) {
            Log.d(TAG, "onDisconnectWithError:${e?.message}")
        }

        override fun onKickedWithReason(reason: String?) {
            Log.d(TAG, "onKickedWithReason:${reason}")
        }

        override fun onRoomStateChanged(modifyState: RoomState) {
            Log.d(TAG, "onRoomStateChanged:${modifyState}")
            modifyState.sceneState?.let(::onSceneStateChanged)
            modifyState.memberState?.let(::onMemberStateChanged)
            modifyState.broadcastState?.let(::onBroadcastStateChanged)
        }

        override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
            Log.d(TAG, "onCanUndoStepsUpdate:${canUndoSteps}")
            onUndoStepsChanged(canUndoSteps)
        }

        override fun onCanRedoStepsUpdate(canRedoSteps: Long) {
            Log.d(TAG, "onCanRedoStepsUpdate:${canRedoSteps}")
            onRedoStepsChanged(canRedoSteps)
        }

        override fun onCatchErrorWhenAppendFrame(userId: Long, error: Exception?) {
            Log.w(TAG, "onCatchErrorWhenAppendFrame:${error}")
        }
    }

    @UiThread
    private fun onUndoStepsChanged(canUndoSteps: Long) {
        binding.undo.isEnabled = canUndoSteps != 0L
    }

    @UiThread
    private fun onRedoStepsChanged(canRedoSteps: Long) {
        binding.redo.isEnabled = canRedoSteps != 0L
    }

    private var joinRoomCallback = object : Promise<Room> {
        override fun then(room: Room) {
            this@WhiteboardComponent.room = room
            onInitRoomState(room.roomState)
            setRoomWritable(viewModel.state.value.isWritable)
        }

        override fun catchEx(t: SDKError) {
            // showError Dialog & restart activity
        }
    }

    private fun onInitRoomState(roomState: RoomState) {
        roomState.memberState?.let(::onMemberStateChanged)
        roomState.sceneState?.let(::onSceneStateChanged)
    }

    private fun onMemberStateChanged(memberState: MemberState) {
        with(memberState) {
            updateAppliance(viewModel.state.value.isWritable, currentApplianceName)
            applianceAdapter.setCurrentAppliance(ApplianceItem.of(currentApplianceName))

            binding.seeker.setStrokeWidth(strokeWidth.toInt())
            val item = ColorItem.of(strokeColor)
            colorAdapter.setCurrentColor(ColorItem.of(strokeColor).color)
            binding.toolsSubPaint.setImageResource(item.drawableRes)
        }
    }

    private fun onSceneStateChanged(sceneState: SceneState) {
        sceneState.apply {
            val currentDisplay = index + 1
            val lastDisplay = scenes.size
            binding.pageIndicate.text = "${currentDisplay}/${lastDisplay}"
            binding.pagePreview.isEnabled = currentDisplay != 1
            binding.pageNext.isEnabled = currentDisplay != lastDisplay
            binding.pageStart.isEnabled = currentDisplay != 1
            binding.pageEnd.isEnabled = currentDisplay != lastDisplay
        }
    }

    private fun onBroadcastStateChanged(broadcastState: BroadcastState) {
        viewModel.updateViewMode(broadcastState.mode)
    }

    private fun join(roomUUID: String, roomToken: String) {
        val roomParams = RoomParams(roomUUID, roomToken).apply {
            useMultiViews = true

            val styleMap = HashMap<String, String>()
            styleMap["bottom"] = "30px"
            styleMap["right"] = "44px"
            styleMap["position"] = "fixed"

            windowParams = WindowParams().setChessboard(false).setDebug(true).setCollectorStyles(styleMap)
        }
        whiteSdk.joinRoom(roomParams, roomListener, joinRoomCallback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        whiteSdk.releaseRoom()
        room?.disconnect()
    }
}