package io.agora.flat.ui.compose

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.agora.flat.data.AliyunCaptchaConfig
import io.agora.flat.data.AppEnv
import io.agora.flat.util.JsonUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val JS_BRIDGE_NAME = "captchaJsBridge"
private const val CAPTCHA_HTML_URL = "file:///android_asset/captcha/index.html"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CaptchaDialog(
    onVerifySuccess: (String) -> Unit,
    onVerifyFail: () -> Unit = { },
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var successHandled by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CaptchaWebView(
                onVerifySuccess = {
                    successHandled = true
                    onVerifySuccess(it)
                },
                onVerifyFail = onVerifyFail,
                onClose = {
                    scope.launch {
                        delay(200)
                        if (!successHandled) onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
fun CaptchaWebView(
    onVerifySuccess: (String) -> Unit,
    onVerifyFail: () -> Unit = { },
    onClose: () -> Unit = { },
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.cleanupAndDestroy()
            webView = null
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webView = this
                setupWebView()
                addCaptchaJsBridge(
                    onVerifySuccess = onVerifySuccess,
                    onVerifyFail = onVerifyFail,
                    onClose = onClose,
                    captchaConfig = AppEnv(context).aliyunCaptchaConfig
                )
                loadUrl(CAPTCHA_HTML_URL)
            }
        }
    )
}

private fun WebView.setupWebView() {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    WebView.setWebContentsDebuggingEnabled(true)

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_NO_CACHE
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = true
    }

    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }
    }
}

private fun WebView.addCaptchaJsBridge(
    onVerifySuccess: (String) -> Unit,
    onVerifyFail: () -> Unit,
    onClose: () -> Unit,
    captchaConfig: AliyunCaptchaConfig
) {
    addJavascriptInterface(
        CaptchaJsBridge(
            onVerifySuccess = onVerifySuccess,
            onVerifyFail = onVerifyFail,
            onClose = onClose,
            captchaConfig = captchaConfig
        ),
        JS_BRIDGE_NAME
    )
}

private fun WebView.cleanupAndDestroy() {
    removeJavascriptInterface(JS_BRIDGE_NAME)
    stopLoading()
    loadUrl("about:blank")
    clearHistory()
    clearCache(true)
    destroy()
}

class CaptchaJsBridge(
    private val onVerifySuccess: (String) -> Unit,
    private val onVerifyFail: () -> Unit,
    private val onClose: () -> Unit,
    private val captchaConfig: AliyunCaptchaConfig
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun getConfig(): String {
        return JsonUtils.toJson(captchaConfig)
    }

    @JavascriptInterface
    fun getCaptchaVerifyParam(captchaVerifyParam: String) {
        mainHandler.post { onVerifySuccess(captchaVerifyParam) }
    }

    @JavascriptInterface
    fun dispatchFail(result: String) {
        mainHandler.post { onVerifyFail() }
    }

    @JavascriptInterface
    fun dispatchClose() {
        mainHandler.post { onClose() }
    }
}