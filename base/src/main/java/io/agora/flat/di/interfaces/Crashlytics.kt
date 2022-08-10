package io.agora.flat.di.interfaces

import android.content.Context

interface Crashlytics {
    fun init(context: Context) {}

    fun setUserId(id: String) {}

    fun log(tag: String?, message: String, t: Throwable?) {}
}