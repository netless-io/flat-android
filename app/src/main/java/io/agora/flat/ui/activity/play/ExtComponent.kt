package io.agora.flat.ui.activity.play

import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.error.FlatErrorHandler
import io.agora.flat.common.rtc.NetworkQuality
import io.agora.flat.common.rtc.RtcEvent
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.databinding.ComponentExtensionBinding
import io.agora.flat.databinding.ComponentRoomStateBinding
import io.agora.flat.event.RemoteLoginEvent
import io.agora.flat.event.RoomKickedEvent
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.view.RoomExitDialog
import io.agora.flat.ui.view.TimeStateData
import io.agora.flat.util.delayAndFinish
import io.agora.flat.util.isDarkMode
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * display common loading, toast, dialog, global layout change.
 */
class ExtComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
    private val roomStateContainer: FrameLayout,
) : BaseComponent(activity, rootView) {
    private lateinit var extensionBinding: ComponentExtensionBinding
    private lateinit var roomStateBinding: ComponentRoomStateBinding

    private val viewModel: ExtensionViewModel by activity.viewModels()
    private val classRoomViewModel: ClassRoomViewModel by activity.viewModels()

    override fun onCreate(owner: LifecycleOwner) {
        initView()
        observeState()
    }

    private fun initView() {
        extensionBinding = ComponentExtensionBinding.inflate(activity.layoutInflater, rootView, true)
        roomStateBinding = ComponentRoomStateBinding.inflate(activity.layoutInflater, roomStateContainer, true)
    }

    private fun observeState() {
        lifecycleScope.launchWhenCreated {
            classRoomViewModel.loginThirdParty()
        }

        lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                showLoading(it.loading)
                it.error?.run {
                    handleErrorMessage(it.error)
                    viewModel.clearError()
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            classRoomViewModel.state.filterNotNull().collect {
                if (it.roomStatus == RoomStatus.Stopped) {
                    showRoomExitDialog(activity.getString(R.string.exit_room_stopped_message))
                }

                // TODO current version does not have an end time limit
                roomStateBinding.timeStateLayout.updateTimeStateData(
                    TimeStateData(
                        it.beginTime,
                        Long.MAX_VALUE,
                        10 * 60 * 1000,
                    )
                )
            }
        }

        lifecycleScope.launchWhenResumed {
            classRoomViewModel.classroomEvent.collect { event ->
                when (event) {
                    RemoteLoginEvent -> {
                        showRoomExitDialog(activity.getString(R.string.exit_remote_login_message))
                    }

                    RoomKickedEvent -> {
                        showRoomExitDialog(activity.getString(R.string.exit_room_stopped_message))
                    }

                    else -> {}
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            classRoomViewModel.rtcEvent.collect { event ->
                when (event) {
                    is RtcEvent.NetworkStatus -> {
                        when (event.quality) {
                            NetworkQuality.Unknown, NetworkQuality.Excellent -> {
                                roomStateBinding.networkStateIcon.setColorFilter(ContextCompat.getColor(activity, R.color.flat_green_6))
                            }
                            NetworkQuality.Good -> {
                                roomStateBinding.networkStateIcon.setColorFilter(ContextCompat.getColor(activity, R.color.flat_yellow_6))
                            }
                            NetworkQuality.Bad-> {
                                roomStateBinding.networkStateIcon.setColorFilter(ContextCompat.getColor(activity, R.color.flat_red_6))
                            }
                        }
                    }

                    is RtcEvent.LastmileDelay -> {
                        roomStateBinding.networkDelay.text = activity.getString(R.string.room_class_network_delay, event.delay)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    private fun handleErrorMessage(error: UiMessage) {
        if (error.exception == null) {
            activity.showToast(error.text)
        } else {
            showRoomExitDialog(FlatErrorHandler.getErrorStr(activity, error.exception))
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
                putString(Constants.IntentKey.MESSAGE, message)
            }
        }
        dialog.setListener { activity.delayAndFinish(250) }
        dialog.show(activity.supportFragmentManager, "RoomExitDialog")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // clear overlay when activity destroy
        RoomOverlayManager.setShown(RoomOverlayManager.AREA_ID_NO_OVERLAY, false)
    }
}
