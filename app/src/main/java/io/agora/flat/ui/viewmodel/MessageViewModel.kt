package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.rtm.Message
import io.agora.flat.common.rtm.MessageFactory
import io.agora.flat.common.rtm.RoomBanEvent
import io.agora.flat.data.repository.MiscRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.event.EventBus
import io.agora.flat.event.MessagesAppended
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.manager.UserManager
import io.agora.flat.ui.util.ObservableLoadingCounter
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val miscRepository: MiscRepository,
    private val messageManager: ChatMessageManager,
    private val syncedClassState: SyncedClassState,
    private val userManager: UserManager,
    private val messageQuery: MessageQuery,
    private val rtmApi: RtmApi,
    private val eventbus: EventBus,
) : ViewModel() {
    val roomUUID: String = savedStateHandle[Constants.IntentKey.ROOM_UUID]!!

    private val _messageUpdate = MutableStateFlow(MessagesUpdate())
    val messageUpdate = _messageUpdate.asStateFlow()

    private val messageLoading = ObservableLoadingCounter()

    val messageUiState = combine(
        syncedClassState.observeClassroomState(),
        messageLoading.observable,
        userManager.observeOwnerUUID(),
    ) { classState, loading, ownerUuid ->
        MessageUiState(
            ban = classState.ban,
            isOwner = ownerUuid == userRepository.getUserUUID(),
            loading = loading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessageUiState(),
    )

    init {
        initMessageQuery()
        loadHistoryMessage()
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
        if (messageUiState.value.loading || !messageQuery.hasMore) {
            return
        }
        viewModelScope.launch(SupervisorJob()) {
            messageLoading.addLoader()
            if (messageManager.isEmpty()) {
                val msgs = messageQuery.loadMore().asReversed()
                appendMessages(msgs)
            } else {
                val msgs = messageQuery.loadMore().asReversed()
                prependMessages(msgs)
            }
            messageLoading.removeLoader()
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

    fun muteChat(muted: Boolean) {
        viewModelScope.launch {
            if (userManager.isOwner()) {
                syncedClassState.updateBan(muted)
                rtmApi.sendChannelCommand(RoomBanEvent(roomUUID = roomUUID, status = muted))
                appendMessages(listOf(MessageFactory.createNotice(ban = muted)))
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

data class MessageUiState(
    val ban: Boolean = false,
    val isOwner: Boolean = false,
    val loading: Boolean = false,
)