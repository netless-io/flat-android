package io.agora.flat.http.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class AgoraMessageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val builder = request.newBuilder().apply {
            addHeader("Content-type", "application/json; charset=utf-8")
        }

        return chain.proceed(builder.build())
    }
}
