package io.agora.flat.ui.activity.play

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.AppModule
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.ui.viewmodel.UserQuery

abstract class BaseComponent(
    val activity: AppCompatActivity,
    val rootView: FrameLayout,
) : LifecycleOwner, DefaultLifecycleObserver {

    override fun getLifecycle(): Lifecycle {
        return activity.lifecycle
    }
}