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
import com.agora.netless.simpleui.StrokeSeeker
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.flat.BuildConfig
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.databinding.LayoutScenePreviewBinding
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.view.PaddingItemDecoration
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

    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private lateinit var whiteSdk: WhiteSdk
    private var room: Room? = null
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var slideAdapter: SceneAdapter
    private lateinit var slideAnimator: SimpleAnimator

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        initWhiteboard()
        loadData()
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
                binding.toolsLayout.apply { isVisible = !isVisible }

                viewModel.notifyOperatingAreaShown(ClassRoomEvent.AREA_ID_APPLIANCE)
            },
            binding.clear to {
                binding.toolsLayout.visibility = View.GONE

                room?.cleanScene(true)
            },
            binding.hand to { onSelectAppliance(Appliance.HAND) },
            binding.clicker to { onSelectAppliance(Appliance.CLICKER) },
            binding.text to { onSelectAppliance(Appliance.TEXT) },
            binding.selector to { onSelectAppliance(Appliance.SELECTOR) },
            binding.eraser to { onSelectAppliance(Appliance.ERASER) },
            binding.pencil to { onSelectAppliance(Appliance.PENCIL) },
            binding.laser to { onSelectAppliance(Appliance.LASER_POINTER) },
            binding.rectangle to { onSelectAppliance(Appliance.RECTANGLE) },
            binding.arrow to { onSelectAppliance(Appliance.ARROW) },
            binding.circle to { onSelectAppliance(Appliance.ELLIPSE) },
            binding.line to { onSelectAppliance(Appliance.STRAIGHT) },

            binding.toolsSubPaint to {
                binding.toolsSubLayout.apply { isVisible = !isVisible }

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
    }

    private fun onSelectAppliance(appliance: String) {
        binding.toolsLayout.isVisible = false
        updateAppliance(appliance)
        room?.memberState = MemberState().apply {
            currentApplianceName = appliance
        }
    }

    // TODO
    private fun updateAppliance(appliance: String) {
        binding.tools.setImageResource(applianceResource(appliance))

        when (appliance) {
            Appliance.SELECTOR -> {
                binding.toolsSub.isVisible = viewModel.state.value.isWritable
                binding.toolsSubDelete.isVisible = true
                binding.toolsSubPaint.isVisible = false
            }
            Appliance.LASER_POINTER, Appliance.ERASER, Appliance.HAND, Appliance.CLICKER -> {
                binding.toolsSub.isVisible = false
            }
            else -> {
                binding.toolsSub.isVisible = viewModel.state.value.isWritable
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

    // TODO 限制多次点击
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

    private fun loadData() {
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
        room?.setWritable(writable, object : Promise<Boolean> {
            override fun then(result: Boolean) {
                if (result) {
                    room?.disableSerialization(false)
                }
            }

            override fun catchEx(error: SDKError) {
            }
        })

        binding.tools.isVisible = writable
        binding.toolsSub.isVisible = writable
        binding.showScenes.isVisible = writable
        binding.pageIndicateLy.isVisible = writable
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
            activity.runOnUiThread {
                room?.roomState?.sceneState?.let(::onSceneStateChanged)
            }
        }

        override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
            Log.d(TAG, "onCanUndoStepsUpdate:${canUndoSteps}")
            activity.runOnUiThread { onUndoStepsChanged(canUndoSteps) }
        }

        override fun onCanRedoStepsUpdate(canRedoSteps: Long) {
            Log.d(TAG, "onCanRedoStepsUpdate:${canRedoSteps}")
            activity.runOnUiThread { onRedoStepsChanged(canRedoSteps) }
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
            // On Room Ready
            room.getRoomState(object : Promise<RoomState> {
                override fun then(roomState: RoomState) {
                    onInitRoomState(roomState)
                }

                override fun catchEx(t: SDKError?) {
                }
            })
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

    private val applianceMap = mapOf(
        Appliance.PENCIL to R.drawable.ic_toolbox_pencil_selected,
        Appliance.SELECTOR to R.drawable.ic_toolbox_selector_selected,
        Appliance.RECTANGLE to R.drawable.ic_toolbox_rectangle_selected,
        Appliance.ELLIPSE to R.drawable.ic_toolbox_circle_selected,
        Appliance.ERASER to R.drawable.ic_toolbox_eraser_selected,
        Appliance.TEXT to R.drawable.ic_toolbox_text_selected,
        Appliance.STRAIGHT to R.drawable.ic_toolbox_line_selected,
        Appliance.ARROW to R.drawable.ic_toolbox_arrow_selected,
        Appliance.HAND to R.drawable.ic_toolbox_hand_selected,
        Appliance.LASER_POINTER to R.drawable.ic_toolbox_laser_selected,
        Appliance.CLICKER to R.drawable.ic_toolbox_clicker_selected,
    )

    private fun applianceResource(appliance: String): Int {
        return applianceMap[appliance] ?: 0
    }

    private fun onMemberStateChanged(memberState: MemberState) {
        updateAppliance(memberState.currentApplianceName)
        memberState.apply {
            binding.seeker.setStrokeWidth(strokeWidth.toInt())
            var item = ColorItem.colors.find {
                it.color.contentEquals(strokeColor)
            }
            // 设置默认颜色
            if (item == null) {
                item = ColorItem.colors[0]
                room?.memberState = room?.memberState?.apply {
                    strokeColor = item.color
                }
            }
            colorAdapter.setCurrentColor(item.color)
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

    private fun join(roomUUID: String, roomToken: String) {
        val roomParams = RoomParams(roomUUID, roomToken).apply {
            useMultiViews = true

            val styleMap = HashMap<String, String>()
            styleMap["bottom"] = "60px"
            styleMap["right"] = "12px"
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