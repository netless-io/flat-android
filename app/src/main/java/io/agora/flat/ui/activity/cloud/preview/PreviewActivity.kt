package io.agora.flat.ui.activity.cloud.preview

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberImagePainter
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.CoursewareType
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.setting.ComposeWebView
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.MaxWidthSpread

@AndroidEntryPoint
class PreviewActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PreviewPage(onClose = { this.finish() })
        }
    }
}

@Composable
private fun PreviewPage(
    viewModel: PreviewViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    val viewState by viewModel.state.collectAsState()

    val actioner: (PreviewAction) -> Unit = { action ->
        when (action) {
            PreviewAction.OnClose -> {
                onClose()
            }
            PreviewAction.OnLoadFinished -> {
                viewModel.onLoadFinished()
            }
        }
    }

    FlatColumnPage {
        CloseTopAppBar(stringResource(R.string.title_cloud_preview), onClose = { onClose() })
        Box(MaxWidthSpread) {
            Box {
                when (viewState.type) {
                    CoursewareType.Unknown -> {
                        LaunchedEffect(viewState.type) {
                            onClose()
                        }
                    }
                    CoursewareType.Image -> ImagePreview(file = viewState.file!!, actioner = actioner)

                    CoursewareType.Audio,
                    CoursewareType.Video,
                    -> MediaPreview(file = viewState.file!!, actioner = actioner)

                    CoursewareType.DocStatic,
                    CoursewareType.DocDynamic,
                    -> DocumentPreview(
                        viewState = viewState,
                        actioner = actioner
                    )
                }
            }
            if (viewState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    FlatPageLoading()
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocumentPreview(
    modifier: Modifier = Modifier.fillMaxSize(),
    viewState: PreviewState,
    actioner: (PreviewAction) -> Unit,
) {
    Box {
        ComposeWebView(
            modifier = modifier,
            url = viewState.previewUrl,
            initSettings = { settings ->
                settings?.apply {
                    javaScriptEnabled = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    setSupportZoom(false)
                }
            },
            onBack = {
                actioner(PreviewAction.OnClose)
            },
            onProgressChange = {
                if (it >= 100) {
                    actioner(PreviewAction.OnLoadFinished)
                }
            },
            onReceivedError = {
                actioner(PreviewAction.OnClose)
            }
        )
    }
}

@Composable
fun MediaPreview(
    modifier: Modifier = Modifier.fillMaxSize(),
    file: CloudStorageFile,
    actioner: (PreviewAction) -> Unit,
) {
    LaunchedEffect(file) {
        actioner(PreviewAction.OnLoadFinished)
    }

    var playerControl by remember {
        mutableStateOf<MediaPlayback?>(null)
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        ComposeVideoPlayer(
            uriString = file.fileURL,
            onPlayEvent = {},
            onPlayerControl = { playerControl = it },
            Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ImagePreview(
    modifier: Modifier = Modifier.fillMaxSize(),
    file: CloudStorageFile,
    actioner: (PreviewAction) -> Unit,
) {
    LaunchedEffect(file) {
        actioner(PreviewAction.OnLoadFinished)
    }

    Image(
        painter = rememberImagePainter(file.fileURL),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
