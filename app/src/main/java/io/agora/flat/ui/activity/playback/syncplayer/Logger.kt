package io.agora.flat.ui.activity.playback.syncplayer

import android.util.Log

class Logger {
    companion object {
        private const val TAG = "SyncPlayer"

        fun d(msg: String) {
            Log.d(TAG, msg)
        }

        fun e(msg: String) {
            Log.e(TAG, msg)
        }

        fun e(msg: String, throwable: Throwable) {
            Log.e(TAG, msg, throwable)
        }
    }
}