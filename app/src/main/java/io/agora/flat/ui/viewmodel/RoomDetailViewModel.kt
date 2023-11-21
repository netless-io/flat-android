package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.data.AppEnv
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomDetailPeriodic
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val appEnv: AppEnv,
    private val eventBus: EventBus,
    val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val roomInfo = MutableStateFlow<UIRoomInfo?>(null)
    private val periodicRoomInfo = MutableStateFlow<RoomDetailPeriodic?>(null)
    private val loadingState = ObservableLoadingCounter()

    private var userUUID: String = userRepository.getUserInfo()!!.uuid
    private val roomUUID: String = savedStateHandle[Constants.IntentKey.ROOM_UUID]!!
    private val periodicUUID: String? = savedStateHandle[Constants.IntentKey.PERIODIC_UUID]

    private val _state = MutableStateFlow(RoomDetailViewState(userUUID = userUUID))
    val state: StateFlow<RoomDetailViewState>
        get() = _state

    // TODO find a way to handle once request
    private var _cancelSuccess: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val cancelSuccess: StateFlow<Boolean>
        get() = _cancelSuccess

    init {
        viewModelScope.launch {
            combine(
                roomInfo,
                periodicRoomInfo,
                loadingState.observable,
            ) { roomInfo, periodicRoomInfo, loading ->
                _state.value.copy(
                    roomInfo = roomInfo,
                    periodicRoomInfo = periodicRoomInfo,
                    loading = loading
                )
            }.collect {
                _state.value = it
            }
        }

        loadOrdinaryRoom()

        if (periodicUUID != null) {
            loadPeriodicRoomInfo()
        }
    }

    private fun loadOrdinaryRoom() {
        viewModelScope.launch {
            loadingState.addLoader()
            val resp = roomRepository.getOrdinaryRoomInfo(roomUUID)
            if (resp is Success) {
                val info = resp.data.roomInfo
                info.map(
                    roomUUID,
                    periodicUUID,
                    userRepository.getUsername(),
                    appEnv.baseInviteUrl,
                    userRepository.getUserUUID() == info.ownerUUID,
                ).also { roomInfo.value = it }
            }
            loadingState.removeLoader()
        }
    }

    private fun loadPeriodicRoomInfo() {
        viewModelScope.launch {
            loadingState.addLoader()
            val resp = roomRepository.getPeriodicRoomInfo(periodicUUID!!)
            if (resp is Success) {
                periodicRoomInfo.value = resp.data
            }
            loadingState.removeLoader()
        }
    }

    fun cancelRoom() {
        viewModelScope.launch {
            loadingState.addLoader()
            val resp = if (periodicUUID != null) {
                roomRepository.cancelPeriodic(periodicUUID)
            } else {
                roomRepository.cancelOrdinary(roomUUID)
            }
            if (resp is Success) {
                _cancelSuccess.value = true
                eventBus.produceEvent(RoomsUpdated)
            } else {
                // show cancel error
            }
            loadingState.removeLoader()
        }
    }
}

data class RoomDetailViewState(
    val roomInfo: UIRoomInfo? = null,
    val userUUID: String,
    val periodicRoomInfo: RoomDetailPeriodic? = null,
    val allRoomsShown: Boolean = false,
    val loading: Boolean = false,
) {
    val isOwner: Boolean
        get() = roomInfo?.ownerUUID == userUUID
}

data class UIRoomInfo(
    val roomUUID: String,
    val periodicUUID: String? = null,
    val ownerUUID: String = "",
    val username: String = "",
    val title: String = "",
    val beginTime: Long = 0,
    val endTime: Long = 0,
    val roomType: RoomType,
    val roomStatus: RoomStatus = RoomStatus.Idle,
    val isPeriodic: Boolean = false,
    val hasRecord: Boolean = false,
    val inviteCode: String,
    val baseInviteUrl: String,
    val isPmi: Boolean = false,
    val isOwner: Boolean = false,
)

private fun RoomInfo.map(
    inRoomUUID: String,
    inPeriodicUUID: String?,
    inUsername: String,
    baseInviteUrl: String,
    isOwner: Boolean,
): UIRoomInfo {
    return UIRoomInfo(
        roomUUID = inRoomUUID,
        periodicUUID = inPeriodicUUID,
        ownerUUID = ownerUUID,
        username = inUsername,
        title = title,
        beginTime = beginTime,
        endTime = endTime,
        roomType = roomType,
        roomStatus = roomStatus,
        hasRecord = hasRecord,
        inviteCode = inviteCode,
        isPmi = isPmi == true,
        baseInviteUrl = baseInviteUrl,
        isOwner = isOwner,
    )
}