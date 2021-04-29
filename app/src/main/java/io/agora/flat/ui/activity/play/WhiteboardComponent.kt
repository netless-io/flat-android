package io.agora.flat.ui.activity.play

import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.annotation.UiThread
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
import io.agora.flat.ui.animator.SimpleAnimator
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.dp2px
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class WhiteboardComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = WhiteboardComponent::class.simpleName
    }

    private lateinit var binding: ComponentWhiteboardBinding

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
            binding.reset to { room?.scalePptToFit() },
            binding.showScenes to { previewSlide() },


            binding.tools to {
                binding.toolsLayout.apply {
                    visibility = if (visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
            },
            binding.fileUpload to {
                binding.toolsLayout.visibility = View.GONE
            },
            binding.clear to {
                binding.toolsLayout.visibility = View.GONE

                room?.cleanScene(true)
            },
            binding.hand to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_hand_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.HAND
                }
            },
            binding.clicker to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_clicker_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = "clicker"
                }
            },
            binding.text to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_text_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.TEXT
                }
            },
            binding.selector to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_selector_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.SELECTOR
                }
            },
            binding.eraser to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_eraser_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.ERASER
                }
            },
            binding.pencil to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_pencil_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.PENCIL
                }
            },
            binding.laser to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_laser_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.LASER_POINTER
                }
            },
            binding.rectangle to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_rectangle_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.RECTANGLE
                }
            },
            binding.arrow to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_arrow_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.ARROW
                }
            },
            binding.circle to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_circle_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.ELLIPSE
                }
            },
            binding.line to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_line_selected)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.STRAIGHT
                }
            },

            binding.toolsSub to {
                binding.toolsSubLayout.apply {
                    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
            },

            // slide
            binding.scenePreview to {

            },
            binding.sceneAdd to {
                addSlideToNext()
            },
            binding.sceneDelete to {
                deleteCurrentSlide()
            },
            binding.sceneCover to {
                slideAnimator.hide()
            }
        )

        map.forEach { (view, action) -> view.setOnClickListener { action(it) } }

        colorAdapter = ColorAdapter(ColorItem.colors)
        colorAdapter.setOnItemClickListener(object : ColorAdapter.OnItemClickListener {
            override fun onColorSelected(item: ColorItem) {
                binding.toolsSubLayout.visibility = View.GONE
                room?.memberState = room?.memberState?.apply {
                    strokeColor = item.color
                }
                binding.toolsSub.setImageResource(item.drawableRes)
            }
        })
        binding.colorRecyclerView.adapter = colorAdapter
        binding.colorRecyclerView.layoutManager = GridLayoutManager(activity, 4)
        binding.seeker.setOnStrokeChangedListener(object : StrokeSeeker.OnStrokeChangedListener {
            override fun onStroke(width: Int) {
                Log.e("Test", "stroke $width")
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
        binding.sceneRecyclerView.adapter = slideAdapter
        binding.sceneRecyclerView.layoutManager =
            LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        slideAnimator = SimpleAnimator(
            onUpdate = ::updateSlide,
            onShowStart = {
                binding.scenePreviewLayout.visibility = View.VISIBLE
            },
            onHideEnd = {
                binding.scenePreviewLayout.visibility = View.GONE
            }
        )
    }

    private val previewWidth = activity.dp2px(128)

    private fun updateSlide(value: Float) {
        val layoutParams = binding.scenePreview.layoutParams
        layoutParams.height = (previewWidth * value).toInt()
        binding.scenePreview.layoutParams = layoutParams

        binding.sceneCover.alpha = value
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
            viewModel.roomPlayInfo.collect {
                it?.apply {
                    join(whiteboardRoomUUID, whiteboardRoomToken)
                }
            }
        }
    }

    private fun initWhiteboard() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        val configuration = WhiteSdkConfiguration(Constants.NETLESS_APP_IDENTIFIER, true)
        configuration.isUserCursor = true

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
            room.disableSerialization(false)
            // On Room Ready
            room.getRoomState(object : Promise<RoomState> {
                override fun then(roomState: RoomState) {
                    onInitRoomState(roomState)
                }

                override fun catchEx(t: SDKError?) {
                }
            })
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
        val applianceMap = mapOf(
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
            "clicker" to R.drawable.ic_toolbox_clicker_selected,
        )
        applianceMap[memberState.currentApplianceName]?.let {
            binding.tools.setImageResource(it)
        } ?: binding.tools.setImageDrawable(null)

        memberState.apply {
            binding.seeker.setStrokeWidth(strokeWidth.toInt())
            val item = ColorItem.colors.find {
                it.color.contentEquals(strokeColor)
            } ?: ColorItem.colors[0]
            colorAdapter.setCurrentColor(item.color)
            binding.toolsSub.setImageResource(item.drawableRes)
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
        whiteSdk.joinRoom(RoomParams(roomUUID, roomToken), roomListener, joinRoomCallback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        whiteSdk.releaseRoom()
    }
}