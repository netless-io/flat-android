package io.agora.flat.ui.viewmodel

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 页面内同步
 */
@ActivityRetainedScoped
class MessageState @Inject constructor() {
    private var _messages = MutableStateFlow<List<RTMMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    fun isEmpty(): Boolean {
        return _messages.value.isEmpty()
    }

    fun appendMessages(msgs: List<RTMMessage>) {
        val list = _messages.value.toMutableList().apply {
            addAll(msgs)
        }
        _messages.value = list;
    }

    fun prependMessages(msgs: List<RTMMessage>) {
        val list = _messages.value.toMutableList().apply {
            addAll(0, msgs)
        }
        _messages.value = list;
    }
}