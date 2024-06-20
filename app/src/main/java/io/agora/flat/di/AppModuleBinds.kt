package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.common.rtc.AgoraRtc
import io.agora.flat.common.rtm.AgoraRtm
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
    abstract fun provideRtcInitializer(bind: AgoraRtc): StartupInitializer

    @Binds
    @IntoSet
    abstract fun provideRtmInitializer(bind: AgoraRtm): PostLoginInitializer

    @Binds
    @IntoSet
    abstract fun providerNetworkObserverInitializer(bind: FlatNetworkObserver): StartupInitializer

    @Binds
    abstract fun providerRtcApi(bind: AgoraRtc): RtcApi

    @Binds
    abstract fun providerRtmApi(bind: AgoraRtm): RtmApi

    @Binds
    abstract fun providerNetworkObserver(bind: FlatNetworkObserver): NetworkObserver
}