package io.agora.flat.common.version

import com.google.gson.Gson
import io.agora.flat.Config
import io.agora.flat.data.AppKVCenter
import io.agora.flat.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class VersionChecker constructor(
    @NetworkModule.NormalOkHttpClient val client: OkHttpClient,
    val appKVCenter: AppKVCenter,
    private val appVersion: String,
    private val versionCheckerUrl: String,
) {
    private val gson = Gson()
    private var lastCheckerTime = 0L

    companion object {
        internal fun checkCanUpdate(local: String, remote: String): Boolean {
            return !local.atLeast(remote)
        }

        internal fun checkForceUpdate(local: String, minVersion: String): Boolean {
            return !local.atLeast(minVersion)
        }

        private fun String.atLeast(version: String): Boolean {
            val localParts = this.split(".")
            val versionParts = version.split(".")

            if (localParts.size != 3 && versionParts.size != 3) {
                return true
            }
            for (i in 0..2) {
                if (versionParts[i].toInt() < localParts[i].toInt()) {
                    return true
                }
                if (versionParts[i].toInt() > localParts[i].toInt()) {
                    return false
                }
            }
            return true
        }
    }

    suspend fun check(): VersionCheckResult {
        if (System.currentTimeMillis() - lastCheckerTime < Config.callVersionCheckInterval) {
            return VersionCheckResult.Default
        }
        lastCheckerTime = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(versionCheckerUrl).build()
                val newCall = client.newCall(request)
                val versionBody = newCall.execute().body?.string()
                val response = gson.fromJson(versionBody, VersionResponse::class.java)
                val forceUpdate = checkForceUpdate(appVersion, response.minVersion)
                return@withContext VersionCheckResult(
                    appUrl = response.appUrl,
                    appVersion = response.appVersion,
                    title = response.title,
                    description = response.description,
                    showUpdate = checkCanUpdate(appVersion, response.appVersion) && canUpdateByUser() || forceUpdate,
                    forceUpdate = forceUpdate,
                )
            } catch (e: Exception) {
                // ignore
                e.printStackTrace()
                return@withContext VersionCheckResult.Default
            }
        }
    }

    private fun canUpdateByUser(): Boolean {
        val lastCancelUpdate = appKVCenter.getLastCancelUpdate()
        return System.currentTimeMillis() - lastCancelUpdate > Config.INTERVAL_VERSION_CHECK
    }

    fun cancelUpdate() {
        appKVCenter.setLastCancelUpdate(System.currentTimeMillis())
    }

    internal class VersionResponse(
        val appVersion: String,
        val appUrl: String,
        val title: String,
        val description: String,
        val minVersion: String,
    )
}