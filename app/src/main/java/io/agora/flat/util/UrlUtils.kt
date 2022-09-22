package io.agora.flat.util

import java.net.HttpURLConnection
import java.net.URL

object UrlUtils {
    fun isResourceExisted(url: String): Boolean {
        return try {
            val huc: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            huc.requestMethod = "HEAD"
            huc.responseCode in 200..299
        } catch (ignore: Exception) {
            false
        }
    }
}