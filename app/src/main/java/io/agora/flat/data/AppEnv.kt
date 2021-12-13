package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
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
    }

    val envMap = mutableMapOf<String, EnvItem>()

    init {
        envMap[ENV_DEV] = EnvItem(
            "https://flat-api-dev.whiteboard.agora.io",
            "9821657775fbc74773f1",
            "https://flat-web-dev.whiteboard.agora.io"
        )
        envMap[ENV_PROD] = EnvItem(
            "https://flat-api.whiteboard.agora.io",
            "71a29285a437998bdfe0",
            "https://flat-web.whiteboard.agora.io",
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

    val githubCallback get() = "${flatServiceUrl}v1/login/github/callback"

    val baseInviteUrl = run {
        currentEnvItem.baseInviteUrl
    }

    data class EnvItem(
        val serviceUrl: String,
        val githubClientId: String,
        val baseInviteUrl: String,
    )
}