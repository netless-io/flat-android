package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.android.ClipboardController
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.ui.util.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val roomConfigRepository: RoomConfigRepository,
    private val roomRepository: RoomRepository,
    private val clipboard: ClipboardController,
) : ViewModel() {
    companion object {
        const val ROOM_UUID_PATTERN = """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""
    }

    val roomUUID = MutableStateFlow("")
    val roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val error = MutableStateFlow<UiError?>(null)

    fun joinRoom(roomUUID: String, openVideo: Boolean, openAudio: Boolean) {
        viewModelScope.launch {
            roomConfigRepository.updateRoomConfig(RoomConfig(roomUUID, openVideo, openAudio))

            when (val result = roomRepository.joinRoom(roomUUID)) {
                is Success -> roomPlayInfo.value = result.data
                is ErrorResult -> {
                    error.value = when (result.error.code) {
                        FlatErrorCode.Web_RoomNotFound -> UiError("room not found")
                        FlatErrorCode.Web_RoomIsEnded -> UiError(" room has been ended")
                        else -> UiError("join room error ${result.error.code}")
                    }
                }
            }
        }
    }

    fun checkClipboardText() {
        val ct = currentClipboardText()
        if (ct.isNotBlank()) {
            roomUUID.value = ct
        }
    }

    private fun currentClipboardText(): String {
        if (clipboard.getText().isNotBlank()) {
            val regex = ROOM_UUID_PATTERN.toRegex()
            val entire = regex.find(clipboard.getText())
            if (entire != null) {
                return entire.value
            }
        }
        return ""
    }
}

internal sealed class JoinRoomAction {
    object Close : JoinRoomAction()
    data class JoinRoom(val roomUUID: String, val openVideo: Boolean, val openAudio: Boolean) : JoinRoomAction()
}