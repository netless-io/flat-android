package io.agora.flat.ui.activity.play

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityRoomPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableFullScreen()

        componentSet.add(WhiteboardComponent(this, binding.whiteboardContainer, binding.scenePreviewContainer))
        componentSet.add(RtcComponent(this,
            binding.videoListContainer,
            binding.fullVideoContainer,
            binding.shareScreenContainer))
        componentSet.add(RtmComponent(this, binding.messageContainer))
        componentSet.add(ToolComponent(this, binding.toolContainer))

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

    private fun enableFullScreen() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.hide(WindowInsetsCompat.Type.statusBars())
        // Some oneplus, huawei devices rely on this line of code for full screen
        controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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