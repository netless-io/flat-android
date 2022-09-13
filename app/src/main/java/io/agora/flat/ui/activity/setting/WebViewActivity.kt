package io.agora.flat.ui.activity.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.agora.flat.Constants
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import kotlinx.coroutines.launch

class WebViewActivity : BaseComposeActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WebViewContent(
                url = intent.getStringExtra(Constants.IntentKey.URL)!!,
                title = intent.getStringExtra(Constants.IntentKey.TITLE),
                onPageBack = { this.finish() }
            )
        }
    }
}

@Composable
internal fun WebViewContent(url: String, title: String?, onPageBack: () -> Unit) {
    var webViewProgress by remember { mutableStateOf(-1) }
    var titleLocal by remember { mutableStateOf(title ?: "") }

    FlatColumnPage {
        BackTopAppBar(title = titleLocal, onBackPressed = onPageBack)
        Box {
            ComposeWebView(
                modifier = Modifier.fillMaxSize(),
                url = url,
                onProgressChange = { progress ->
                    webViewProgress = progress
                },
                onTitleChange = { t ->
                    if (titleLocal == "") titleLocal = t
                },
                initSettings = { settings ->
                    settings?.apply {
                        javaScriptEnabled = true
                        useWideViewPort = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        setSupportZoom(false)
                    }
                }, onBack = { webView ->
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        onPageBack()
                    }
                }, onReceivedError = {

                }
            )
            if (webViewProgress != 100) {
                LinearProgressIndicator(
                    progress = webViewProgress * 1.0F / 100F,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
fun ComposeWebView(
    modifier: Modifier = Modifier,
    url: String,
    onBack: (webView: WebView?) -> Unit,
    onProgressChange: (progress: Int) -> Unit = {},
    onTitleChange: (title: String) -> Unit = {},
    initSettings: (webSettings: WebSettings?) -> Unit = {},
    onReceivedError: (error: WebResourceError?) -> Unit = {},
) {
    val webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChange(newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            onTitleChange(title)
        }
    }

    val webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onProgressChange(-1)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            onProgressChange(100)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (null == request.url) return false
            val showOverrideUrl = request.url.toString()
            try {
                if (!showOverrideUrl.startsWith("http://") && !showOverrideUrl.startsWith("https://")) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(showOverrideUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        view.context?.applicationContext?.startActivity(this)
                    }
                    return true
                }
            } catch (e: Exception) {
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            onReceivedError(error)
        }
    }

    var webView by remember { mutableStateOf<WebView?>(null) }

    val scope = rememberCoroutineScope()
    BackHandler {
        scope.launch { onBack(webView) }
    }

    // Due to limited knowledge,information and time, more input is needed here to verify the correctness
    DisposableEffect(
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.webViewClient = webViewClient
                    this.webChromeClient = webChromeClient
                    initSettings(this.settings)
                    webView = this
                    loadUrl(url)
                }
            })
    ) {
        onDispose {
            webView?.run {
                removeAllViews()
                destroy()
                webView = null
            }
        }
    }
}