package io.agora.flat.event

import io.agora.flat.common.message.Message
import io.agora.flat.di.interfaces.Event

data class RTMMessageEvent constructor(val messages: List<Message>) : Event()