package io.agora.flat.ui.viewmodel

import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class RtcVideoController @Inject constructor(
    private val rtcApi: RtcEngineProvider,
) : ViewModel() {
    private var _state = MutableStateFlow(0)
    val state = _state.asStateFlow()

    private var uidTextureMap = HashMap<Int, TextureView>()
    private var fullScreenUid: Int = 0
    private var localUid: Int = 0

    fun setLocalUid(localUid: Int) {
        this.localUid = localUid;
    }

    fun enterFullScreen(uid: Int) {
        this.fullScreenUid = uid
    }

    fun exitFullScreen() {
        fullScreenUid = 0
    }

    fun setupUserVideo(videoContainer: FrameLayout, uid: Int, fullscreen: Boolean = false) {
        if (fullScreenUid == uid && !fullscreen) {
            return
        }
        if (uidTextureMap[uid] == null) {
            uidTextureMap[uid] = RtcEngine.CreateTextureView(videoContainer.context)
        } else {
            val parent = uidTextureMap[uid]!!.parent as FrameLayout
            parent.removeAllViews()
        }
        videoContainer.apply {
            if (childCount >= 1) {
                removeAllViews()
            }

            val textureView = uidTextureMap[uid] as TextureView
            addView(
                textureView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            setupVideo(textureView, uid)
        }
    }

    fun releaseVideo(uid: Int) {
        setupVideoByVideoCanvas(uid, VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun setupVideo(textureView: View, uid: Int) {
        setupVideoByVideoCanvas(uid, VideoCanvas(textureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun setupVideoByVideoCanvas(uid: Int, videoCanvas: VideoCanvas) {
        with(rtcApi.rtcEngine()) {
            if (uid == localUid) {
                setupLocalVideo(videoCanvas)
            } else {
                setupRemoteVideo(videoCanvas)
            }
        }
    }
}