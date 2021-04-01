package link.netless.flat.di;

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import link.netless.flat.Constants
import link.netless.flat.data.api.UserService
import link.netless.flat.http.HeaderProvider
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
    fun provideUserHeaderProvider(): HeaderProvider {
        return object : HeaderProvider {
            override fun getHeaders(): Set<Pair<String, String>> {
                return setOf("Authorization" to String.format("Bearer %s", Constants.WX_TOKEN))
            }
        }
    }

//    @Provides
//    @Singleton
//    fun provideUserRepository(
//        userService: UserService,
//        @AppModule.GlobalData appDataCenter: AppDataCenter
//    ): UserRepository {
//        return UserRepository(userService, appDataCenter)
//    }
}
