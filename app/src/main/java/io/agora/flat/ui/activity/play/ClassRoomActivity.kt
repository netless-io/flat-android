package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.databinding.ActivityRoomPlayBinding

@AndroidEntryPoint
class ClassRoomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoomPlayBinding
    private var componentSet: MutableSet<BaseComponent> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSystemUI()

        componentSet.add(WhiteboardComponent(this, binding.whiteboardContainer, binding.scenePreviewContainer))
        componentSet.add(RtcComponent(this, binding.videoListContainer, binding.fullVideoContainer))
        componentSet.add(RtmComponent(this, binding.messageContainer))
        componentSet.add(ToolComponent(this, binding.extToolContainer))

        componentSet.forEach { lifecycle.addObserver(it) }
    }

    private fun initSystemUI() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}