package io.agora.flat.http.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class BodyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}