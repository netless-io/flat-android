package io.agora.flat.util

import android.util.Log

object ClassroomTrace {
    enum class Step {
        JoinBoardSuccess,
        JoinRtmSuccess,
        FetchRtmMembers,
        JoinRtcSuccess,
        ConnectSyncedStore,
    }

    const val TAG = "ClassroomTrace"

    fun trace(step: String) {
        Log.i(TAG, "trace called, step $step")
    }
}