package io.agora.flat

import android.app.Application
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp
import io.agora.flat.common.android.DarkModeManager
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.util.isApkInDebug

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UploadManager.init(this)
        LanguageManager.init(this)
        DarkModeManager.init(this)
        WebView.setWebContentsDebuggingEnabled(isApkInDebug())
    }
}