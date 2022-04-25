package io.agora.flat.common

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.agora.flat.Constants
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.ui.activity.LoginActivity
import io.agora.flat.ui.activity.cloud.preview.PreviewActivity
import io.agora.flat.ui.activity.dev.DevSettingsActivity
import io.agora.flat.ui.activity.dev.DevToolsActivity
import io.agora.flat.ui.activity.home.MainActivity
import io.agora.flat.ui.activity.phone.PhoneBindActivity
import io.agora.flat.ui.activity.play.ClassRoomActivity
import io.agora.flat.ui.activity.playback.ReplayActivity
import io.agora.flat.ui.activity.setting.*

object Navigator {
    fun launchHomeActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    // join room
    fun launchHomeActivity(context: Context, roomUUID: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Constants.IntentKey.ROOM_UUID, roomUUID)
        }
        context.startActivity(intent)
    }

    fun launchLoginActivity(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
    }

    fun launchPlaybackActivity(context: Context, roomUUID: String) {
        val intent = Intent(context, ReplayActivity::class.java).apply {
            putExtra(Constants.IntentKey.ROOM_UUID, roomUUID)
        }
        context.startActivity(intent)
    }

    fun launchRoomPlayActivity(
        context: Context,
        roomUUID: String,
        periodicUUID: String? = null,
        quickStart: Boolean = false,
    ) {
        val intent = Intent(context, ClassRoomActivity::class.java).apply {
            putExtra(Constants.IntentKey.ROOM_UUID, roomUUID)
            putExtra(Constants.IntentKey.PERIODIC_UUID, periodicUUID)
            putExtra(Constants.IntentKey.ROOM_QUICK_START, quickStart)
        }
        context.startActivity(intent)
    }

    fun launchRoomPlayActivity(context: Context, roomPlayInfo: RoomPlayInfo) {
        val intent = Intent(context, ClassRoomActivity::class.java).apply {
            putExtra(Constants.IntentKey.ROOM_UUID, roomPlayInfo.roomUUID)
            putExtra(Constants.IntentKey.ROOM_PLAY_INFO, roomPlayInfo)
        }
        context.startActivity(intent)
    }

    fun launchDevToolsActivity(context: Context) {
        val intent = Intent(context, DevToolsActivity::class.java)
        context.startActivity(intent)
    }

    fun launchDevSettingsActivity(context: Context) {
        val intent = Intent(context, DevSettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun launchFeedbackActivity(context: Context) {
        val intent = Intent(context, FeedbackActivity::class.java)
        context.startActivity(intent)
    }

    fun launchUserInfoActivity(context: Context) {
        val intent = Intent(context, UserInfoActivity::class.java)
        context.startActivity(intent)
    }

    fun launchAboutUsActivity(context: Context) {
        val intent = Intent(context, AboutUsActivity::class.java)
        context.startActivity(intent)
    }

    fun launchLanguageActivity(context: Context) {
        val intent = Intent(context, LanguageActivity::class.java)
        context.startActivity(intent)
    }

    fun launchDarkModeActivity(context: Context) {
        val intent = Intent(context, DarkModeActivity::class.java)
        context.startActivity(intent)
    }

    fun launchEditNameActivity(context: Context) {
        val intent = Intent(context, EditNameActivity::class.java)
        context.startActivity(intent)
    }

    fun launchWebViewActivity(context: Context, url: String) {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(Constants.IntentKey.URL, url)
        }
        context.startActivity(intent)
    }

    fun launchPreviewActivity(context: Context, file: CloudStorageFile) {
        val intent = Intent(context, PreviewActivity::class.java).apply {
            putExtra(Constants.IntentKey.CLOUD_FILE, file)
        }
        context.startActivity(intent)
    }

    fun launchPhoneBindActivity(context: Context) {
        val intent = Intent(context, PhoneBindActivity::class.java)
        context.startActivity(intent)
    }

    // system
    fun gotoNetworkSetting(context: Context) {
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }
}