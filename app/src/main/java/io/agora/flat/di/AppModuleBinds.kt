package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.common.rtc.RtcApiImpl
import io.agora.flat.common.rtm.RtmApiImpl
import io.agora.flat.di.interfaces.*
import io.agora.flat.logger.LoggerModuleBinds

/**
 * 全局
 */
@InstallIn(SingletonComponent::class)
@Module(includes = [LoggerModuleBinds::class])
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
}