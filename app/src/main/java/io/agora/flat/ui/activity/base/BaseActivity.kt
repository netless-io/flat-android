package io.agora.flat.ui.activity.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.util.isPhoneMode

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockOrientation()
    }

    protected open fun lockOrientation() {
        if (isPhoneMode()) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
}