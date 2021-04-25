package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.di.interfaces.RtcEngineProvider
import javax.inject.Inject

@HiltViewModel
class RtcVideoController @Inject constructor(
    private val rtcEngineProvider: RtcEngineProvider,
) : ViewModel() {

}