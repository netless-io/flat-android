package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.herewhite.sdk.WhiteboardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import io.agora.flat.R
import io.agora.flat.ui.viewmodel.ClassRoomViewModel

@AndroidEntryPoint
class ClassRoomActivity : AppCompatActivity() {
    private val viewModel: ClassRoomViewModel by viewModels()

    private lateinit var whiteboardComponent: WhiteboardComponent
    private lateinit var whiteboard: WhiteboardView

    private lateinit var rtcRoot: FrameLayout
    private lateinit var rtcComponent: RtcComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        setContentView(R.layout.activity_room_play)
        supportActionBar?.hide()

        whiteboard = findViewById(R.id.whiteboard)
        whiteboardComponent = WhiteboardComponent(this, whiteboard)

        rtcRoot = findViewById(R.id.userVideoLayout)
        rtcComponent = RtcComponent(this, rtcRoot)

        lifecycleScope.launch {
            viewModel.roomPlayInfo.collect {
                it?.apply {
                    whiteboardComponent.join(whiteboardRoomUUID, whiteboardRoomToken)
                    rtcComponent.enterChannel(
                        rtcUID = rtcUID,
                        rtcToken = rtcToken,
                        uuid = roomUUID,
                        rtmToken = rtmToken
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        whiteboardComponent.onActivityDestroy()
        rtcComponent.onActivityDestroy()
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