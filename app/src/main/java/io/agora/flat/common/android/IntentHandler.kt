package io.agora.flat.common.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.R
import java.io.File
import javax.inject.Inject

class IntentHandler @Inject constructor(@ApplicationContext val context: Context) {

    fun launchUrl(url: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            val createIntent = Intent.createChooser(
                intent,
                context.getString(R.string.intent_browser_choose_title),
            )
            createIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(createIntent)
        }
    }

    fun installApk(uri: Uri) {
        try {
            var apkUri = uri
            val intent = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context, context.packageName + ".provider", File(uri.path))
                intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }
}