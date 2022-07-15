package io.agora.flat.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import io.agora.flat.common.login.LoginActivityHandler
import io.agora.flat.common.login.LoginManager
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository

@Module
@InstallIn(ActivityComponent::class)
class ActivityModule {
    @Provides
    fun provideLoginActivityHandler(
        @ActivityContext context: Context,
        loginManager: LoginManager,
        userRepository: UserRepository,
        appKVCenter: AppKVCenter,
        appEnv: AppEnv,
    ): LoginActivityHandler {
        return LoginActivityHandler(context, loginManager, userRepository, appKVCenter, appEnv)
    }
}