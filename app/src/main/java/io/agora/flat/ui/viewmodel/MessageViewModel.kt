package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.EventBus
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.event.RTMMessageEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val messageState: MessageState,
    private val rtmApi: RtmEngineProvider,
    private val eventbus: EventBus,
) : ViewModel() {
    val roomUUID: String = savedStateHandle.get(Constants.IntentKey.ROOM_UUID)!!

    private val _messageUpdate = MutableStateFlow(MessagesUpdate())
    val messageUpdate = _messageUpdate.asStateFlow()

    private var _messageLoading = MutableStateFlow(false)
    val messageLoading = _messageLoading.asStateFlow()

    private lateinit var messageQuery: MessageQuery

    init {
        viewModelScope.launch {
            loadHistoryMessage()
        }

        viewModelScope.launch {
            eventbus.events.filterIsInstance<RTMMessageEvent>().collect {
                appendMessages(it.messages)
            }
        }
    }

    fun loadHistoryMessage() {
        ensureMessageQuery()
        if (messageLoading.value || !messageQuery.hasMoreMessage()) {
            return
        }
        viewModelScope.launch {
            _messageLoading.value = true
            if (messageState.isEmpty()) {
                val msgs = messageQuery.loadMore().asReversed()
                appendMessages(msgs)
            } else {
                val msgs = messageQuery.loadMore().asReversed()
                prependMessages(msgs)
            }
            _messageLoading.value = false
        }
    }

    private fun ensureMessageQuery() {
        if (!this::messageQuery.isInitialized) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 3600_000 * 24
            val userQuery = UserQuery(roomUUID, userRepository, roomRepository)
            messageQuery = MessageQuery(roomUUID, startTime, endTime, rtmApi, userQuery, false)
        }
    }

    private fun appendMessages(msgs: List<RTMMessage>) {
        messageState.appendMessages(msgs)

        _messageUpdate.value = _messageUpdate.value.copy(
            updateOp = MessagesUpdate.APPEND,
            messages = msgs,
        )
    }

    private fun prependMessages(msgs: List<RTMMessage>) {
        messageState.prependMessages( msgs)

        _messageUpdate.value = _messageUpdate.value.copy(
            updateOp = MessagesUpdate.PREPEND,
            messages = msgs
        )
    }

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            rtmApi.sendChannelMessage(message)
            appendMessages(listOf(ChatMessage(name = userRepository.getUserUUID(), message = message, isSelf = true)))
        }
    }
}

data class MessagesUpdate(
    val updateOp: Int = IDLE,
    val messages: List<RTMMessage> = listOf(),
) {
    companion object {
        const val IDLE = 0
        const val APPEND = 1
        const val PREPEND = 2
    }
}