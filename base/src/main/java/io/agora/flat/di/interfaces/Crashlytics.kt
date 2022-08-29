package io.agora.flat.di.interfaces

import android.content.Context

interface Crashlytics {
    fun init(context: Context) {}

    fun setUserId(id: String) {}

    fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
}