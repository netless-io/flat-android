package io.agora.flat.ui.activity.playback

import android.util.Log
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.agora.netless.syncplayer.AtomPlayer
import com.agora.netless.syncplayer.AtomPlayerListener
import com.agora.netless.syncplayer.AtomPlayerPhase
import com.agora.netless.syncplayer.ClusterPlayer
import com.agora.netless.syncplayer.MultiVideoPlayer
import com.agora.netless.syncplayer.VideoItem
import com.agora.netless.syncplayer.WhiteboardPlayer
import com.herewhite.sdk.CommonCallback
import com.herewhite.sdk.Player
import com.herewhite.sdk.WhiteSdk
import com.herewhite.sdk.WhiteSdkConfiguration
import com.herewhite.sdk.domain.PlayerConfiguration
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.Region
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.WindowParams
import com.herewhite.sdk.domain.WindowPrefersColorScheme.Dark
import com.herewhite.sdk.domain.WindowPrefersColorScheme.Light
import io.agora.board.fast.model.FastRegion
import io.agora.flat.BuildConfig
import io.agora.flat.R
import io.agora.flat.data.AppEnv
import io.agora.flat.data.model.RecordItem
import io.agora.flat.databinding.ComponentReplayVideoBinding
import io.agora.flat.databinding.ComponentReplayWhiteboardBinding
import io.agora.flat.ui.activity.play.BaseComponent
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.isDarkMode
import kotlinx.coroutines.launch
import org.json.JSONObject

class ReplayPlayerComponent(
    activity: ReplayActivity,
    rootView: FrameLayout,
    private val videoLayout: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = ReplayPlayerComponent::class.simpleName
        const val SEEK_BAR_MAX_PROGRESS = 100
    }

    private lateinit var whiteBinding: ComponentReplayWhiteboardBinding
    private lateinit var videoBinding: ComponentReplayVideoBinding
    private lateinit var whiteSdk: WhiteSdk

    private val appEnv = AppEnv(activity.applicationContext)
    private val viewModel: ReplayViewModel by activity.viewModels()
    private var whiteboardPlayer: WhiteboardPlayer? = null
    private var videoCombinePlayer: AtomPlayer? = null
    private var clusterPlayer: AtomPlayer? = null
    private var isSeeking = false

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        initWhiteboard()
        observeData()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        val whiteboardView = whiteBinding.whiteboardView
        whiteboardView.removeAllViews()
        whiteboardView.destroy()

        clusterPlayer?.release()
    }

    private fun initView() {
        whiteBinding = ComponentReplayWhiteboardBinding.inflate(activity.layoutInflater, rootView, true)
        videoBinding = ComponentReplayVideoBinding.inflate(activity.layoutInflater, videoLayout, true)

        whiteBinding.playbackSeekBar.max = SEEK_BAR_MAX_PROGRESS
        whiteBinding.playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var targetProgress: Int = -1

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    targetProgress = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
                targetProgress = -1
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                if (targetProgress != -1) {
                    clusterPlayer?.seekTo(toPosition(targetProgress))
                }
            }
        })
        whiteBinding.playbackStart.setOnClickListener {
            startPlayer()
        }
        whiteBinding.playbackPause.setOnClickListener {
            pausePlayer()
        }

        videoBinding.videosContainer.layoutParams = videoBinding.videosContainer.layoutParams.also {
            it.width = itemWidth
        }
    }

    private fun toPosition(progress: Int): Long {
        return (progress * viewModel.state.value.duration / SEEK_BAR_MAX_PROGRESS)
    }

    private fun toProgress(position: Long, duration: Long = viewModel.state.value.duration): Int {
        val percent = if (duration > 0L) position * 1f / duration else 0f
        return (percent * SEEK_BAR_MAX_PROGRESS).toInt()
    }

    private fun startPlayer() {
        clusterPlayer?.play()
        setPlaying(true)
    }

    private fun pausePlayer() {
        clusterPlayer?.pause()
        setPlaying(false)
    }

    private fun setPlaying(isPlaying: Boolean) {
        whiteBinding.playbackStart.isVisible = !isPlaying
        whiteBinding.playbackPause.isVisible = isPlaying
    }

    private fun initWhiteboard() {
        val configuration = WhiteSdkConfiguration(appEnv.whiteAppId, true).apply {
            isUserCursor = true
            useMultiViews = true
        }

        whiteSdk = WhiteSdk(whiteBinding.whiteboardView, activity, configuration)
        whiteSdk.setCommonCallbacks(object : CommonCallback {
            override fun urlInterrupter(sourceUrl: String): String {
                return sourceUrl
            }

            override fun onMessage(message: JSONObject) {
                Log.d(TAG, message.toString())
            }

            override fun sdkSetupFail(error: SDKError) {
                Log.e(TAG, "sdkSetupFail $error")
            }

            override fun throwError(args: Any) {
                Log.e(TAG, "throwError $args")
            }

            override fun onPPTMediaPlay() {
                Log.d(TAG, "onPPTMediaPlay")
            }

            override fun onPPTMediaPause() {
                Log.d(TAG, "onPPTMediaPause")
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.state.collect {
                if (it.recordInfo != null && clusterPlayer == null) {
                    createVideoPlayer(it.recordInfo.recordInfo, it.beginTime)
                    createWhitePlayer(
                        it.recordInfo.whiteboardRoomUUID,
                        it.recordInfo.whiteboardRoomToken,
                        it.recordInfo.region,
                        it.beginTime,
                        it.duration
                    )
                    whiteBinding.playbackTime.text = getPlaybackTime()
                }
            }
        }
    }

    private val itemWidth = activity.resources.getDimensionPixelSize(R.dimen.room_replay_video_width) * 17

    private fun createVideoPlayer(recordItems: List<RecordItem>, beginTime: Long) {
        if (recordItems.isNotEmpty()) {
            val videoItems = recordItems.map {
                VideoItem(
                    beginTime = it.beginTime - beginTime,
                    endTime = it.endTime - beginTime,
                    videoURL = it.videoURL
                )
            }
            val player = MultiVideoPlayer(activity, videoItems)
            player.setPlayerContainer(videoBinding.videosContainer)
            videoCombinePlayer = player
        }
    }

    private fun String.toRegion(): Region {
        val region = Region.values().find { it.name.lowercase().replace('_', '-') == this }
        return region ?: Region.cn
    }

    private fun createWhitePlayer(
        roomUUID: String,
        roomToken: String,
        region: String,
        beginTime: Long,
        duration: Long
    ) {
        val conf = PlayerConfiguration(roomUUID, roomToken).apply {
            val styleMap = hashMapOf(
                "bottom" to "30px",
                "right" to "44px",
                "position" to "fixed",
            )

            windowParams = WindowParams()
                .setChessboard(false)
                .setDebug(true)
                .setCollectorStyles(styleMap)
                .setContainerSizeRatio(9.0f / 16)
            windowParams.scrollVerticalOnly = true
            windowParams.prefersColorScheme = if (activity.isDarkMode()) Dark else Light
        }
        conf.region = region.toRegion()
        conf.beginTimestamp = beginTime
        conf.duration = duration

        whiteSdk.createPlayer(conf, object : Promise<Player> {
            override fun then(player: Player) {
                whiteboardPlayer = WhiteboardPlayer(player)
                clusterPlayer = if (videoCombinePlayer == null) {
                    whiteboardPlayer!!
                } else {
                    ClusterPlayer(videoCombinePlayer!!, whiteboardPlayer!!)
                }

                clusterPlayer?.addPlayerListener(object : AtomPlayerListener {
                    override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                        super.onPositionChanged(atomPlayer, position)
                        if (!isSeeking) {
                            whiteBinding.playbackSeekBar.progress = toProgress(position)
                            whiteBinding.playbackTime.text = getPlaybackTime(position)
                        }
                    }

                    override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {}

                    override fun onPhaseChanged(atomPlayer: AtomPlayer, phase: AtomPlayerPhase) {
                        when (phase) {
                            AtomPlayerPhase.Idle -> {}
                            AtomPlayerPhase.Ready -> {
                            }

                            AtomPlayerPhase.Paused -> {
                                setPlaying(false)
                            }

                            AtomPlayerPhase.Playing -> {
                                setPlaying(true)
                            }

                            AtomPlayerPhase.Buffering -> {

                            }

                            AtomPlayerPhase.End -> {
                                clusterPlayer?.pause()
                                clusterPlayer?.seekTo(0)
                                setPlaying(false)
                            }
                        }
                        showLoading(AtomPlayerPhase.Buffering == phase)
                    }
                })
                clusterPlayer?.prepare()
            }

            override fun catchEx(error: SDKError) {
                Log.e(TAG, "createPlayer error: $error")
                // handle create error
            }
        })
    }

    private fun showLoading(show: Boolean) {
        whiteBinding.progressBar.isVisible = show
    }

    private fun getPlaybackTime(position: Long? = null): String {
        return if (position != null) {
            "${FlatFormatter.timeDisplay(position)}/${FlatFormatter.timeDisplay(viewModel.state.value.duration)}"
        } else {
            FlatFormatter.timeDisplay(viewModel.state.value.duration)
        }
    }
}
