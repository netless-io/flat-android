package link.netless.flat.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import link.netless.flat.data.AppDataCenter
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
}