package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.databinding.ComponentToolBinding


@AndroidEntryPoint
class ClassRoomActivity : AppCompatActivity() {
    private lateinit var whiteboardRoot: FrameLayout
    private lateinit var rtcRoot: FrameLayout
    private lateinit var rtmRoot: FrameLayout
    private lateinit var fullVideoRoot: FrameLayout
    private lateinit var extToolRoot: FrameLayout

    private var componentSet: MutableSet<BaseComponent> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_play)

        initSystemUI()

        whiteboardRoot = findViewById(R.id.whiteboardContainer)
        rtcRoot = findViewById(R.id.userVideoContainer)
        rtmRoot = findViewById(R.id.messageContainer)
        fullVideoRoot = findViewById(R.id.fullVideoContainer)
        extToolRoot = findViewById(R.id.extToolContainer)

        componentSet.add(WhiteboardComponent(this, whiteboardRoot))
        componentSet.add(RtcComponent(this, rtcRoot, fullVideoRoot))
        componentSet.add(RtmComponent(this, rtmRoot))
        componentSet.add(ToolComponent(this, extToolRoot))

        componentSet.forEach { lifecycle.addObserver(it) }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun initSystemUI() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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