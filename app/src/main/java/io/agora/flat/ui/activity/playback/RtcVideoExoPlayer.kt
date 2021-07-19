package io.agora.flat.ui.activity.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.herewhite.sdk.combinePlayer.NativePlayer
import com.herewhite.sdk.combinePlayer.NativePlayer.NativePlayerPhase
import com.herewhite.sdk.combinePlayer.PlayerSyncManager
import io.agora.flat.R
import io.agora.flat.data.model.RecordItem
import java.util.concurrent.TimeUnit

class RtcVideoExoPlayer(
    private val context: Context,
    private val records: List<RecordItem>,
) :
    NativePlayer, Player.EventListener {
    companion object {
        private const val TAG = "RtcVideoExoPlayer"
    }

    private var exoPlayer: SimpleExoPlayer?
    private var mediaSource: MediaSource? = null
    private var playerView: PlayerView? = null
    private val dataSourceFactory: DataSource.Factory
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var playerSyncManager: PlayerSyncManager? = null
    private var playerPhase = NativePlayerPhase.Idle
    private var currentState = Player.STATE_IDLE

    init {
        exoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()
        exoPlayer!!.addListener(this)
        exoPlayer!!.setAudioAttributes(AudioAttributes.DEFAULT, true)
        exoPlayer!!.playWhenReady = false
        dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)))
        setVideoPath(records[0].videoURL)
    }

    /**
     * 绑定 playerSyncManager，设置的同时，需要将当前实例的 NativePlayerPhase 也更新
     *
     * @param player PlayerSyncManager 实例
     */
    fun bindPlayerSyncManager(player: PlayerSyncManager?) {
        Log.d(TAG, "setPlayerSyncManager: $playerPhase")
        playerSyncManager = player
        playerSyncManager!!.updateNativePhase(playerPhase)
    }

    /**
     * 设置播放视图
     *
     * @param playerView 视图实例
     */
    fun setPlayerView(playerView: PlayerView) {
        this.playerView = playerView
        this.playerView!!.requestFocus()
        this.playerView!!.player = exoPlayer
    }

    /**
     * 设置播放链接
     *
     * @param path 播放链接
     */
    fun setVideoPath(path: String) {
        setVideoURI(Uri.parse(path))
    }

    /**
     * 设置播放 Uri
     *
     * @param uri 播放链接对应的 Uri
     */
    fun setVideoURI(uri: Uri) {
        mediaSource = createMediaSource(uri)
        playerPhase = NativePlayerPhase.Buffering
        exoPlayer!!.prepare(mediaSource!!)
    }

    /**
     * 由 nativePlayer 进行主动 seek，然后在 seek 完成后，再调用 [PlayerSyncManager] 同步
     *
     * @param time 跳转时间戳
     * @param unit 时间戳单位
     */
    fun seek(time: Long, unit: TimeUnit) {
        val timestampMs = TimeUnit.MILLISECONDS.convert(time, unit)
        exoPlayer!!.seekTo(timestampMs)
    }

    /**
     * 获取当前是否正在播放
     *
     * @return true or false
     */
    val isPlaying: Boolean
        get() = if (exoPlayer == null) false else exoPlayer!!.isPlaying

    /**
     * 在当播放器对用户可见以及 `surface_type` 是 `spherical_gl_surface_view` 时，应调用此方法。
     * 此方法与 [.onPause] 对应
     *
     *
     * 通常应在 `Activity.onStart()`, 或者 API level <= 23 时的 `Activity.onResume()` 中调用此方法
     */
    fun onResume() {
        playerView!!.onResume()
    }

    /**
     * 在当播放器对用户不可见以及 `surface_type` 为 `spherical_gl_surface_view` 时，应调用此方法
     * 此方法与 [.onResume] 对应
     *
     *
     * 通常应在 `Activity.onStop()`, 或者 API level <= 23 时的 `Activity.onPause()` 中调用此方法
     */
    fun onPause() {
        playerView!!.onPause()
    }

    /**
     * 释放播放器资源
     */
    fun release() {
        if (exoPlayer != null) {
            exoPlayer!!.release()
            exoPlayer = null
            mediaSource = null
        }
    }

    /**
     * 获取当前播放时间
     *
     * @return 当前播放时间，单位：ms
     */
    val currentPosition: Long
        get() = exoPlayer!!.currentPosition

    /**
     * 获取视频总时长
     *
     * @return 视频总时长，单位：ms
     */
    val duration: Long
        get() = exoPlayer!!.duration

    override fun play() {
        handler.post {
            if (isPlaying) {
                return@post
            }
            exoPlayer!!.playWhenReady = true
        }
    }

    override fun pause() {
        handler.post {
            if (!isPlaying) {
                return@post
            }
            exoPlayer!!.playWhenReady = false
        }
    }

    override fun hasEnoughBuffer(): Boolean {
        return if (currentState == Player.STATE_IDLE) {
            false
        } else {
            playerPhase != NativePlayerPhase.Buffering
        }
    }

    override fun getPhase(): NativePlayerPhase {
        return playerPhase
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.e(TAG, "onPlayerStateChanged $currentState")
        currentState = playbackState
        when (playbackState) {
            Player.STATE_IDLE -> {
            }
            Player.STATE_BUFFERING -> {
                // 缓冲中触发，缓冲完会触发 STATE_READY 状态
                playerPhase = NativePlayerPhase.Buffering
                if (playerSyncManager != null) {
                    playerSyncManager!!.updateNativePhase(playerPhase)
                }
            }
            Player.STATE_READY -> {
                // 准备好并可以立即播放时触发
                playerPhase = if (isPlaying) NativePlayerPhase.Playing else NativePlayerPhase.Pause
                if (playerSyncManager != null) {
                    playerSyncManager!!.updateNativePhase(playerPhase)
                }
            }
            Player.STATE_ENDED -> {
            }
            else -> {
            }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.e(TAG, "onError: " + error.message)
        when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
            }
            ExoPlaybackException.TYPE_RENDERER -> {
            }
            ExoPlaybackException.TYPE_UNEXPECTED -> {
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        // 播放状态改变时会执行此回调
        playerPhase = if (isPlaying) NativePlayerPhase.Playing else NativePlayerPhase.Pause
    }

    override fun onSeekProcessed() {
        if (playerSyncManager != null) {
            val pos = exoPlayer!!.currentPosition
            playerSyncManager!!.seek(pos, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * 创建播放源
     * 当前仅支持 Hls 以及 mp4 等常规媒体文件，其他格式请参考 []//exoplayer.dev/media-sources.html"">&quot;https://exoplayer.dev/media-sources.html&quot;
     *
     * @param uri 播放地址
     * @return MediaSource 对象
     */
    private fun createMediaSource(uri: Uri): MediaSource {
        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }
}