package io.agora.flat.util

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import io.agora.flat.BuildConfig

fun Context.getAppVersion(defaultVersion: String = "1.0.0"): String {
    var versionName: String? = null
    try {
        versionName = packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return versionName ?: defaultVersion
}

fun Context.showDebugToast(message: String) {
    if (BuildConfig.DEBUG) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.dp2px(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}