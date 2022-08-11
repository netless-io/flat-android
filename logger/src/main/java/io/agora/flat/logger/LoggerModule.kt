package io.agora.flat.logger

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.agora.flat.di.interfaces.Crashlytics
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.StartupInitializer
import javax.inject.Singleton

@Module
object LoggerModule {
    @Singleton
    @Provides
    fun providerCrashlytics(): Crashlytics = BuglyCrashlytics()

    @Singleton
    @Provides
    fun providerLogger(crashlytics: Crashlytics): Logger = FlatLogger(crashlytics)
}

@Module
abstract class LoggerModuleBinds {
    @Binds
    @IntoSet
    abstract fun providerTimberInitializer(bind: TimberInitializer): StartupInitializer
}