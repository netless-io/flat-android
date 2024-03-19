package io.agora.flat.data

import io.agora.flat.di.NetworkModule
import io.agora.flat.http.api.CloudRecordService
import io.agora.flat.http.api.RoomService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Fetch room service by uuid
 *
 * For joining rooms between different regions
 */
@Singleton
class ServiceFetcher @Inject constructor(
    @NetworkModule.NormalOkHttpClient private val client: OkHttpClient,
    private val appEnv: AppEnv
) {
    companion object {
        private val regions = listOf(
            "CN",
            "SG",
        )

        private val codeMap = mapOf(
            "1" to "CN",
            "2" to "SG",
        )

        fun fetchEnv(uuid: String, currentEnv: String): String {
            var (region, envType) = currentEnv.split("_")

            // short invite code
            if (uuid.length == 11) {
                val code = uuid[0] + ""
                codeMap[code]?.let { region = it }
            }

            // long uuid
            if (uuid.length > 15) {
                val firstTwo = uuid.substring(0, 2)
                regions.find { it == firstTwo.uppercase() }?.let { region = it }
            }

            return "${region}_$envType".lowercase()
        }
    }

    private val allCache = mutableMapOf<Pair<String, String>, Any>()

    fun fetchRoomService(uuid: String): RoomService {
        return getApiService<RoomService>(uuid)
    }

    fun fetchCloudRecordService(uuid: String): CloudRecordService {
        return getApiService<CloudRecordService>(uuid)
    }

    private inline fun <reified T> getApiService(uuid: String): T {
        val env = fetchEnv(uuid, appEnv.getEnv())
        val name = T::class.java.simpleName
        return allCache.getOrPut(env to name) {
            val serviceUrl = appEnv.getEnvServiceUrl(env)
            createService<T>(serviceUrl)!!
        } as T
    }

    private inline fun <reified T> createService(serviceUrl: String): T {
        return Retrofit.Builder()
            .baseUrl(serviceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(T::class.java)
    }
}