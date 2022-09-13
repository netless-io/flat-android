package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.LoginPlatform
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserBindingsUpdated
import io.agora.flat.event.UserUpdated
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.ContentInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val eventBus: EventBus,
) : ViewModel() {
    private val _userInfo = MutableStateFlow(userRepository.getUserInfo())
    private val _userBindings = MutableStateFlow<UserBindings?>(null)
    private val uiMessageManager = UiMessageManager()

    private val _state = MutableStateFlow(UserInfoUiState())
    val state: StateFlow<UserInfoUiState> = _state.asStateFlow()

    private var uploadingUUID: String? = null

    init {
        viewModelScope.launch {
            combine(_userInfo, _userBindings, uiMessageManager.message) { userInfo, bindings, message ->
                UserInfoUiState(userInfo, bindings, message = message)
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch {
            eventBus.events.filterIsInstance<UserBindingsUpdated>().collect {
                _userBindings.value = userRepository.getBindings()
            }
        }

        loadBindings()

        viewModelScope.launch {
            UploadManager.uploadSuccess.filterNotNull().filter { it.uuid == uploadingUUID }.collect {
                cloudStorageRepository.updateAvatarFinish(it.uuid)
                eventBus.produceEvent(UserUpdated)
            }
        }
    }

    private fun loadBindings() {
        viewModelScope.launch {
            _userBindings.value = userRepository.listBindings().get()
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            _userInfo.value = userRepository.getUserInfo()
        }
    }

    fun processAction(action: UserInfoUiAction) {
        viewModelScope.launch {
            when (action) {
                UserInfoUiAction.UnbindGithub -> userRepository.removeBinding(LoginPlatform.Github)
                UserInfoUiAction.UnbindWeChat -> userRepository.removeBinding(LoginPlatform.WeChat)
                is UserInfoUiAction.PickedAvatar -> handlePickedAvatar(action.info)
                else -> {}
            }
            _userBindings.value = userRepository.getBindings()
        }
    }

    private fun handlePickedAvatar(info: ContentInfo) {
        viewModelScope.launch {
            when (val resp = cloudStorageRepository.updateAvatarStart(info.filename, info.size)) {
                is Success -> {
                    val result = resp.data
                    uploadingUUID = result.fileUUID
                    val request = UploadRequest(
                        uuid = result.fileUUID,
                        policy = result.policy,
                        policyURL = result.ossDomain,
                        filepath = result.ossFilePath,
                        signature = result.signature,
                        filename = info.filename,
                        size = info.size,
                        mediaType = info.mediaType,
                        uri = info.uri
                    )
                    UploadManager.upload(request)
                }
                is Failure -> uiMessageManager.emitMessage(UiMessage("", exception = resp.exception))
            }
        }
    }

    fun clearMessage(id: Long) {
        viewModelScope.launch {
            uiMessageManager.clearMessage(id)
        }
    }
}

data class UserInfoUiState(
    val userInfo: UserInfo? = null,
    val bindings: UserBindings? = null,
    val message: UiMessage? = null,
)

sealed class UserInfoUiAction {
    object Finish : UserInfoUiAction()
    object BindGithub : UserInfoUiAction()
    object BindWeChat : UserInfoUiAction()

    object UnbindWeChat : UserInfoUiAction()
    object UnbindGithub : UserInfoUiAction()
    data class PickedAvatar(val info: ContentInfo) : UserInfoUiAction()
}
