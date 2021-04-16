package io.agora.flat.ui.activity.play

import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.agora.flat.MainApplication

abstract class BaseComponent(val activity: ClassRoomActivity, val rootView: FrameLayout) :
    LifecycleOwner, DefaultLifecycleObserver {

    override fun getLifecycle(): Lifecycle {
        return activity.lifecycle
    }

    protected fun application(): MainApplication {
        return activity.application as MainApplication
    }
}