package io.agora.flat.ui.activity.cloud.uploading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.upload.UploadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadingViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(UploadingUIState())
    val state: StateFlow<UploadingUIState>
        get() = _state

    init {
        viewModelScope.launch {
            UploadManager.observeUploadFiles(Constants.UPLOAD_TAG_CLOUD).map {
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