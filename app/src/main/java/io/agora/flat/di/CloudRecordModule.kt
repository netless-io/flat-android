package io.agora.flat.di;

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.agora.flat.data.AppEnv
import io.agora.flat.data.api.CloudRecordService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudRecordModule {

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
}
