package io.agora.flat.ui.activity.play

import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomPhase
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import io.agora.flat.Constants
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

    private val viewModel: ClassRoomViewModel by activity.viewModels()
    private lateinit var whiteboard: WhiteboardView
    private lateinit var whiteSdk: WhiteSdk
    private var room: Room? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        initWhiteboard()
        loadData()
    }

    private fun initView() {
        whiteboard = WhiteboardView(activity)
        rootView.addView(whiteboard, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
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
        whiteSdk = WhiteSdk(whiteboard, activity, configuration)
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
        override fun onPhaseChanged(phase: RoomPhase?) {
            Log.d(TAG, "onPhaseChanged:${phase?.name}")
        }

        override fun onDisconnectWithError(e: Exception?) {
            Log.d(TAG, "onDisconnectWithError:${e?.message}")
        }

        override fun onKickedWithReason(reason: String?) {
            Log.d(TAG, "onKickedWithReason:${reason}")
        }

        override fun onRoomStateChanged(modifyState: RoomState?) {
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
        }

        override fun catchEx(t: SDKError) {

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