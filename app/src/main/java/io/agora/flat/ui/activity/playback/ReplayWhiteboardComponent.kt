package io.agora.flat.ui.activity.playback

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.herewhite.sdk.*
import com.herewhite.sdk.combinePlayer.PlayerSyncManager
import com.herewhite.sdk.domain.*
import io.agora.flat.BuildConfig
import io.agora.flat.Constants
import io.agora.flat.data.model.RecordItem
import io.agora.flat.databinding.ComponentReplayVideoBinding
import io.agora.flat.databinding.ComponentReplayWhiteboardBinding
import io.agora.flat.ui.activity.play.BaseComponent
import io.agora.flat.ui.activity.play.WhiteboardComponent
import io.agora.flat.ui.viewmodel.ReplayViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class ReplayWhiteboardComponent(
    activity: ReplayActivity,
    rootView: FrameLayout,
    private val videoLayout: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = ReplayWhiteboardComponent::class.simpleName
    }

    private lateinit var whiteBinding: ComponentReplayWhiteboardBinding
    private lateinit var videoBinding: ComponentReplayVideoBinding
    private lateinit var whiteSdk: WhiteSdk

    private val viewModel: ReplayViewModel by activity.viewModels()
    private var player: Player? = null
    private var videoplayer: RtcVideoExoPlayer? = null
    private var playerSyncManager: PlayerSyncManager? = null

    private var mSeekBarUpdateHandler: Handler = Handler(Looper.getMainLooper())
    private var mUpdateSeekBar: Runnable = object : Runnable {
        override fun run() {
            val progress: Float = player?.playerTimeInfo?.progress() ?: 0f
            whiteBinding.playbackSeekBar.progress = (progress * 10).toInt()
            Log.v(TAG, "progress: $progress")
            mSeekBarUpdateHandler.postDelayed(this, 500)
        }
    }

    private fun PlayerTimeInfo.progress(): Float {
        if (timeDuration == 0L) {
            return 0f
        }
        return scheduleTime * 100f / timeDuration
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        initView()
        initWhiteboard()
        observeData()

        mSeekBarUpdateHandler.post(mUpdateSeekBar)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mSeekBarUpdateHandler.removeCallbacks(mUpdateSeekBar)
    }

    private fun initView() {
        whiteBinding = ComponentReplayWhiteboardBinding.inflate(activity.layoutInflater, rootView, true)
        videoBinding = ComponentReplayVideoBinding.inflate(activity.layoutInflater, videoLayout, true)

        whiteBinding.playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val timeDuration = player?.playerTimeInfo?.timeDuration ?: 0
                    playerSyncManager?.seek((progress * timeDuration * 1f / 1000).toLong(), TimeUnit.MILLISECONDS)
                    whiteBinding.playbackSeekBar.progress = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        whiteBinding.playbackSeekBar.max = 1000

        whiteBinding.playbackStart.setOnClickListener {
            playerSyncManager?.play()

            whiteBinding.playbackStart.isVisible = false
            whiteBinding.playbackPause.isVisible = true
        }
        whiteBinding.playbackPause.setOnClickListener {
            playerSyncManager?.pause()

            whiteBinding.playbackStart.isVisible = true
            whiteBinding.playbackPause.isVisible = false
        }
    }

    private fun initWhiteboard() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        val configuration = WhiteSdkConfiguration(Constants.NETLESS_APP_IDENTIFIER, true)
        configuration.isUserCursor = true

        whiteSdk = WhiteSdk(whiteBinding.whiteboardView, activity, configuration)
        whiteSdk.setCommonCallbacks(object : CommonCallback {
            override fun urlInterrupter(sourceUrl: String): String {
                return sourceUrl
            }

            override fun onMessage(message: JSONObject) {
                Log.d(WhiteboardComponent.TAG, message.toString())
            }

            override fun sdkSetupFail(error: SDKError) {
                Log.e(WhiteboardComponent.TAG, "sdkSetupFail $error")
            }

            override fun throwError(args: Any) {
                Log.e(WhiteboardComponent.TAG, "throwError $args")
            }

            override fun onPPTMediaPlay() {
                Log.d(WhiteboardComponent.TAG, "onPPTMediaPlay")
            }

            override fun onPPTMediaPause() {
                Log.d(WhiteboardComponent.TAG, "onPPTMediaPause")
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.state.collect {
                if (it.recordInfo != null && it.roomInfo != null) {
                    createVideoPlayer(it.roomInfo.beginTime, it.recordInfo.recordInfo)
                    createWhitePlayer(it.recordInfo.whiteboardRoomUUID, it.recordInfo.whiteboardRoomToken)
                }
            }
        }
    }

    private fun createVideoPlayer(startTime: Long, recordInfo: List<RecordItem>) {
        val records = recordInfo.map {
            it.copy(beginTime = it.beginTime - startTime, endTime = it.endTime - startTime)
        }
        videoplayer = RtcVideoExoPlayer(activity, records)
        videoplayer?.setPlayerView(videoBinding.videoView)
    }

    private fun createWhitePlayer(roomUUID: String, roomToken: String) {
        val conf = PlayerConfiguration(roomUUID, roomToken)
        val playerListener = object : PlayerListener {
            override fun onPhaseChanged(phase: PlayerPhase) {
                Log.d(TAG, "play phase $phase")
                playerSyncManager?.updateWhitePlayerPhase(phase);
            }

            override fun onLoadFirstFrame() {
                Log.d(TAG, "onLoadFirstFrame call")
            }

            override fun onSliceChanged(slice: String) {
                Log.d(TAG, "onSliceChanged called $slice")
            }

            override fun onPlayerStateChanged(modifyState: PlayerState) {
                Log.d(TAG, "onPlayerStateChanged called $modifyState")
            }

            override fun onStoppedWithError(error: SDKError) {
                Log.d(TAG, "onStoppedWithError called $error")
            }

            override fun onScheduleTimeChanged(time: Long) {
                // Log.d(TAG, "onScheduleTimeChanged called $time")
            }

            override fun onCatchErrorWhenAppendFrame(error: SDKError) {
                Log.d(TAG, "onCatchErrorWhenAppendFrame called $error")
            }

            override fun onCatchErrorWhenRender(error: SDKError) {
                Log.d(TAG, "onCatchErrorWhenRender called $error")
            }
        }
        whiteSdk.createPlayer(conf, playerListener, object : Promise<Player> {
            override fun then(player: Player) {
                this@ReplayWhiteboardComponent.player = player

                playerSyncManager = PlayerSyncManager(player, videoplayer, object : PlayerSyncManager.Callbacks {
                    override fun startBuffering() {
                        Log.d(TAG, "startBuffering: ")
                    }

                    override fun endBuffering() {
                        Log.d(TAG, "endBuffering: ")
                    }
                })
                videoplayer!!.bindPlayerSyncManager(playerSyncManager)
                player.getPlayerTimeInfo(object : Promise<PlayerTimeInfo> {
                    override fun then(info: PlayerTimeInfo) {
                        whiteBinding.playbackTime.text =
                            "${info.timeDuration / 60_000}:${(info.timeDuration / 1000) % 60}"
                    }

                    override fun catchEx(t: SDKError?) {

                    }
                })
            }

            override fun catchEx(error: SDKError) {
                Log.e(TAG, "createPlayer error: $error")
                // handle create error
            }
        })
    }
}
