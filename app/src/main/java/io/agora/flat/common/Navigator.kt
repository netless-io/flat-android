package io.agora.flat.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.agora.flat.Constants
import io.agora.flat.data.model.CloudFile
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.ui.activity.CallingCodeActivity
import io.agora.flat.ui.activity.LoginActivity
import io.agora.flat.ui.activity.bind.EmailBindActivity
import io.agora.flat.ui.activity.cloud.preview.PreviewActivity
import io.agora.flat.ui.activity.dev.DevSettingsActivity
import io.agora.flat.ui.activity.dev.DevToolsActivity
import io.agora.flat.ui.activity.home.MainActivity
import io.agora.flat.ui.activity.password.PasswordChangeActivity
import io.agora.flat.ui.activity.password.PasswordResetActivity
import io.agora.flat.ui.activity.password.PasswordSetActivity
import io.agora.flat.ui.activity.phone.PhoneBindActivity
import io.agora.flat.ui.activity.play.ClassRoomActivity
import io.agora.flat.ui.activity.playback.ReplayActivity
import io.agora.flat.ui.activity.register.RegisterActivity
import io.agora.flat.ui.activity.register.RegisterProfileActivity
import io.agora.flat.ui.activity.setting.AboutUsActivity
import io.agora.flat.ui.activity.setting.AccountSecurityActivity
import io.agora.flat.ui.activity.setting.DarkModeActivity
import io.agora.flat.ui.activity.setting.EditNameActivity
import io.agora.flat.ui.activity.setting.FeedbackActivity
import io.agora.flat.ui.activity.setting.LanguageActivity
import io.agora.flat.ui.activity.setting.UserInfoActivity
import io.agora.flat.ui.activity.setting.WebViewActivity

object Navigator {
    fun launchHomeActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    fun launchHomeActivity(context: Context, intent: Intent) {
        val newIntent = Intent(intent).apply {
            component = ComponentName(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(newIntent)
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

    fun launchAccountSecurityActivity(context: Context) {
        val intent = Intent(context, AccountSecurityActivity::class.java)
        context.startActivity(intent)
    }

    fun launchWebViewActivity(context: Context, url: String) {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra(Constants.IntentKey.URL, url)
        }
        context.startActivity(intent)
    }

    fun launchPreviewActivity(context: Context, file: CloudFile) {
        val intent = Intent(context, PreviewActivity::class.java).apply {
            putExtra(Constants.IntentKey.CLOUD_FILE, file)
        }
        context.startActivity(intent)
    }

    fun launchPhoneBindActivity(context: Context, from: String = Constants.From.Login) {
        val intent = Intent(context, PhoneBindActivity::class.java)
        intent.putExtra(Constants.IntentKey.FROM, from)
        context.startActivity(intent)
    }

    fun launchEmailBindActivity(context: Context) {
        val intent = Intent(context, EmailBindActivity::class.java)
        context.startActivity(intent)
    }

    // system
    fun gotoNetworkSetting(context: Context) {
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun launchCallingCodeActivity(context: Context) {
        val intent = Intent(context, CallingCodeActivity::class.java)
        context.startActivity(intent)
    }

    fun launchRegisterActivity(loginActivity: LoginActivity) {
        val intent = Intent(loginActivity, RegisterActivity::class.java)
        loginActivity.startActivity(intent)
    }

    fun launchForgotPwdActivity(loginActivity: LoginActivity) {
        val intent = Intent(loginActivity, PasswordResetActivity::class.java)
        loginActivity.startActivity(intent)
    }

    fun launchPasswordChangeActivity(context: Context) {
        val intent = Intent(context, PasswordChangeActivity::class.java)
        context.startActivity(intent)
    }

    fun launchPasswordSetActivity(context: Context) {
        val intent = Intent(context, PasswordSetActivity::class.java)
        context.startActivity(intent)
    }

    fun launchRegisterProfile(context: Context) {
        val intent = Intent(context, RegisterProfileActivity::class.java)
        context.startActivity(intent)
    }
}