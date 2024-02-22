package io.agora.flat.common.rtc

import org.junit.Test

class AgoraRtcTest {
    @Test
    fun getOverallQuality() {
        assert(AgoraRtc.getOverallQuality(0, 0) == NetworkQuality.Unknown)
        assert(AgoraRtc.getOverallQuality(1, 0) == NetworkQuality.Excellent)
        assert(AgoraRtc.getOverallQuality(2, 0) == NetworkQuality.Good)
        assert(AgoraRtc.getOverallQuality(3, 0) == NetworkQuality.Bad)
        assert(AgoraRtc.getOverallQuality(0, 1) == NetworkQuality.Excellent)
        assert(AgoraRtc.getOverallQuality(0, 2) == NetworkQuality.Good)
        assert(AgoraRtc.getOverallQuality(0, 3) == NetworkQuality.Bad)
        assert(AgoraRtc.getOverallQuality(1, 2) == NetworkQuality.Good)
        assert(AgoraRtc.getOverallQuality(2, 3) == NetworkQuality.Bad)
    }
}