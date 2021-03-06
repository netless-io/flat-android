package io.agora.flat.common

import android.content.Context
import android.content.Intent
import io.agora.flat.Constants
import io.agora.flat.ui.activity.LoginActivity
import io.agora.flat.ui.activity.dev.DevToolsActivity
import io.agora.flat.ui.activity.home.MainActivity
import io.agora.flat.ui.activity.play.ClassRoomActivity
import io.agora.flat.ui.activity.playback.PlaybackActivity
import io.agora.flat.ui.activity.room.CreateRoomActivity
import io.agora.flat.ui.activity.room.JoinRoomActivity
import io.agora.flat.ui.activity.room.RoomDetailActivity
import io.agora.flat.ui.activity.room.SubscribeRoomActivity
import io.agora.flat.ui.activity.setting.AboutUsActivity
import io.agora.flat.ui.activity.setting.FeedbackActivity
import io.agora.flat.ui.activity.setting.SettingActivity
import io.agora.flat.ui.activity.setting.UserInfoActivity
import io.agora.flat.ui.activity.setting.MyProfileActivity

object Navigator {
    fun launchHomeActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    fun launchLoginActivity(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
    }

    fun launchRoomDetailActivity(context: Context, roomUUID: String, periodicUUID: String? = null) {
        val intent = Intent(context, RoomDetailActivity::class.java).apply {
            putExtra(Constants.IntentKey.ROOM_UUID, roomUUID)
            putExtra(Constants.IntentKey.PERIODIC_UUID, periodicUUID)
        }
        context.startActivity(intent)
    }

    fun launchPlaybackActivity(context: Context, roomUUID: String) {
        val intent = Intent(context, PlaybackActivity::class.java)
        context.startActivity(intent)
    }

    fun launchRoomPlayActivity(context: Context, roomUUID: String, periodicUUID: String? = null) {
        val intent = Intent(context, ClassRoomActivity::class.java).apply {
            putExtra(Constants.IntentKey.ROOM_UUID, roomUUID)
            putExtra(Constants.IntentKey.PERIODIC_UUID, periodicUUID)
        }
        context.startActivity(intent)
    }

    fun launchSettingActivity(context: Context) {
        val intent = Intent(context, SettingActivity::class.java)
        context.startActivity(intent)
    }

    fun launchDevToolsActivity(context: Context) {
        val intent = Intent(context, DevToolsActivity::class.java)
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

    fun launchMyProfileActivity(context: Context) {
        val intent = Intent(context, MyProfileActivity::class.java)
        context.startActivity(intent)
    }

    fun launchAboutUsActivity(context: Context) {
        val intent = Intent(context, AboutUsActivity::class.java)
        context.startActivity(intent)
    }

    fun launchJoinRoomActivity(context: Context) {
        val intent = Intent(context, JoinRoomActivity::class.java)
        context.startActivity(intent)
    }

    fun launchCreateRoomActivity(context: Context) {
        val intent = Intent(context, CreateRoomActivity::class.java)
        context.startActivity(intent)
    }

    fun launchSubscribeRoomActivity(context: Context) {
        val intent = Intent(context, SubscribeRoomActivity::class.java)
        context.startActivity(intent)
    }
}