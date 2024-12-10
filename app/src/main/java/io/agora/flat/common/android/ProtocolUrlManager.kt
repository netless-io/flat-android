package io.agora.flat.common.android

import android.app.Application
import android.content.Context
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppEnv.Companion.ENV_CN_DEV
import io.agora.flat.data.AppEnv.Companion.ENV_CN_PROD
import io.agora.flat.data.AppEnv.Companion.ENV_SG_DEV
import io.agora.flat.data.AppEnv.Companion.ENV_SG_PROD

object ProtocolUrlManager {
    private const val PRIVACY = "privacy"
    private const val SERVICE = "service"
    private const val WEBSITE = "website"

    private lateinit var application: Context

    private val appEnv by lazy { AppEnv(application) }

    private val urls: Map<String, Map<String, Map<String, String>>> = mapOf(
        PRIVACY to mapOf(
            ENV_CN_PROD to mapOf(
                "en" to "https://www.flat.apprtc.cn/en/privacy.html", "zh" to "https://www.flat.apprtc.cn/privacy.html"
            ),
            ENV_CN_DEV to mapOf(
                "en" to "https://www.flat.apprtc.cn/en/privacy.html", "zh" to "https://www.flat.apprtc.cn/privacy.html"
            ),
            ENV_SG_PROD to mapOf(
                "en" to "https://flat.agora.io/privacy.html", "zh" to "https://flat.agora.io/zh/privacy.html"
            ),
            ENV_SG_DEV to mapOf(
                "en" to "https://flat.agora.io/privacy.html", "zh" to "https://flat.agora.io/zh/privacy.html"
            ),
        ), SERVICE to mapOf(
            ENV_CN_PROD to mapOf(
                "en" to "https://www.flat.apprtc.cn/en/service.html", "zh" to "https://www.flat.apprtc.cn/service.html"
            ),
            ENV_CN_DEV to mapOf(
                "en" to "https://www.flat.apprtc.cn/en/service.html", "zh" to "https://www.flat.apprtc.cn/service.html"
            ),
            ENV_SG_PROD to mapOf(
                "en" to "https://flat.agora.io/en/service.html", "zh" to "https://flat.agora.io/zh/service.html"
            ),
            ENV_SG_DEV to mapOf(
                "en" to "https://flat.agora.io/en/service.html", "zh" to "https://flat.agora.io/zh/service.html"
            ),
        ),

        WEBSITE to mapOf(
            ENV_CN_PROD to mapOf(
                "en" to "https://www.flat.apprtc.cn/#download", "zh" to "https://www.flat.apprtc.cn/#download"
            ),
            ENV_CN_DEV to mapOf(
                "en" to "https://www.flat.apprtc.cn/#download", "zh" to "https://www.flat.apprtc.cn/#download"
            ),
            ENV_SG_PROD to mapOf(
                "en" to "https://flat.agora.io/#download", "zh" to "https://flat.agora.io/#download"
            ),
            ENV_SG_DEV to mapOf(
                "en" to "https://flat.agora.io/#download", "zh" to "https://flat.agora.io/#download"
            ),
        )
    )

    fun init(app: Application) {
        application = app
    }

    val Service: String
        get() {
            val env = appEnv.getEnv()
            val lang = LanguageManager.currentLocale().language
            return urls[SERVICE]!![env]?.get(lang) ?: "https://www.flat.apprtc.cn/service.html"
        }

    val Privacy: String
        get() {
            val env = appEnv.getEnv()
            val lang = LanguageManager.currentLocale().language
            return urls[PRIVACY]!![env]?.get(lang) ?: "https://www.flat.apprtc.cn/privacy.html"
        }

    val Website: String
        get() {
            val env = appEnv.getEnv()
            val lang = LanguageManager.currentLocale().language
            return urls[WEBSITE]!![env]?.get(lang) ?: "https://www.flat.apprtc.cn/#download"
        }
}