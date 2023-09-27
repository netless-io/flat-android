package io.agora.flat.ui.activity.playback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.model.RecordInfo
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.ui.util.UiErrorMessage
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cloudRecordRepository: CloudRecordRepository,
) : ViewModel() {
    private val messageManager = UiMessageManager()

    private val roomUUID: String = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)!!

    private var _state = MutableStateFlow(ReplayUiState(roomUUID = roomUUID))
    val state: StateFlow<ReplayUiState>
        get() = _state

    init {
        viewModelScope.launch {
            cloudRecordRepository.getRecordInfo(roomUUID)
                .onSuccess {
                    _state.value = state.value.copy(recordInfo = it)
                }.onFailure {
                    messageManager.emitMessage(UiErrorMessage(it))
                }
        }

        viewModelScope.launch {
            messageManager.message.collect {
                _state.value = state.value.copy(message = it)
            }
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            messageManager.clearMessage(id)
        }
    }
}

data class ReplayUiState(
    val roomUUID: String,
    val recordInfo: RecordInfo? = null,
    val message: UiMessage? = null
) {
    val duration: Long
        get() = recordInfo?.recordInfo?.run {
            this.last().endTime - this.first().beginTime
        } ?: 0L

    val beginTime: Long
        get() = (recordInfo?.recordInfo?.first()?.beginTime) ?: 0L
}