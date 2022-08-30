package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.NetworkObserver
import io.agora.flat.event.EventBus
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.event.UserUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val eventBus: EventBus,
    private val appKVCenter: AppKVCenter,
    networkObserver: NetworkObserver,
    private val logger: Logger,
) : ViewModel() {
    private val userInfo = MutableStateFlow(appKVCenter.getUserInfo() ?: UserInfo("", "", ""))
    private val rooms = MutableStateFlow(listOf<RoomInfo>())
    private val refreshing = ObservableLoadingCounter()
    private val networkActive = networkObserver.observeNetworkActive()

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState>
        get() = _state

    private var page: Int = 1

    init {
        viewModelScope.launch {
            combine(
                rooms,
                refreshing.observable,
                userInfo,
                networkActive
            ) { rooms, refreshing, userInfo, networkActive ->
                HomeUiState(
                    roomList = rooms,
                    refreshing = refreshing,
                    userInfo = userInfo,
                    networkActive = networkActive,
                    errorMessage = null,
                )
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch {
            networkActive.collect {
                _state.value = _state.value.copy(networkActive = it)
            }
        }

        viewModelScope.launch {
            eventBus.events.filterIsInstance<RoomsUpdated>().collect {
                reloadRooms()
            }
        }

        viewModelScope.launch {
            eventBus.events.filterIsInstance<UserUpdated>().collect {
                appKVCenter.getUserInfo()?.let {
                    userInfo.value = it
                }
            }
        }

        reloadRooms()
    }

    fun reloadRooms() {
        viewModelScope.launch {
            refreshing.addLoader()
            runAtLeast {
                when (val result = roomRepository.getRoomListAll(page)) {
                    is Success -> {
                        rooms.value = result.data
                    }
                    is Failure -> when (result.exception) {
                        // handle error
                    }
                }
            }
            refreshing.removeLoader()
        }
    }
}