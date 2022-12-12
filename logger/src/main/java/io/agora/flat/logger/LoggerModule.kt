package io.agora.flat.logger

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.agora.flat.di.interfaces.Crashlytics
import io.agora.flat.di.interfaces.LogReporter
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.StartupInitializer
import javax.inject.Singleton

@Module
object LoggerModule {
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
    abstract fun providerCrashlyticsInitializer(bind: BuglyCrashlytics): StartupInitializer

    @Binds
    @IntoSet
    abstract fun providerLogReporterInitializer(bind: AliyunLogReporter): StartupInitializer

    @Binds
    @IntoSet
    abstract fun providerTimberInitializer(bind: TimberInitializer): StartupInitializer

    @Binds
    abstract fun providerCrashlytics(bind: BuglyCrashlytics): Crashlytics

    @Binds
    abstract fun providerLogReporter(bind: AliyunLogReporter): LogReporter
}