package io.agora.flat

import android.app.Application
import android.util.Log
import android.webkit.WebView
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.herewhite.sdk.WhiteboardView
import dagger.hilt.android.HiltAndroidApp
import io.agora.flat.common.android.CallingCodeManager
import io.agora.flat.common.android.DarkModeManager
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.common.android.ProtocolUrlManager
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.di.GlobalInstanceProvider
import io.agora.flat.util.isApkInDebug

@HiltAndroidApp
class MainApplication : Application(), CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
        UploadManager.init(this)
        LanguageManager.init(this)
        DarkModeManager.init(this)
        CallingCodeManager.init(this)
        GlobalInstanceProvider.init(this)
        ProtocolUrlManager.init(this)
        WebView.setWebContentsDebuggingEnabled(true)
        WhiteboardView.setEntryUrl("file:///android_asset/flatboard/index.html")
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder
            .fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
    }
}