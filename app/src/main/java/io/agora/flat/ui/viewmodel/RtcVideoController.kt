package io.agora.flat.ui.viewmodel

import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.di.interfaces.RtcApi
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import javax.inject.Inject
import kotlin.collections.set

@ActivityRetainedScoped
class RtcVideoController @Inject constructor(private val rtcApi: RtcApi) {
    private var textureMap = HashMap<Int, TextureView>()
    var fullScreenUid: Int = 0

    var shareScreenContainer: FrameLayout? = null
    var localUid: Int = 0
    var shareScreenUid: Int = 0

    fun setupUid(uid: Int, ssUid: Int) {
        localUid = uid
        shareScreenUid = ssUid
    }

    fun enterFullScreen(uid: Int) {
        this.fullScreenUid = uid
    }

    fun exitFullScreen() {
        fullScreenUid = 0
    }

    fun updateFullScreenVideo(videoContainer: FrameLayout, uid: Int) {
        if (fullScreenUid == uid) {
            setupUserVideo(videoContainer, uid)
        }
    }

    fun setupUserVideo(container: FrameLayout, uid: Int) {
        if (textureMap[uid] == null) {
            textureMap[uid] = RtcEngine.CreateTextureView(container.context)
        }

        val textureView = textureMap[uid]!!
        if (textureView.parent == container) {
            setupVideo(textureView, uid)
        } else {
            (textureView.parent as? FrameLayout)?.removeAllViews()

            with(container) {
                removeAllViews()
                addView(textureView, generateLayoutParams())
                setupVideo(textureView, uid)
            }
        }
    }

    private fun generateLayoutParams() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    private fun releaseVideo(uid: Int) {
        setupVideoByVideoCanvas(uid, VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun setupVideo(textureView: View, uid: Int) {
        setupVideoByVideoCanvas(uid, VideoCanvas(textureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun setupVideoByVideoCanvas(uid: Int, videoCanvas: VideoCanvas) {
        if (uid == localUid) {
            rtcApi.setupLocalVideo(videoCanvas)
        } else {
            rtcApi.setupRemoteVideo(videoCanvas)
        }
    }

    fun handleOffline(uid: Int) {
        releaseVideo(uid)
        if (uid == shareScreenUid) {
            shareScreenContainer?.isVisible = false
        }
    }

    fun handlerJoined(uid: Int) {
        if (uid == shareScreenUid) {
            shareScreenContainer?.run {
                setupUserVideo(this, uid)
                isVisible = true
            }
        }
    }
}