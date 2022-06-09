package io.agora.flat.ui.activity.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Config
import io.agora.flat.Constants
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val roomConfigRepository: RoomConfigRepository,
    private val stringFetcher: StringFetcher,
    private val appKVCenter: AppKVCenter,
) : ViewModel() {
    private var _state = MutableStateFlow(MainViewState(appKVCenter.isProtocolAgreed()))
    val state = _state.asStateFlow()

    val roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val error = MutableStateFlow<UiError?>(null)

    private val roomUUID = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)

    init {
        viewModelScope.launch {
            if (isLoggedIn()) {
                if (AppKVCenter.MockData.mockEnable || userRepository.loginCheck() is Success) {
                    _state.value = _state.value.copy(loginState = LoginState.Login)
                } else {
                    _state.value = _state.value.copy(loginState = LoginState.Error)
                }
            } else {
                _state.value = _state.value.copy(loginState = LoginState.Error)
            }
        }

        viewModelScope.launch {
            if (roomUUID != null) {
                joinRoom(roomUUID, false, openAudio = false)
            }
        }
    }

    fun isLoggedIn() = userRepository.isLoggedIn()

    fun needBindPhone(): Boolean {
        val bound = userRepository.getUserInfo()?.hasPhone ?: false
        return !bound && Config.forceBindPhone
    }

    private fun joinRoom(roomUUID: String, openVideo: Boolean, openAudio: Boolean) {
        viewModelScope.launch {
            roomConfigRepository.updateRoomConfig(RoomConfig(roomUUID, openVideo, openAudio))

            when (val result = roomRepository.joinRoom(roomUUID)) {
                is Success -> roomPlayInfo.value = result.data
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

    fun agreeProtocol() {
        appKVCenter.setProtocolAgreed(true)
        _state.value = _state.value.copy(protocolAgreed = true)
    }
}

enum class MainTab {
    // 首页
    Home,

    // 云盘
    CloudStorage
}

data class MainViewState(
    val protocolAgreed: Boolean,
    val loginState: LoginState = LoginState.Init,
    val mainTab: MainTab = MainTab.Home,
)

sealed class LoginState {
    object Init : LoginState()
    object Login : LoginState()
    object Error : LoginState()
}