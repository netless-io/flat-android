package io.agora.flat.ui.activity.play

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

abstract class BaseComponent(
    val activity: AppCompatActivity,
    val rootView: FrameLayout,
) : LifecycleOwner, DefaultLifecycleObserver {

    override fun getLifecycle(): Lifecycle {
        return activity.lifecycle
    }
}