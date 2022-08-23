package io.agora.flat.logger

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.agora.flat.di.interfaces.*
import javax.inject.Singleton

@Module
object LoggerModule {
    @Singleton
    @Provides
    fun providerLogReporter(logConfig: LogConfig): LogReporter = AliyunLogReporter(logConfig)

    @Singleton
    @Provides
    fun providerCrashlytics(): Crashlytics = BuglyCrashlytics()

    @Singleton
    @Provides
    fun providerLogger(
        crashlytics: Crashlytics,
        logReporter: LogReporter
    ): Logger = FlatLogger(crashlytics, logReporter)
}

@Module
abstract class LoggerModuleBinds {
    @Binds
    @IntoSet
    abstract fun providerTimberInitializer(bind: TimberInitializer): StartupInitializer
}