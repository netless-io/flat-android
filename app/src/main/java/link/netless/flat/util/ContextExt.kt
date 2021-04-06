package link.netless.flat.util

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast

fun Context.getAppVersion(defaultVersion: String = "1.0.0"): String {
    var versionName: String? = null
    try {
        versionName = packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return versionName ?: defaultVersion;
}

fun Context.showDebugToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}