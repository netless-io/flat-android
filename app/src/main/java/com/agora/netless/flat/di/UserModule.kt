package com.agora.netless.flat.di;

import com.agora.netless.flat.Constants
import com.agora.netless.flat.data.api.UserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    @Provides
    @Singleton
    fun provideUserService(@NormalOkHttpClient client: OkHttpClient): UserService {
        return Retrofit.Builder()
            .baseUrl(Constants.FLAT_SERVICE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserService::class.java)
    }

    @Provides
    fun provideUserToken(): String {
        return "Token";
    }
}
