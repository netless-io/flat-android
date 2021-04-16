package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R

@AndroidEntryPoint
class ClassRoomActivity : AppCompatActivity() {
    private lateinit var whiteboardRoot: FrameLayout
    private lateinit var rtcRoot: FrameLayout
    private lateinit var rtmRoot: FrameLayout

    private var componentSet: MutableSet<BaseComponent> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        setContentView(R.layout.activity_room_play)
        supportActionBar?.hide()

        whiteboardRoot = findViewById(R.id.whiteboardContainer)
        rtcRoot = findViewById(R.id.userVideoContainer)
        rtmRoot = findViewById(R.id.userVideoContainer)

        componentSet.add(WhiteboardComponent(this, whiteboardRoot))
        componentSet.add(RtcComponent(this, rtcRoot))
        componentSet.add(RtmComponent(this, rtmRoot))

        componentSet.forEach { lifecycle.addObserver(it) }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        // or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}