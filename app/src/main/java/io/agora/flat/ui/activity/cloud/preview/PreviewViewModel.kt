package io.agora.flat.ui.activity.cloud.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.AppEnv
import io.agora.flat.data.model.CloudFile
import io.agora.flat.data.model.CoursewareType
import io.agora.flat.util.coursewareType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    appEnv: AppEnv,
) : ViewModel() {
    val file: CloudFile = savedStateHandle.get<CloudFile>(Constants.IntentKey.CLOUD_FILE)!!

    private var loading = true
    private var rendering = true

    private var _state = MutableStateFlow(
        PreviewState(
            true,
            file = file,
            baseUrl = appEnv.baseInviteUrl
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadCourseware()
        }
    }

    private fun loadCourseware() {
        when (val type = file.fileURL.coursewareType()) {
            CoursewareType.Unknown -> {
                loading = false
                _state.value = state.value.copy(loading = isLoading())
            }
            else -> {
                loading = false
                _state.value = state.value.copy(loading = isLoading(), type = type)
            }
        }
    }

    fun onLoadFinished() {
        rendering = false
        _state.value = state.value.copy(loading = isLoading())
    }

    private fun isLoading(): Boolean {
        return loading || rendering
    }
}