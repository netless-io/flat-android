package io.agora.flat.common.rtc

data class AudioVolumeInfo(
    val uid: Int,
    val volume: Int,
    val vad: Int,
)


enum class NetworkQuality {
    Excellent,
    Good,
    Bad,
    Unknown
}

sealed class RtcEvent {
    data class UserOffline(val uid: Int, val reason: Int) : RtcEvent()

    data class UserJoined(val uid: Int, val elapsed: Int) : RtcEvent()

    class VolumeIndication(val speakers: List<AudioVolumeInfo>, val totalVolume: Int) : RtcEvent()

    class NetworkStatus(val quality: NetworkQuality) : RtcEvent()

    class LastmileDelay(val delay: Int) : RtcEvent()

}