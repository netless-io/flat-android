package io.agora.flat.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.data.AppDataCenter
import io.agora.flat.di.impl.RtcProviderImpl
import io.agora.flat.di.impl.RtmProviderImpl
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.di.interfaces.StartupInitializer
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 全局
 */
@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RemoteUserDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class LocalUserDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class GlobalData

    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @GlobalData
    @Singleton
    @Provides
    fun provideAppDataCenter(@ApplicationContext context: Context): AppDataCenter {
        return AppDataCenter(context)
    }

    @Singleton
    @Provides
    fun providerRtcProvider(): RtcEngineProvider {
        return RtcProviderImpl()
    }

    @Singleton
    @Provides
    fun providerRtmProvider(): RtmEngineProvider {
        return RtmProviderImpl()
    }

    /**
     * StartupInitializer Set
     */
    @Provides
    @IntoSet
    fun provideRtcInitializer(rtcEngineProvider: RtcEngineProvider): StartupInitializer {
        return rtcEngineProvider as StartupInitializer;
    }

    @Provides
    @IntoSet
    fun provideRtmInitializer(rtmEngineProvider: RtmEngineProvider): StartupInitializer {
        return rtmEngineProvider as StartupInitializer;
    }
}