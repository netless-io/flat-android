package io.agora.flat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.http.HeaderProvider
import io.agora.flat.http.api.UserService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {

    @Provides
    @Singleton
    fun provideUserService(@NetworkModule.NormalOkHttpClient client: OkHttpClient, appEnv: AppEnv): UserService {
        return Retrofit.Builder()
            .baseUrl(appEnv.flatServiceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserService::class.java)
    }

    @Provides
    @IntoSet
    fun provideUserHeaderProvider(appKVCenter: AppKVCenter): HeaderProvider {
        return object : HeaderProvider {
            override fun getHeaders(): Set<Pair<String, String>> {
                return appKVCenter.getToken()?.let {
                    setOf(
                        "Authorization" to String.format("Bearer %s", it),
                        "x-session-id" to appKVCenter.getSessionId(),
                        "x-request-id" to UUID.randomUUID().toString()
                    )
                } ?: emptySet()
            }
        }
    }
}
