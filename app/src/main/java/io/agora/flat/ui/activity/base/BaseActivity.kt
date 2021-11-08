package io.agora.flat.ui.activity.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import io.agora.flat.common.android.LanguageManager

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.onAttach(newBase))
    }
}