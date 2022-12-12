package io.agora.flat.logger

import android.content.Context
import android.os.Build
import com.tencent.bugly.CrashModule
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import io.agora.flat.di.interfaces.Crashlytics
import io.agora.flat.di.interfaces.StartupInitializer
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Initialize the Bugly SDK after the user has authorized the Privacy Policy
 */
@Singleton
class BuglyCrashlytics @Inject constructor() : Crashlytics, StartupInitializer {
    var uid: String? = null

    override fun init(context: Context) {
        val strategy = UserStrategy(context)
        strategy.deviceModel = getDeviceModel()
        CrashReport.initCrashReport(context, "57ff6f9227", false, strategy)
        uid?.let {
            setUserId(it)
        }
    }

    private fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model + ""
        } else "$manufacturer $model"
    }

    override fun setUserId(id: String) {
        if (CrashModule.getInstance().hasInitialized()) {
            CrashReport.setUserId(id)
        } else {
            this.uid = id
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) {
            CrashReport.postCatchedException(t)
        }
    }
}