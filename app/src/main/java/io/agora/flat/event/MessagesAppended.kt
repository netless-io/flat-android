package io.agora.flat.event

import io.agora.flat.common.rtm.Message

data class MessagesAppended constructor(val messages: List<Message>) : Event()