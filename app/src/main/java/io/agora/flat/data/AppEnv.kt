package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.BuildConfig
import io.agora.flat.di.interfaces.LogConfig
import javax.inject.Inject

/**
 * 应用内切换配置
 */
class AppEnv @Inject constructor(@ApplicationContext context: Context) {
    private val store: SharedPreferences = context.getSharedPreferences("flat_env", Context.MODE_PRIVATE)

    companion object {
        const val ENV_PROD = "prod"
        const val ENV_DEV = "dev"

        const val STORE_KEY_ENV = "key_env"

        const val AGORA_APP_ID = "931b86d6781e49a2a255db4ce6e8e804"
        const val AGORA_APP_ID_DEV = "a185de0a777f4c159e302abcc0f03b64"
    }

    val envMap = mutableMapOf<String, EnvItem>()

    init {
        envMap[ENV_DEV] = EnvItem(
            AGORA_APP_ID_DEV,
            "https://flat-api-dev.whiteboard.agora.io",
            "9821657775fbc74773f1",
            "https://flat-web-dev.whiteboard.agora.io",
            versionCheckUrl = "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/versions/latest/beta/android/checkVersion.json",
            logConfig = LogConfig(
                ak = BuildConfig.ALIYUN_LOG_DEV_AK,
                sk = BuildConfig.ALIYUN_LOG_DEV_SK,
                project = "hz-flat-dev",
                logstore = "android",
                endpoint = "cn-hangzhou.log.aliyuncs.com",
            ),
            ossKey = "LTAI5tD4WSVBxAyfwVoaKTWr",
        )
        envMap[ENV_PROD] = EnvItem(
            AGORA_APP_ID,
            "https://flat-api.whiteboard.agora.io",
            "71a29285a437998bdfe0",
            "https://flat-web.whiteboard.agora.io",
            versionCheckUrl = "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/versions/latest/stable/android/checkVersion.json",
            logConfig = LogConfig(
                ak = BuildConfig.ALIYUN_LOG_PROD_AK,
                sk = BuildConfig.ALIYUN_LOG_PROD_SK,
                project = "flat-prod",
                logstore = "android",
                endpoint = "cn-hangzhou.log.aliyuncs.com",
            ),
            ossKey = "LTAI5tMwHQ1xyroeneA9XLh4",
        )
    }

    fun setEnv(env: String) {
        store.edit(commit = true) {
            putString(STORE_KEY_ENV, env)
        }
    }

    fun getEnv(): String {
        return store.getString(STORE_KEY_ENV, ENV_PROD)!!
    }

    private val currentEnvItem = envMap[getEnv()]!!

    val flatServiceUrl = run {
        currentEnvItem.serviceUrl
    }

    val githubClientID = run {
        currentEnvItem.githubClientId
    }

    val githubCallback get() = "${flatServiceUrl}/v1/login/github/callback"

    val githubBindingCallback get() = "${flatServiceUrl}/v1/login/github/callback/binding"

    val baseInviteUrl = run {
        currentEnvItem.baseInviteUrl
    }

    val agoraAppId get() = currentEnvItem.agoraAppId

    val versionCheckUrl get() = currentEnvItem.versionCheckUrl

    val logConfig get() = currentEnvItem.logConfig

    val ossKey get() = currentEnvItem.ossKey

    data class EnvItem(
        val agoraAppId: String,
        val serviceUrl: String,
        val githubClientId: String,
        val baseInviteUrl: String,
        val versionCheckUrl: String,
        val logConfig: LogConfig,
        val ossKey: String,
    )
}