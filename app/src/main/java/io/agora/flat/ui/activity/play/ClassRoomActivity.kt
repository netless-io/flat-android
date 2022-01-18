package io.agora.flat.ui.activity.play

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.databinding.ActivityRoomPlayBinding
import io.agora.flat.ui.activity.base.BaseActivity
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class ClassRoomActivity : BaseActivity() {
    private lateinit var binding: ActivityRoomPlayBinding
    private var componentSet: MutableSet<BaseComponent> = mutableSetOf()

    private val viewModel: ClassRoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        componentSet.add(WhiteboardComponent(this, binding.whiteboardContainer, binding.scenePreviewContainer))
        componentSet.add(RtcComponent(this,
            binding.videoListContainer,
            binding.fullVideoContainer,
            binding.shareScreenContainer))
        componentSet.add(RtmComponent(this, binding.messageContainer))
        componentSet.add(ToolComponent(this, binding.toolContainer))
        componentSet.add(ExtComponent(this, binding.extensionContainer))

        componentSet.forEach { lifecycle.addObserver(it) }

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launchWhenStarted {
            viewModel.errorMessage.collect {
                showToast(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableFullScreen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        componentSet.forEach { it.onConfigurationChanged(newConfig) }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onBackPressed() {
        componentSet.forEach {
            if (it.handleBackPressed()) {
                return
            }
        }
        super.onBackPressed()
    }
}