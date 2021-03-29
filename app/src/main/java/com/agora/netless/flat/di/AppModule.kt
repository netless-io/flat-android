package com.agora.netless.flat.di

import android.content.Context
import android.content.SharedPreferences
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

    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @GlobalData
    @Singleton
    @Provides
    fun provideAppGlobalData(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("global_kv_data", Context.MODE_PRIVATE)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object UserRepositoryModule {

        @Singleton
        @Provides
        fun provideUserRepository(
            userService: UserService,
            @GlobalData globalData: SharedPreferences
        ): UserRepository {
            return UserRepository(userService, globalData)
        }
    }
}