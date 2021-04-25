package io.agora.flat.ui.activity.play

import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.di.interfaces.RtmEngineProvider

abstract class BaseComponent(val activity: ClassRoomActivity, val rootView: FrameLayout) :
    LifecycleOwner, DefaultLifecycleObserver {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ComponentEntryPoint {
        fun rtmApi(): RtmEngineProvider
        fun rtcApi(): RtcEngineProvider
    }

    override fun getLifecycle(): Lifecycle {
        return activity.lifecycle
    }
}