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
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserUpdated
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.ContentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val appEnv: AppEnv,
    private val eventBus: EventBus,
) : ViewModel() {
    private val userInfo = MutableStateFlow(userRepository.getUserInfo())
    private val uiMessageManager = UiMessageManager()

    private val _state = MutableStateFlow(UserInfoUiState())
    val state: StateFlow<UserInfoUiState> = _state.asStateFlow()

    private var uploadingUUID: String? = null

    init {
        viewModelScope.launch {
            combine(userInfo, uiMessageManager.message) { userInfo, message ->
                UserInfoUiState(
                    userInfo = userInfo,
                    message = message
                )
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch {
            UploadManager.observeSuccess().filter { it.uuid == uploadingUUID }.collect {
                cloudStorageRepository.updateAvatarFinish(it.uuid)
                eventBus.produceEvent(UserUpdated)
            }
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            userInfo.value = userRepository.getUserInfo()
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
                        uri = info.uri,
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
    val message: UiMessage? = null,
)