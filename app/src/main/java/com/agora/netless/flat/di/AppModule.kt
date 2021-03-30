package com.agora.netless.flat.di

import android.content.Context
import com.agora.netless.flat.data.AppDataCenter
import com.agora.netless.flat.data.api.UserService
import com.agora.netless.flat.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

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

    @Module
    @InstallIn(SingletonComponent::class)
    object UserRepositoryModule {

        @Provides
        @Singleton
        fun provideUserRepository(
            userService: UserService,
            @GlobalData appDataCenter: AppDataCenter
        ): UserRepository {
            return UserRepository(userService, appDataCenter)
        }
    }
}