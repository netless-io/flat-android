package io.agora.flat.di.interfaces

import android.content.Context

interface StartupInitializer {
    fun onCreate(context: Context)

    fun onTerminate() {

    }
}