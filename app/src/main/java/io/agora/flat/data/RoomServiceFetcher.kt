package io.agora.flat.data

import io.agora.flat.di.NetworkModule
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
class RoomServiceFetcher @Inject constructor(
    @NetworkModule.NormalOkHttpClient private val client: OkHttpClient,
    private val appEnv: AppEnv
) {
    companion object {
        val regions = listOf(
            "CN",
            "SG",
        )

        val codeMap = mapOf(
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

    private val cache = mutableMapOf<String, RoomService>()

    fun fetch(uuid: String): RoomService {
        val env = fetchEnv(uuid, appEnv.getEnv())
        val envServiceUrl = appEnv.getEnvServiceUrl(env)

        return cache[env] ?: createRoomService(envServiceUrl).also {
            cache[env] = it
        }
    }

    private fun createRoomService(serviceUrl: String): RoomService {
        return Retrofit.Builder()
            .baseUrl(serviceUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoomService::class.java)
    }
}