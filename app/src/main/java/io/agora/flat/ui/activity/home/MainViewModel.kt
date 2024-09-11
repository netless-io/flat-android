package io.agora.flat.ui.activity.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.AndroidDownloader
import io.agora.flat.common.version.VersionCheckResult
import io.agora.flat.common.version.VersionChecker
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.data.model.RoomPlayInfo
import io.agora.flat.data.repository.MiscRepository
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
    private val miscRepository: MiscRepository,
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
    val replayInfo = MutableStateFlow<String?>(null)

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

        viewModelScope.launch {
            miscRepository.getRegionConfigs()
        }
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
        return !bound && appEnv.loginConfig.forceBindPhone()
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

    fun cleanRoomPlayInfo() {
        roomPlayInfo.value = null
    }

    fun clearReplayInfo() {
        replayInfo.value = null
    }

    fun handleDeepLink(uri: Uri) {
        val startsWithBaseUrl = AppEnv.ALL_BASE_URLS.any { baseUrl -> uri.toString().startsWith(baseUrl) }
        if (startsWithBaseUrl) {
            val pathSegments = uri.pathSegments
            // https://web.flat.apprtc.cn/join/SmallClass/c06ab0b1-05ef-403d-9db0-69cc85ee23bf/
            if (pathSegments.size == 2 && pathSegments[0] == "join") {
                val roomUUID = pathSegments[1]
                joinRoom(roomUUID, openVideo = false, openAudio = false)
            }
            // https://web.flat.apprtc.cn/replay/SmallClass/c06ab0b1-05ef-403d-9db0-69cc85ee23bf/855c8d51-b5a5-439f-a74b-2eb1a0284036/
            if (pathSegments.size == 4 && pathSegments[0] == "replay") {
                val roomUUID = pathSegments[2]
                replayInfo.value = roomUUID
            }
        }
        if (uri.scheme == "x-agora-flat-client") {
            when (uri.authority) {
                // x-agora-flat-client://joinRoom?roomUUID=6827e45b-e7b4-4744-b62f-24a31d5e1381
                "joinRoom" -> {
                    val roomUUID = uri.getQueryParameter("roomUUID")
                    joinRoom(roomUUID!!, openVideo = false, openAudio = false)
                }

                "replayRoom" -> {
                    // x-agora-flat-client://replayRoom?roomUUID=245361ea-1cf4-4a03-9cdd-6170f9e8d9e3&ownerUUID=6a8fa51b-4d94-4d2f-92e3-2504065641ac&roomType=BigClass
                    val roomUUID = uri.getQueryParameter("roomUUID")
                    replayInfo.value = roomUUID
                }
            }
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