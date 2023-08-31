package io.agora.flat.ui.activity.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.common.upload.UploadRequest
import io.agora.flat.data.AppEnv
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.util.UiMessageManager
import io.agora.flat.util.ContentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class RegisterProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val appEnv: AppEnv,
) : ViewModel() {
    private val loading = ObservableLoadingCounter()
    private val messageManager = UiMessageManager()
    private var avatarUuid: String? = null

    private var _state = MutableStateFlow(RegisterProfileUiState.Init)
    val state: StateFlow<RegisterProfileUiState>
        get() = _state

    init {
        viewModelScope.launch {
            loading.observable.collect {
                _state.value = _state.value.copy(loading = it)
            }
        }
    }

    fun handleConfirmInfo(name: String?, avatarRes: ContentInfo?) {
        viewModelScope.launch {
            loading.addLoader()
            if (name?.isNotBlank() == true) {
                userRepository.rename(name = name)
            }
            if (avatarRes != null) {
                cloudStorageRepository.updateAvatarStart(avatarRes.filename, avatarRes.size)
                    .onSuccess {
                        avatarUuid = it.fileUUID
                        val request = UploadRequest(
                            uuid = it.fileUUID,
                            policy = it.policy,
                            policyURL = it.ossDomain,
                            filepath = it.ossFilePath,
                            ossKey = appEnv.ossKey,
                            signature = it.signature,
                            filename = avatarRes.filename,
                            size = avatarRes.size,
                            mediaType = avatarRes.mediaType,
                            uri = avatarRes.uri,
                        )
                        UploadManager.upload(request)
                        val result = withTimeoutOrNull(5000) {
                            UploadManager.observeSuccess().filter { r -> r.uuid == avatarUuid }.first()
                        }
                        if (result != null) {
                            cloudStorageRepository.updateAvatarFinish(avatarUuid!!)
                        }
                    }
            }
            loading.removeLoader()
            _state.value = _state.value.copy(success = true)
        }
    }

    fun clearUiMessage(id: Long) {
        viewModelScope.launch {
            messageManager.clearMessage(id)
        }
    }
}

data class RegisterProfileUiState(
    val success: Boolean = false,
    val loading: Boolean = false,
    val message: UiMessage? = null,
) {
    companion object {
        val Init = RegisterProfileUiState()
    }
}