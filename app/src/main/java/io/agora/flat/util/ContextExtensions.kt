package io.agora.flat.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import io.agora.flat.R
import io.agora.flat.common.android.DarkModeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

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

fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
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

fun Context.px(@DimenRes dimen: Int): Int = resources.getDimension(dimen).toInt()

fun Context.dp(@DimenRes dimen: Int): Float = px(dimen) / resources.displayMetrics.density

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

fun Context.getActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.getActivity()
        else -> null
    }
}

fun Context.getCurrentLocale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0);
    } else {
        resources.configuration.locale;
    }
}

fun Context.contentInfo(uri: Uri): ContentInfo? {
    val mediaType = contentResolver.getType(uri) ?: "text/plain"
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        val filename = cursor.getString(0)
        val size = cursor.getLong(1)
        cursor.close()

        return ContentInfo(uri, filename, size, mediaType)
    }
}

data class ContentInfo(val uri: Uri, val filename: String, val size: Long, val mediaType: String)

fun Context.installApk(uri: Uri) {
    try {
        var apkUri = uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(this, "$packageName.provider", File(uri.path))
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivity(intent)
    } catch (e: Exception) {
        // ignore
    }
}

fun Context.launchMarket() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: ActivityNotFoundException) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
        )
    }
}

fun Context.launchBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // ignore
    }
}