package io.agora.flat.event

import io.agora.flat.di.interfaces.Event
import io.agora.flat.ui.viewmodel.RTMMessage

class RTMMessageEvent constructor(val messages: List<RTMMessage>) : Event() {
}