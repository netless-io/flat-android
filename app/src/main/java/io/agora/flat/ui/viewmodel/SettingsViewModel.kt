package io.agora.flat.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.AndroidDownloader
import io.agora.flat.common.version.VersionCheckResult
import io.agora.flat.common.version.VersionChecker
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.MiscRepository
import io.agora.flat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appKVCenter: AppKVCenter,
    private val versionChecker: VersionChecker,
    private val downloader: AndroidDownloader,
    private val miscRepository: MiscRepository,
    env: AppEnv
) : ViewModel() {
    private val _state = MutableStateFlow(
        SettingsUiState(infoUrl = "${env.baseInviteUrl}/sensitive?token=${appKVCenter.getToken()}")
    )

    val state: StateFlow<SettingsUiState>
        get() = _state

    init {
        viewModelScope.launch {
            val checkResult = versionChecker.forceCheck()
            _state.update {
                it.copy(versionCheckResult = checkResult)
            }
        }

        if (env.showStreamAgreement) {
            _state.update {
                it.copy(isAgreeStream = false)
            }
            viewModelScope.launch {
                val value = miscRepository.getStreamAgreement() ?: false
                _state.update {
                    it.copy(isAgreeStream = value)
                }
            }
        }
    }

    suspend fun downloadApp(): Uri {
        val result = _state.value.versionCheckResult
        return downloader.download(result.appUrl!!, "${result.appVersion}.apk")
    }

    fun cancelUpdate() {
        versionChecker.cancelUpdate()
        _state.value = _state.value.copy(versionCheckResult = VersionCheckResult.Empty)
    }

    fun setAgreeStream(isAgree: Boolean) {
        viewModelScope.launch {
            miscRepository.setStreamAgreement(isAgree)
            _state.value = _state.value.copy(isAgreeStream = isAgree)
        }
    }

    fun logout() {
        userRepository.logout()
    }
}

data class SettingsUiState(
    val infoUrl: String = "",
    val versionCheckResult: VersionCheckResult = VersionCheckResult.Empty,
    val isAgreeStream: Boolean? = null
)
