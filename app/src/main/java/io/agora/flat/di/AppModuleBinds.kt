package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.di.impl.RtcProviderImpl
import io.agora.flat.di.impl.RtmProviderImpl
import io.agora.flat.di.interfaces.*

/**
 * 全局
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBinds {
    @Binds
    @IntoSet
    abstract fun provideRtcInitializer(bind: RtcProviderImpl): StartupInitializer

    @Binds
    @IntoSet
    abstract fun provideRtmInitializer(bind: RtmProviderImpl): StartupInitializer

    @Binds
    @IntoSet
    abstract fun providerNetworkObserverInitializer(bind: FlatNetworkObserver): StartupInitializer

    @Binds
    abstract fun providerRtcProvider(bind: RtcProviderImpl): RtcEngineProvider

    @Binds
    abstract fun providerRtmProvider(bind: RtmProviderImpl): RtmEngineProvider

    @Binds
    abstract fun providerNetworkObserver(bind: FlatNetworkObserver): NetworkObserver
}