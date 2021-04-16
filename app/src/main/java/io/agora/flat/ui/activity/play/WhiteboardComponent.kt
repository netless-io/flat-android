package io.agora.flat.ui.activity.play

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.databinding.ComponentWhiteboardBinding
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject

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
            binding.pagePreview to { room?.pptPreviousStep() },
            binding.pageNext to { room?.pptNextStep() },

            binding.tools to { binding.toolsLayout.visibility = View.VISIBLE },
            binding.fileUpload to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_file_upload)
            },
            binding.clear to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_clear)

                room?.cleanScene(true)
            },
            binding.clicker to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_clicker)

                room?.memberState = MemberState().apply {
                    currentApplianceName = "clicker"
                }
            },
            binding.text to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_text)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.TEXT
                }
            },
            binding.selector to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_selector)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.SELECTOR
                }
            },
            binding.eraser to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_eraser)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.ERASER
                }
            },
            binding.pencil to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_pencil)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.PENCIL
                }
            },
            binding.laser to {
                binding.toolsLayout.visibility = View.GONE
                binding.tools.setImageResource(R.drawable.ic_toolbox_laser)

                room?.memberState = MemberState().apply {
                    currentApplianceName = Appliance.LASER_POINTER
                }
            },
        )

        map.forEach { (view, action) -> view.setOnClickListener { action(it) } }
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
        }

        override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
            Log.d(TAG, "onCanUndoStepsUpdate:${canUndoSteps}")
        }

        override fun onCanRedoStepsUpdate(canRedoSteps: Long) {
            Log.d(TAG, "onCanRedoStepsUpdate:${canRedoSteps}")
        }

        override fun onCatchErrorWhenAppendFrame(userId: Long, error: Exception?) {
            Log.d(TAG, "onCatchErrorWhenAppendFrame:${error}")
        }
    }

    private var joinRoomCallback = object : Promise<Room> {
        override fun then(room: Room) {
            this@WhiteboardComponent.room = room
            room.disableSerialization(false)
        }

        override fun catchEx(t: SDKError) {
            // showError Dialog & restart activity
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