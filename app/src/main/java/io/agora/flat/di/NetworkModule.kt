package io.agora.flat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.agora.flat.data.AppEnv
import io.agora.flat.http.HeaderProvider
import io.agora.flat.http.api.*
import io.agora.flat.http.interceptor.AgoraMessageInterceptor
import io.agora.flat.http.interceptor.HeaderInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class NormalOkHttpClient

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AgoraMessageOkHttpClient

    @NormalOkHttpClient
    @Provides
    fun provideOkHttpClient(headerProviders: Set<@JvmSuppressWildcards HeaderProvider>): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor(HeaderInterceptor(headerProviders))
            .build()
    }

    @AgoraMessageOkHttpClient
    @Provides
    fun provideAgoraMessageOkHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor(AgoraMessageInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRoomService(@NetworkModule.NormalOkHttpClient client: OkHttpClient, appEnv: AppEnv): RoomService {
        return Retrofit.Builder()
            .baseUrl(appEnv.flatServiceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoomService::class.java)
    }

    @Provides
    @Singleton
    fun provideCloudStorageService(
        @NetworkModule.NormalOkHttpClient client: OkHttpClient,
        appEnv: AppEnv,
    ): CloudStorageService {
        return Retrofit.Builder()
            .baseUrl(appEnv.flatServiceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudStorageService::class.java)
    }

    @Provides
    @Singleton
    fun provideCloudRecordService(
        @NetworkModule.NormalOkHttpClient client: OkHttpClient,
        appEnv: AppEnv,
    ): CloudRecordService {
        return Retrofit.Builder()
            .baseUrl(appEnv.flatServiceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudRecordService::class.java)
    }

    @Provides
    @Singleton
    fun provideMiscService(@NetworkModule.NormalOkHttpClient client: OkHttpClient, appEnv: AppEnv): MiscService {
        return Retrofit.Builder()
            .baseUrl(appEnv.flatServiceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MiscService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageService(
        @NetworkModule.AgoraMessageOkHttpClient client: OkHttpClient,
    ): MessageService {
        return Retrofit.Builder()
            .baseUrl("https://api.agora.io/dev/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MessageService::class.java)
    }
}