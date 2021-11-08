package io.agora.flat.ui.activity.base

import android.content.Context
import androidx.activity.ComponentActivity
import io.agora.flat.common.android.LanguageManager

open class BaseComposeActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.onAttach(newBase))
    }
}