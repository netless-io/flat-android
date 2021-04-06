package link.netless.flat.common

import android.content.Context
import android.content.Intent
import link.netless.flat.ui.activities.UserProfileActivity
import link.netless.flat.ui.activity.DevToolsActivity
import link.netless.flat.ui.activity.FeedbackActivity
import link.netless.flat.ui.activity.LoginActivity
import link.netless.flat.ui.activity.SettingActivity
import link.netless.flat.ui.activity.home.HomeActivity

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
}