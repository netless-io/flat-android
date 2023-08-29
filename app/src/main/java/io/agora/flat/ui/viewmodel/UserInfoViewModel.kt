package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.AppEnv
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.LoginPlatform
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.onFailure
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserBindingsUpdated
import io.agora.flat.event.UserUpdated
import io.agora.flat.ui.util.UiErrorMessage
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
    private val appEnv: AppEnv,
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

    fun processUnbindAction(platform: LoginPlatform) {
        viewModelScope.launch {
            userRepository.removeBinding(platform).onFailure {
                uiMessageManager.emitMessage(UiErrorMessage(it))
            }
            _userBindings.value = userRepository.getBindings()
        }
    }

    fun handlePickedAvatar(info: ContentInfo) {
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
                        ossKey = appEnv.ossKey,
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
) {
    val bindingCount
        get() = bindings?.run {
            listOf(wechat, phone, email, agora, apple, github, google).count { it }
        } ?: 0
}

sealed class UserInfoUiAction {
    object Finish : UserInfoUiAction()
    data class PickedAvatar(val info: ContentInfo) : UserInfoUiAction()

    object BindGithub : UserInfoUiAction()
    object BindWeChat : UserInfoUiAction()
    object BindPhone : UserInfoUiAction()
    object BindEmail : UserInfoUiAction()
    object BindGoogle : UserInfoUiAction()

    data class UnbindAction(val platform: LoginPlatform) : UserInfoUiAction()

//    object UnbindWeChat : UserInfoUiAction()
//    object UnbindGithub : UserInfoUiAction()
//    object UnbindPhone : UserInfoUiAction()
//    object UnbindEmail : UserInfoUiAction()
//    object UnbindGoogle : UserInfoUiAction()
}