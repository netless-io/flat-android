package io.agora.flat.logger

import android.content.Context
import com.tencent.bugly.crashreport.BuglyLog
import com.tencent.bugly.crashreport.CrashReport
import io.agora.flat.di.interfaces.Crashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuglyCrashlytics @Inject constructor() : Crashlytics {
    override fun init(context: Context) {
        CrashReport.initCrashReport(context, "57ff6f9227", false)
    }

    override fun setUserId(id: String) {
        CrashReport.setUserId(id)
    }

    override fun log(tag: String?, message: String, t: Throwable?) {
        BuglyLog.e(tag, message, t)
    }
}