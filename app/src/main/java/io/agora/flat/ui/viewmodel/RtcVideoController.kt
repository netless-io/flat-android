package io.agora.flat.ui.viewmodel

import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import javax.inject.Inject
import kotlin.collections.set

@ActivityRetainedScoped
class RtcVideoController @Inject constructor(private val rtcApi: RtcEngineProvider) {
    private var uidTextureMap = HashMap<Int, TextureView>()
    private var fullScreenUid: Int = 0

    var shareScreenContainer: FrameLayout? = null
    var localUid: Int = 0
    var shareScreenUid: Int = 0

    fun enterFullScreen(uid: Int) {
        this.fullScreenUid = uid
    }

    fun exitFullScreen() {
        fullScreenUid = 0
    }

    fun setupFullscreenVideo(videoContainer: FrameLayout, uid: Int) {
        if (fullScreenUid == uid) {
            setupUserVideo(videoContainer, uid)
        }
    }

    fun setupUserVideo(videoContainer: FrameLayout, uid: Int) {
        if (uidTextureMap[uid] == null) {
            uidTextureMap[uid] = RtcEngine.CreateTextureView(videoContainer.context)
        } else {
            if (uidTextureMap[uid]!!.parent == videoContainer) {
                setupVideo(uidTextureMap[uid]!!, uid)
                return
            }

            uidTextureMap[uid]!!.parent?.run {
                this as FrameLayout
                removeAllViews()
            }
        }
        videoContainer.run {
            if (childCount >= 1) {
                removeAllViews()
            }

            val textureView = uidTextureMap[uid]!!
            addView(textureView, generateLayoutParams())
            setupVideo(textureView, uid)
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

    private fun setupVideoByVideoCanvas(uid: Int, videoCanvas: VideoCanvas?) {
        with(rtcApi.rtcEngine()) {
            if (uid == localUid) {
                setupLocalVideo(videoCanvas)
            } else {
                setupRemoteVideo(videoCanvas)
            }
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