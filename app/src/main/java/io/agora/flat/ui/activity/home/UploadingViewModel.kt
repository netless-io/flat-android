package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.upload.UploadFile
import io.agora.flat.common.upload.UploadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadingViewModel @Inject constructor() : ViewModel() {
    companion object {
        const val TAG = "UploadingViewModel"
    }

    private val _state = MutableStateFlow(UploadingUIState())
    val state: StateFlow<UploadingUIState>
        get() = _state

    init {
        viewModelScope.launch {
            UploadManager.uploadFiles.map {
                UploadingUIState(uploadFiles = it)
            }.collect {
                _state.value = it
            }
        }
    }

    fun retryUpload(fileUUID: String) {
        viewModelScope.launch {
            UploadManager.retry(fileUUID)
        }
    }

    fun deleteUpload(fileUUID: String) {
        viewModelScope.launch {
            UploadManager.cancel(fileUUID)
        }
    }
}

data class UploadingUIState(
    val uploadFiles: List<UploadFile> = emptyList(),
)

sealed class UploadingUIAction {
    data class UploadRetry(val fileUUID: String) : UploadingUIAction()
    data class UploadDelete(val fileUUID: String) : UploadingUIAction()

    object CloseUploading : UploadingUIAction()
}