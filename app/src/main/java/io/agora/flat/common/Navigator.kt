package io.agora.flat.common

import android.content.Context
import android.content.Intent
import io.agora.flat.Constants
import io.agora.flat.ui.activity.UserProfileActivity
import io.agora.flat.ui.activity.DevToolsActivity
import io.agora.flat.ui.activity.FeedbackActivity
import io.agora.flat.ui.activity.LoginActivity
import io.agora.flat.ui.activity.SettingActivity
import io.agora.flat.ui.activity.home.HomeActivity
import io.agora.flat.ui.activity.playback.PlaybackActivity
import io.agora.flat.ui.activity.play.ClassRoomActivity
import io.agora.flat.ui.activity.room.CreateRoomActivity
import io.agora.flat.ui.activity.room.JoinRoomActivity
import io.agora.flat.ui.activity.room.RoomDetailActivity

object Navigator {
    fun launchHomeActivity(context: Context) {
        val intent = Intent(context, HomeActivity::class.java).apply {
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

    fun launchUserProfileActivity(context: Context) {
        val intent = Intent(context, UserProfileActivity::class.java)
        context.startActivity(intent)
    }

    fun launchAboutUsActivity(context: Context) {
        val intent = Intent(context, UserProfileActivity::class.java)
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
}