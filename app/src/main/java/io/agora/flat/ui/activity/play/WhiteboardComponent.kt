package io.agora.flat.ui.activity.play

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomPhase
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import io.agora.flat.Constants
import org.json.JSONObject


class WhiteboardComponent(
    val activity: ClassRoomActivity,
    val whiteboard: WhiteboardView,
) : LifecycleOwner {
    companion object {
        val TAG = WhiteboardComponent::class.simpleName
    }

    private var whiteSdk: WhiteSdk
    private var room: Room? = null

    init {
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

    fun join(roomUUID: String, roomToken: String) {
        whiteSdk.joinRoom(RoomParams(roomUUID, roomToken), roomListener, joinRoomCallback)
    }

    fun onActivityDestroy() {
        whiteSdk.releaseRoom()
    }

    override fun getLifecycle(): Lifecycle {
        return activity.lifecycle
    }
}