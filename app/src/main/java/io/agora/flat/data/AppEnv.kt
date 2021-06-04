package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 *  应用内切换配置
 */
class AppEnv @Inject constructor(@ApplicationContext context: Context) {
    private val store: SharedPreferences = context.getSharedPreferences("flat_env", Context.MODE_PRIVATE)

    companion object {
        const val ENV_PROD = "prod"
        const val ENV_DEV = "dev"
        const val ENV_LOCAL = "local"

        const val STORE_KEY_ENV = "key_env"
    }

    val envMap = mutableMapOf<String, EnvItem>()

    init {
        EnvItem("https://flat-api-dev.whiteboard.agora.io/", "9821657775fbc74773f1").also { envMap[ENV_DEV] = it }
        EnvItem("https://flat-api.whiteboard.agora.io/", "71a29285a437998bdfe0").also { envMap[ENV_PROD] = it }
        // EnvItem("https://api-flat-local.netless.group/", "d07230378ca29cef90ee").also { envMap[ENV_LOCAL] = it }
    }

    fun setEnv(env: String) {
        store.edit(commit = true) {
            putString(STORE_KEY_ENV, env)
        }
    }

    fun getEnv(): String {
        return store.getString(STORE_KEY_ENV, ENV_PROD)!!
    }

    val flatServiceUrl
        get() = run {
            val env = store.getString(STORE_KEY_ENV, ENV_PROD)
            envMap[env]!!.serviceUrl
        }

    val githubClientID
        get() = run {
            val env = store.getString(STORE_KEY_ENV, ENV_PROD)
            envMap[env]!!.githubClientId
        }

    val githubCallback get() = "${flatServiceUrl}v1/login/github/callback"

    data class EnvItem(val serviceUrl: String, val githubClientId: String)
}