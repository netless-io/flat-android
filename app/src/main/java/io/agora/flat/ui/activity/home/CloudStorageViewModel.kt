package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Success
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.util.ObservableLoadingCounter
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class CloudStorageViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val appKVCenter: AppKVCenter,
    private val cloudStorageRepository: CloudStorageRepository,
) : ViewModel() {
    companion object {
        val TAG = CloudStorageViewModel.javaClass.simpleName;
    }

    private val refreshing = ObservableLoadingCounter()
    private val files = MutableStateFlow(listOf<CloudStorageFile>())
    private val totalUsage = MutableStateFlow(0L)
    private val pageLoading = ObservableLoadingCounter()

    private val _state = MutableStateFlow(CloudStorageViewState())
    val state: StateFlow<CloudStorageViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                refreshing.observable,
                totalUsage,
                files
            ) { refreshing, totalUsage, files ->
                CloudStorageViewState(
                    refreshing = refreshing,
                    totalUsage = totalUsage,
                    files = files,
                    errorMessage = null,
                )
            }.collect {
                _state.value = it
            }
        }

        reloadFileList()
    }

    fun reloadFileList() {
        viewModelScope.launch {
            refreshing.addLoader()
            runAtLeast {
                val resp = cloudStorageRepository.getFileList(1)
                if (resp is Success) {
                    totalUsage.value = resp.data.totalUsage
                    files.value = resp.data.files
                } else {
                    // TODO
                }
            }
            refreshing.removeLoader()
        }
    }

    fun checkItem(action: CloudStorageUIAction.CheckItem) {
        viewModelScope.launch {
            val fs = files.value.toMutableList()
            fs[action.index] = fs[action.index].copy(checked = action.checked)
            files.value = fs
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            pageLoading.addLoader()
            val fileList = files.value.filter { it.checked }.map { it.fileUUID }
            val result = cloudStorageRepository.remove(fileList)
            if (result is Success) {
                files.value = files.value.toMutableList().filter { !fileList.contains(it.fileUUID) }
            }
            pageLoading.removeLoader()
        }
    }
}

data class CloudStorageViewState(
    val refreshing: Boolean = false,
    val totalUsage: Long = 0,
    val files: List<CloudStorageFile> = emptyList(),
    val pageLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed class CloudStorageUIAction {
    object Delete : CloudStorageUIAction()
    object Reload : CloudStorageUIAction()
    data class CheckItem(val index: Int, val checked: Boolean) : CloudStorageUIAction()
    data class ClickItem(val index: Int) : CloudStorageUIAction()
}