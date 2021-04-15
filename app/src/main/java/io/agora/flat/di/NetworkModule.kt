package io.agora.flat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.agora.flat.http.HeaderProvider
import io.agora.flat.http.interceptor.HeaderInterceptor
import io.agora.flat.http.interceptor.OtherInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class NormalOkHttpClient

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OtherInterceptorOkHttpClient

    @NormalOkHttpClient
    @Provides
    fun provideOkHttpClient(headerProviders: Set<@JvmSuppressWildcards HeaderProvider>): OkHttpClient {
        // TODO workaround inject headerProviders
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor(HeaderInterceptor(headerProviders))
            .build()
    }

    @OtherInterceptorOkHttpClient
    @Provides
    fun provideOtherInterceptorOkHttpClient(
        otherInterceptor: OtherInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(otherInterceptor)
            .build()
    }
}