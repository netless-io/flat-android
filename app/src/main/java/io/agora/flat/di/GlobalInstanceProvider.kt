package io.agora.flat.di

import android.app.Application
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.event.EventBus


object GlobalInstanceProvider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun appEnv(): AppEnv
        fun appKvCenter(): AppKVCenter
        fun eventBus(): EventBus
    }

    private lateinit var context: Application

    fun init(application: Application) {
        context = application
    }

    @JvmStatic
    fun getAppKvCenter(): AppKVCenter {
        return entryPoint().appKvCenter()
    }

    @JvmStatic
    fun getAppEnv(): AppEnv {
        return entryPoint().appEnv()
    }

    @JvmStatic
    private fun entryPoint(): AppEntryPoint = EntryPointAccessors.fromApplication(
        context,
        AppEntryPoint::class.java
    )
}