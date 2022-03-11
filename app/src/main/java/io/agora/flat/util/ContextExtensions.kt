package io.agora.flat.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import io.agora.flat.R
import io.agora.flat.common.android.DarkModeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


fun Context.getAppVersion(defaultVersion: String = "1.0.0"): String {
    var versionName: String? = null
    try {
        versionName = packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return versionName ?: defaultVersion
}

fun Context.isApkInDebug(): Boolean {
    return try {
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun Context.showDebugToast(@StringRes resId: Int) {
    if (isApkInDebug()) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showDebugToast(message: String) {
    if (isApkInDebug()) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.dp2px(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Context.px2dp(px: Int): Int {
    return (px.toFloat() / resources.displayMetrics.density + 0.5).toInt()
}

fun Context.isTabletMode(): Boolean {
    return resources.getBoolean(R.bool.isTablet)
}

fun Context.isPhoneMode(): Boolean {
    return !isTabletMode()
}

fun ComponentActivity.delayAndFinish(duration: Long = 2000, message: String = "") {
    lifecycleScope.launch {
        if (message.isNotBlank()) {
            showToast(message)
        }
        delay(duration)
        finish()
    }
}

fun ComponentActivity.isDarkMode(): Boolean = when (DarkModeManager.current()) {
    DarkModeManager.Mode.Auto -> {
        val nightMode: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightMode == Configuration.UI_MODE_NIGHT_YES
    }
    DarkModeManager.Mode.Light -> false
    DarkModeManager.Mode.Dark -> true
}


fun Context.contentFileInfo(uri: Uri): ContentFileInfo? {
    val mediaType = contentResolver.getType(uri) ?: "text/plain"
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        val filename = cursor.getString(0)
        val size = cursor.getLong(1)
        cursor.close()

        return ContentFileInfo(filename, size, mediaType)
    }
}

data class ContentFileInfo(val filename: String, val size: Long, val mediaType: String)

