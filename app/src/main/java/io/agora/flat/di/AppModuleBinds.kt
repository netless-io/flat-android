package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.common.board.WhiteSyncedState
import io.agora.flat.di.impl.RtcApiImpl
import io.agora.flat.di.impl.RtmApiImpl
import io.agora.flat.di.interfaces.*

/**
 * 全局
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBinds {
    @Binds
    @IntoSet
    abstract fun provideRtcInitializer(bind: RtcApiImpl): StartupInitializer

    @Binds
    @IntoSet
    abstract fun provideRtmInitializer(bind: RtmApiImpl): StartupInitializer

    @Binds
    @IntoSet
    abstract fun providerNetworkObserverInitializer(bind: FlatNetworkObserver): StartupInitializer

    @Binds
    abstract fun providerRtcApi(bind: RtcApiImpl): RtcApi

    @Binds
    abstract fun providerRtmApi(bind: RtmApiImpl): RtmApi

    @Binds
    abstract fun providerNetworkObserver(bind: FlatNetworkObserver): NetworkObserver

    @Binds
    abstract fun providerSyncedState(bind: WhiteSyncedState): SyncedClassState
}