package link.netless.flat.di;

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import link.netless.flat.Constants
import link.netless.flat.data.api.RoomService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideRoomService(@NetworkModule.NormalOkHttpClient client: OkHttpClient): RoomService {
        return Retrofit.Builder()
            .baseUrl(Constants.FLAT_SERVICE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoomService::class.java)
    }
}
