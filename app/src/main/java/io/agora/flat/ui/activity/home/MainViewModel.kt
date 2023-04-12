package io.agora.flat.ui.activity.home

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Config
import io.agora.flat.Constants
import io.agora.flat.common.android.AndroidDownloader
import io.agora.flat.common.version.VersionCheckResult
import io.agora.flat.common.version.VersionChecker
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import kotlinx.coroutines.delay
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
    private val versionChecker: VersionChecker,
    private val appKVCenter: AppKVCenter,
    private val appEnv: AppEnv,
    private val downloader: AndroidDownloader,
) : ViewModel() {
    private val uiMessageManager = UiMessageManager()

    private var _state = MutableStateFlow(MainUiState(appKVCenter.isProtocolAgreed()))
    val state = _state.asStateFlow()

    val roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    private val roomUUID: String? = savedStateHandle[Constants.IntentKey.ROOM_UUID]

    init {
        viewModelScope.launch {
            if (isLoggedIn()) {
                if (userRepository.loginCheck() is Success) {
                    _state.value = _state.value.copy(
                        loginState = LoginState.Login,
                        userAvatar = userRepository.getUserInfo()?.avatar
                    )
                } else {
                    _state.value = _state.value.copy(loginState = LoginState.Error)
                }
            } else {
                _state.value = _state.value.copy(loginState = LoginState.Error)
            }
        }

        viewModelScope.launch {
            uiMessageManager.message.collect {
                _state.value = _state.value.copy(message = it)
            }
        }

        roomUUID?.let { joinRoom(it, openVideo = false, openAudio = false) }
    }

    fun checkVersion() {
        viewModelScope.launch {
            val result = versionChecker.check()
            if (result != VersionCheckResult.Empty) {
                delay(2000)
                _state.value = _state.value.copy(versionCheckResult = result)
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
                is Failure -> uiMessageManager.emitMessage(UiMessage("join room error", result.exception))
            }
        }
    }

    fun agreeProtocol() {
        appKVCenter.setProtocolAgreed(true)
        _state.value = _state.value.copy(protocolAgreed = true)
    }

    suspend fun downloadApp(): Uri {
        val result = _state.value.versionCheckResult
        return downloader.download(result.appUrl!!, "${result.appVersion}.apk")
    }

    fun cancelUpdate() {
        versionChecker.cancelUpdate()
        _state.value = _state.value.copy(versionCheckResult = VersionCheckResult.Empty)
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }

    fun handleDeepLink(url: String) {
        if (url.startsWith("${appEnv.baseInviteUrl}/join")) {
            val roomUUID = url.substringAfterLast("/")
            joinRoom(roomUUID, openVideo = false, openAudio = false)
        }
    }
}

data class MainUiState(
    val protocolAgreed: Boolean,
    val loginState: LoginState = LoginState.Init,
    val userAvatar: String? = null,
    val versionCheckResult: VersionCheckResult = VersionCheckResult.Empty,
    val message: UiMessage? = null,
)

sealed class LoginState {
    object Init : LoginState()
    object Login : LoginState()
    object Error : LoginState()
}

enum class MainTab {
    // 首页
    Home,

    // 云盘
    Cloud
}