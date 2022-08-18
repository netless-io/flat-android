package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.rtm.Message
import io.agora.flat.common.rtm.MessageFactory
import io.agora.flat.data.repository.MiscRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.impl.EventBus
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.event.MessagesAppended
import io.agora.flat.ui.manager.RoomErrorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val miscRepository: MiscRepository,
    private val messageManager: ChatMessageManager,
    private val errorManager: RoomErrorManager,
    private val messageQuery: MessageQuery,
    private val rtmApi: RtmApi,
    private val eventbus: EventBus,
) : ViewModel() {
    val roomUUID: String = savedStateHandle.get(Constants.IntentKey.ROOM_UUID)!!

    private val _messageUpdate = MutableStateFlow(MessagesUpdate())
    val messageUpdate = _messageUpdate.asStateFlow()

    private var _messageLoading = MutableStateFlow(false)
    val messageLoading = _messageLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // TODO
            try {
                initMessageQuery()
                loadHistoryMessage()
            } catch (e: Exception) {
                errorManager.notifyError("fetch message error", e)
            }
        }

        viewModelScope.launch {
            eventbus.events.filterIsInstance<MessagesAppended>().collect {
                appendMessages(it.messages)
            }
        }
    }

    private fun initMessageQuery() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 3600_000 * 24
        messageQuery.update(roomUUID, startTime, endTime, false)
    }

    fun loadHistoryMessage() {
        if (messageLoading.value || !messageQuery.hasMore) {
            return
        }
        viewModelScope.launch {
            _messageLoading.value = true
            if (messageManager.isEmpty()) {
                val msgs = messageQuery.loadMore().asReversed()
                appendMessages(msgs)
            } else {
                val msgs = messageQuery.loadMore().asReversed()
                prependMessages(msgs)
            }
            _messageLoading.value = false
        }
    }

    private fun appendMessages(msgs: List<Message>) {
        messageManager.appendMessages(msgs)

        _messageUpdate.value = _messageUpdate.value.copy(
            updateOp = MessagesUpdate.APPEND,
            messages = msgs,
        )
    }

    private fun prependMessages(msgs: List<Message>) {
        messageManager.prependMessages(msgs)

        _messageUpdate.value = _messageUpdate.value.copy(
            updateOp = MessagesUpdate.PREPEND,
            messages = msgs
        )
    }

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            if (miscRepository.censorRtm(message)) {
                rtmApi.sendChannelMessage(message)
                appendMessages(listOf(MessageFactory.createText(userRepository.getUserUUID(), message)))
            }
        }
    }
}

data class MessagesUpdate(
    val updateOp: Int = IDLE,
    val messages: List<Message> = listOf(),
) {
    companion object {
        const val IDLE = 0
        const val APPEND = 1
        const val PREPEND = 2
    }
}