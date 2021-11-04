package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.data.Failure
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
    private val stringFetcher: StringFetcher,
) : ViewModel() {
    val roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val error = MutableStateFlow<UiError?>(null)

    fun joinRoom(uuid: String, openVideo: Boolean, openAudio: Boolean) {
        val trimID = uuid.replace("\\s".toRegex(), "")

        viewModelScope.launch {
            when (val result = roomRepository.joinRoom(trimID)) {
                is Success -> {
                    roomPlayInfo.value = result.data
                    roomConfigRepository.updateRoomConfig(RoomConfig(result.data.roomUUID, openVideo, openAudio))
                }
                is Failure -> {
                    error.value = when (result.error.code) {
                        FlatErrorCode.Web_RoomNotFound -> UiError(stringFetcher.roomNotFound())
                        FlatErrorCode.Web_RoomIsEnded -> UiError(stringFetcher.roomIsEnded())
                        else -> UiError(stringFetcher.joinRoomError(result.error.code))
                    }
                }
            }
        }
    }
}

internal sealed class JoinRoomAction {
    object Close : JoinRoomAction()
    data class JoinRoom(val roomID: String, val openVideo: Boolean, val openAudio: Boolean) : JoinRoomAction()
}