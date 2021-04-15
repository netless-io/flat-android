package io.agora.flat.di;

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.agora.flat.Constants
import io.agora.flat.data.AppDataCenter
import io.agora.flat.data.api.UserService
import io.agora.flat.http.HeaderProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {

    @Provides
    @Singleton
    fun provideUserService(@NetworkModule.NormalOkHttpClient client: OkHttpClient): UserService {
        return Retrofit.Builder()
            .baseUrl(Constants.FLAT_SERVICE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserService::class.java)
    }

    @Provides
    @IntoSet
    fun provideUserHeaderProvider(appDataCenter: AppDataCenter): HeaderProvider {
        return object : HeaderProvider {
            override fun getHeaders(): Set<Pair<String, String>> {
                return appDataCenter.getToken()?.let {
                    setOf("Authorization" to String.format("Bearer %s", it))
                } ?: emptySet()
            }
        }
    }
}
