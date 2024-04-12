package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.rtc.AgoraRtc
import io.agora.flat.di.interfaces.RtcApi
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.IRtcEngineEventHandler.LastmileProbeResult
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.internal.LastmileProbeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * RTC Test
 */
@HiltViewModel
class CallTestViewModel @Inject constructor(
    rtcApi: RtcApi,
) : ViewModel() {
    private val _state = MutableStateFlow(CallTestState())
    val state: StateFlow<CallTestState>
        get() = _state

    var rtcEngine: RtcEngine = (rtcApi as AgoraRtc).rtcEngine()

    private val eventListener: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onLastmileQuality(quality: Int) {
            _state.value = _state.value.copy(quality = quality)
        }

        override fun onLastmileProbeResult(lastMileProbeResult: LastmileProbeResult) {
            rtcEngine.stopLastmileProbeTest()

            _state.value = _state.value.copy(lastMileProbeResult = lastMileProbeResult)
        }
    }

    init {
        rtcEngine.addHandler(eventListener)
    }

    fun startEchoTest() {
        rtcEngine.startEchoTest(5)
        _state.value = _state.value.copy(echoStarted = true)
    }

    fun stopEchoTest() {
        rtcEngine.stopEchoTest()
        _state.value = _state.value.copy(echoStarted = false)
    }

    fun startLastmileTest() {
        val config: LastmileProbeConfig = LastmileProbeConfig().apply {
            probeUplink = true
            probeDownlink = true
            expectedUplinkBitrate = 100000
            expectedDownlinkBitrate = 100000
        }
        rtcEngine.startLastmileProbeTest(config)
    }

    override fun onCleared() {
        rtcEngine.removeHandler(eventListener)
    }
}

data class CallTestState(
    val quality: Int? = null,
    val lastMileProbeResult: LastmileProbeResult? = null,
    val echoStarted: Boolean = false,
) {
    fun getLastMileResult(): String? {
        if (lastMileProbeResult == null) return null
        val stringBuilder = StringBuilder()
        stringBuilder.append("Rtt: ")
            .append(lastMileProbeResult.rtt)
            .append("ms")
            .append("\n")
            .append("DownlinkAvailableBandwidth: ")
            .append(lastMileProbeResult.downlinkReport.availableBandwidth)
            .append("Kbps")
            .append("\n")
            .append("DownlinkJitter: ")
            .append(lastMileProbeResult.downlinkReport.jitter)
            .append("ms")
            .append("\n")
            .append("DownlinkLoss: ")
            .append(lastMileProbeResult.downlinkReport.packetLossRate)
            .append("%")
            .append("\n")
            .append("UplinkAvailableBandwidth: ")
            .append(lastMileProbeResult.uplinkReport.availableBandwidth)
            .append("Kbps")
            .append("\n")
            .append("UplinkJitter: ")
            .append(lastMileProbeResult.uplinkReport.jitter)
            .append("ms")
            .append("\n")
            .append("UplinkLoss: ")
            .append(lastMileProbeResult.uplinkReport.packetLossRate)
            .append("%")
        return stringBuilder.toString()
    }
}