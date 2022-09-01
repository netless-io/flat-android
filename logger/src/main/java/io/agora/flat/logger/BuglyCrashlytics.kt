package io.agora.flat.logger

import android.content.Context
import com.tencent.bugly.CrashModule
import com.tencent.bugly.crashreport.BuglyLog
import com.tencent.bugly.crashreport.CrashReport
import io.agora.flat.di.interfaces.Crashlytics

/**
 * Initialize the Bugly SDK after the user has authorized the Privacy Policy
 */
internal class BuglyCrashlytics : Crashlytics {
    var uid: String? = null

    override fun init(context: Context) {
        CrashReport.initCrashReport(context, "57ff6f9227", false)
        uid?.let {
            setUserId(it)
        }
    }

    override fun setUserId(id: String) {
        if (CrashModule.getInstance().hasInitialized()) {
            CrashReport.setUserId(id)
        } else {
            this.uid = id
        }
    }

    override fun log(tag: String?, message: String, t: Throwable?) {
        if (CrashModule.getInstance().hasInitialized() && t != null) {
            CrashReport.postCatchedException(t)
        }
    }
}