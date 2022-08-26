package io.agora.flat.ui.activity.play

import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import io.agora.flat.R
import io.agora.flat.common.error.FlatErrorHandler
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.databinding.ComponentExtensionBinding
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.view.RoomExitDialog
import io.agora.flat.util.delayAndFinish
import io.agora.flat.util.isDarkMode
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.filterNotNull

/**
 * display common loading, toast, dialog, global layout change.
 */
class ExtComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    private lateinit var extensionBinding: ComponentExtensionBinding

    private val viewModel: ExtensionViewModel by activity.viewModels()
    private val classRoomViewModel: ClassRoomViewModel by activity.viewModels()

    override fun onCreate(owner: LifecycleOwner) {
        initView()
        observeState()
    }

    private fun initView() {
        extensionBinding = ComponentExtensionBinding.inflate(activity.layoutInflater, rootView, true)
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                showLoading(it.loading)
                it.error?.run {
                    handleErrorMessage(it.error)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            classRoomViewModel.state.filterNotNull().collect {
                if (it.roomStatus == RoomStatus.Stopped) {
                    showRoomExitDialog(activity.getString(R.string.exit_room_stopped_message))
                }
            }
        }


        lifecycleScope.launchWhenCreated {
            classRoomViewModel.loginThirdParty()
        }
    }

    private fun handleErrorMessage(error: UiMessage) {
        if (error.exception == null) {
            activity.showToast(error.text)
        } else {
            // TODO
            showRoomExitDialog(FlatErrorHandler.getStringByError(activity, error.exception, ""))
        }
    }

    private fun showLoading(show: Boolean) {
        extensionBinding.loadingLayout.isVisible = show
        if (show) {
            extensionBinding.loadingView.load(
                if (activity.isDarkMode()) R.raw.loading_dark else R.raw.loading_light,
                gifImageLoader,
            ) {
                crossfade(true)
            }
        }
    }

    private val gifImageLoader = ImageLoader.Builder(activity).apply {
        componentRegistry {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder(activity))
            } else {
                add(GifDecoder())
            }
        }
    }.build()

    private fun showRoomExitDialog(message: String) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        val dialog = RoomExitDialog().apply {
            arguments = Bundle().apply {
                putString(RoomExitDialog.MESSAGE, message)
            }
        }
        dialog.setListener { activity.delayAndFinish(250) }
        dialog.show(activity.supportFragmentManager, "RoomExitDialog")
    }
}
