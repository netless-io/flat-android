package io.agora.flat.di;

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.agora.flat.data.AppEnv
import io.agora.flat.data.api.CloudStorageService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudStorageModule {

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
}
