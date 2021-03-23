package com.agora.netless.flat.http.interceptor

import com.agora.netless.flat.Constants
import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val builder = request.newBuilder().apply {
            addHeader("Content-type", "application/json; charset=utf-8")
            addHeader("Authorization", String.format("Bearer %s", Constants.WX_TOKEN))
        }

        return chain.proceed(builder.build())
    }
}