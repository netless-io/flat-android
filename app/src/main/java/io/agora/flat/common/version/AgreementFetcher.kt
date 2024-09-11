package io.agora.flat.common.version

import com.google.gson.Gson
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.data.AppEnv
import io.agora.flat.data.model.Agreement
import io.agora.flat.data.model.AgreementsResp
import io.agora.flat.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class AgreementFetcher @Inject constructor(
    @NetworkModule.NormalOkHttpClient private val client: OkHttpClient,
    private val appEnv: AppEnv,
    private val gson: Gson = Gson()  // Facilitates testing by allowing injection.
) {
    suspend fun fetchAgreement(): Agreement? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(appEnv.agreementsUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respJson = response.body?.string()
                    if (respJson != null) {
                        val env = appEnv.getEnv()
                        val lang = LanguageManager.currentLocale().language
                        val agreementsResp = gson.fromJson(respJson, AgreementsResp::class.java)
                        return@withContext agreementsResp.data[env]?.get(lang)
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}