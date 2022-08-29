package io.agora.flat.ui.compose

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

@Composable
fun ComposeVideoPlayer(
    uriString: String,
    onPlayEvent: (Player.Events) -> Unit,
    onPlayerControl: (MediaPlayback) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        SimpleExoPlayer.Builder(context).build().apply {
            this.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    onPlayEvent(events)
                }
            })
        }
    }

    LaunchedEffect(uriString) {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, context.packageName)
        )

        val mediaItem = MediaItem
            .Builder()
            .setUri(Uri.parse(uriString))
            .build()

        val source = ProgressiveMediaSource
            .Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSources(listOf(source), true)
        exoPlayer.prepare()
    }

    LifecycleHandler(
        onStart = {
            if (exoPlayer.isPlaying.not()) {
                exoPlayer.play()
            }
        },
        onStop = {
            exoPlayer.pause()
        },
    )

    ConstraintLayout {
        val (videoPlayer) = createRefs()
        DisposableEffect(
            AndroidView(
                modifier = modifier
                    .testTag("VideoPlayer")
                    .constrainAs(videoPlayer) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                })
        ) {
            onDispose {
                exoPlayer.release()
            }
        }
    }

    onPlayerControl(remember(exoPlayer) {
        object : MediaPlayback {
            override fun playPause() {
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            }

            override fun forward(durationInMillis: Long) {
                exoPlayer.seekTo(exoPlayer.currentPosition + durationInMillis)
            }

            override fun rewind(durationInMillis: Long) {
                exoPlayer.seekTo(exoPlayer.currentPosition - durationInMillis)
            }
        }
    })
}

interface MediaPlayback {
    fun playPause()

    fun forward(durationInMillis: Long)

    fun rewind(durationInMillis: Long)
}