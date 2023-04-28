package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appKVCenter: AppKVCenter,
    env: AppEnv
) : ViewModel() {
    private val _state = MutableStateFlow(
        SettingsUiState(
            networkAcceleration = appKVCenter.isNetworkAcceleration(),
            infoUrl = "${env.baseInviteUrl}/sensitive?token=${appKVCenter.getToken()}"
        )
    )

    val state: StateFlow<SettingsUiState>
        get() = _state

    fun logout() {
        userRepository.logout()
    }

    fun setNetworkAcceleration(acc: Boolean) {
        appKVCenter.setNetworkAcceleration(acc)
        _state.value = _state.value.copy(networkAcceleration = acc)
    }
}

data class SettingsUiState(
    val networkAcceleration: Boolean = false,
    val infoUrl: String = ""
)
