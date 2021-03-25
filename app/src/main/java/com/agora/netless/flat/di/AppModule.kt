package com.agora.netless.flat.di

import com.agora.netless.flat.data.api.UserService
import com.agora.netless.flat.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @Module
    @InstallIn(SingletonComponent::class)
    object UserRepositoryModule {

        @Singleton
        @Provides
        fun provideUserRepository(userService: UserService): UserRepository {
            return UserRepository(userService)
        }
    }
}