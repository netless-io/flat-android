package io.agora.flat.ui.activity.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.util.isPhoneMode

open class BaseComposeActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockOrientation()
    }

    open fun lockOrientation() {
        if (isPhoneMode()) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}