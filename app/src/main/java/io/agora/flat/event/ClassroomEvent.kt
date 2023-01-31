package io.agora.flat.event

sealed class ClassroomEvent : Event()

data class RequestDeviceSent(
    val mic: Boolean? = null,
    val camera: Boolean? = null
) : ClassroomEvent()

data class RequestDeviceReceived(
    val mic: Boolean? = null,
    val camera: Boolean? = null
) : ClassroomEvent()

data class RequestDeviceResponseReceived(
    val username: String,
    val mic: Boolean? = null,
    val camera: Boolean? = null
) : ClassroomEvent()

data class NotifyDeviceOffReceived(
    val mic: Boolean? = null,
    val camera: Boolean? = null,
) : ClassroomEvent()


