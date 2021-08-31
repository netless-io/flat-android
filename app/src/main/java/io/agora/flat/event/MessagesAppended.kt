package io.agora.flat.event

import io.agora.flat.common.rtm.Message
import io.agora.flat.di.impl.Event

data class MessagesAppended constructor(val messages: List<Message>) : Event()