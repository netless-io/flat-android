package io.agora.flat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.agora.flat.di.interfaces.StartupInitializer
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    companion object {
        val TAG = "MainApplication"
    }

    @Inject
    lateinit var initializerSet: Set<@JvmSuppressWildcards StartupInitializer>

    override fun onCreate() {
        super.onCreate()
        initializerSet.forEach {
            it.onCreate(this)
        }
    }
}