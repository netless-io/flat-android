package io.agora.flat.common.android

import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidDownloader @Inject constructor(@ApplicationContext val context: Context) {
    init {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val value = downloadCache.remove(reference)
                value?.run { cont.resume(desUri) }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    private val downloadCache = HashMap<Long, Item>()

    suspend fun download(url: String, fileName: String): Uri = suspendCoroutine { cont ->
        val storeFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (storeFile.exists()) {
            cont.resume(Uri.fromFile(storeFile))
            return@suspendCoroutine
        }

        val desUri = Uri.fromFile(storeFile)
        val request = Request(Uri.parse(url))
            .setDestinationUri(desUri)
            .setAllowedNetworkTypes(Request.NETWORK_MOBILE or Request.NETWORK_WIFI)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(fileName)
        val downloadId = downloadManager.enqueue(request)
        downloadCache[downloadId] = Item(url, fileName, desUri, cont)
    }

    internal data class Item(
        val url: String,
        val fileName: String,
        val desUri: Uri,
        val cont: Continuation<Uri>,
    )
}