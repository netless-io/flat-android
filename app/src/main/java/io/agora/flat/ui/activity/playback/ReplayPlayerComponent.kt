package io.agora.flat.ui.activity.playback

import android.util.Log
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.agora.netless.syncplayer.*
import com.herewhite.sdk.CommonCallback
import com.herewhite.sdk.Player
import com.herewhite.sdk.WhiteSdk
import com.herewhite.sdk.WhiteSdkConfiguration
import com.herewhite.sdk.domain.*
import io.agora.flat.BuildConfig
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.data.model.RecordItem
import io.agora.flat.databinding.ComponentReplayVideoBinding
import io.agora.flat.databinding.ComponentReplayWhiteboardBinding
import io.agora.flat.ui.activity.play.BaseComponent
import io.agora.flat.ui.viewmodel.ReplayViewModel
import io.agora.flat.util.FlatFormatter
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
        clusterPlayer?.release()
    }

    private fun initView() {
        whiteBinding = ComponentReplayWhiteboardBinding.inflate(activity.layoutInflater, rootView, true)
        videoBinding = ComponentReplayVideoBinding.inflate(activity.layoutInflater, videoLayout, true)

        whiteBinding.playbackSeekBar.max = SEEK_BAR_MAX_PROGRESS
        whiteBinding.playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var targetProgress: Long = -1

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    targetProgress = progress.toLong()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
                targetProgress = -1
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                if (targetProgress != -1L) {
                    clusterPlayer?.seekTo((targetProgress * viewModel.state.value.duration / SEEK_BAR_MAX_PROGRESS))
                }
            }
        })
        whiteBinding.playbackStart.setOnClickListener {
            startPlayer()
        }
        whiteBinding.playbackPause.setOnClickListener {
            pausePlayer()
        }

        videoBinding.videoContainer.layoutParams = videoBinding.videoContainer.layoutParams.also {
            it.width = itemWidth
        }
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
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        val configuration = WhiteSdkConfiguration(Constants.NETLESS_APP_IDENTIFIER, true).apply {
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
                    createVideoPlayer(it.recordInfo.recordInfo);
                    createWhitePlayer(it.recordInfo.whiteboardRoomUUID, it.recordInfo.whiteboardRoomToken)
                    whiteBinding.playbackTime.text = FlatFormatter.timeMS(it.duration)
                }
            }
        }
    }

    private val itemWidth = activity.resources.getDimensionPixelSize(R.dimen.room_replay_video_width) * 17

    private fun createVideoPlayer(recordItem: List<RecordItem>) {
        if (recordItem.isNotEmpty()) {
            val videoPlayer = VideoPlayer(activity, recordItem[0].videoURL)
            videoPlayer.setPlayerContainer(videoBinding.videoContainer)

            videoCombinePlayer = videoPlayer
        }
    }

    private fun createWhitePlayer(roomUUID: String, roomToken: String) {
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
            windowParams.prefersColorScheme = WindowPrefersColorScheme.Auto
        }

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
                            whiteBinding.playbackSeekBar.progress = progress(position)
                            whiteBinding.playbackTime.text = FlatFormatter.timeMS(position)
                            viewModel.updateTime(position)
                        }
                    }

                    override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {}

                    override fun onPhaseChanged(atomPlayer: AtomPlayer, phase: AtomPlayerPhase) {
                        when (phase) {
                            AtomPlayerPhase.Idle -> {}
                            AtomPlayerPhase.Ready -> {}
                            AtomPlayerPhase.Paused -> {}
                            AtomPlayerPhase.Playing -> {}
                            AtomPlayerPhase.Buffering -> {

                            }
                            AtomPlayerPhase.End -> {
                                clusterPlayer?.pause()
                                clusterPlayer?.seekTo(0)
                                setPlaying(false)
                            }
                        }
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

    private fun progress(position: Long, duration: Long = viewModel.state.value.duration): Int {
        val percent = if (duration > 0L) position * 1f / duration else 0f
        return (percent * SEEK_BAR_MAX_PROGRESS).toInt()
    }
}
