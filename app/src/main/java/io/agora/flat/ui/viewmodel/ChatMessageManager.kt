package io.agora.flat.ui.viewmodel

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.common.rtm.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * a class to manage chat messages in classroom.
 */
@ActivityRetainedScoped
class ChatMessageManager @Inject constructor() {
    private var _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun isEmpty(): Boolean {
        return _messages.value.isEmpty()
    }

    fun appendMessages(msgs: List<Message>) {
        val list = _messages.value.toMutableList().apply {
            addAll(msgs)
        }
        _messages.value = list
    }

    fun prependMessages(msgs: List<Message>) {
        val list = _messages.value.toMutableList().apply {
            addAll(0, msgs)
        }
        _messages.value = list
    }
}