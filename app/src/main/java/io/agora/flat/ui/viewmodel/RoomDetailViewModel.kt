package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.agora.flat.Constants
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomDetailPeriodic
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.model.RoomType
import io.agora.flat.data.repository.RoomRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val roomInfo = MutableStateFlow<UIRoomInfo?>(null)
    private val periodicRoomInfo = MutableStateFlow<RoomDetailPeriodic?>(null)
    private val loading = MutableStateFlow(true)
    private val loadingCount = AtomicInteger(0)

    private val _state = MutableStateFlow(RoomDetailViewState())

    val state: StateFlow<RoomDetailViewState>
        get() = _state

    private var _cancelSuccess: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val cancelSuccess: StateFlow<Boolean>
        get() = _cancelSuccess

    init {
        viewModelScope.launch {
            combine(
                roomInfo,
                periodicRoomInfo,
                loading,
            ) { roomInfo, periodicRoomInfo, loading ->
                RoomDetailViewState(
                    roomInfo = roomInfo,
                    periodicRoomInfo = periodicRoomInfo,
                    loading = loading,
                    errorMessage = null
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }

        loadOrdinaryRoom()
        if (isPeriodicRoom()) {
            loadPeriodicRoomInfo()
        }
    }

    private fun incLoadingCount() {
        loadingCount.incrementAndGet()
        loading.value = true
    }

    private fun decLoadingCount() {
        if (loadingCount.decrementAndGet() == 0) {
            loading.value = false
        }
    }

    private fun loadOrdinaryRoom() {
        viewModelScope.launch {
            incLoadingCount()
            val resp =
                roomRepository.getOrdinaryRoomInfo(intentValue(Constants.IntentKey.ROOM_UUID))
            if (resp is Success) {
                roomInfo.value =
                    resp.data.roomInfo.map(
                        intentValue(Constants.IntentKey.ROOM_UUID),
                        intentValueNullable(Constants.IntentKey.PERIODIC_UUID)
                    )
            }
            decLoadingCount()
        }
    }

    private fun loadPeriodicRoomInfo() {
        viewModelScope.launch {
            incLoadingCount()
            val resp =
                roomRepository.getPeriodicRoomInfo(intentValue(Constants.IntentKey.PERIODIC_UUID))
            if (resp is Success) {
                periodicRoomInfo.value = resp.data
            }
            decLoadingCount()
        }
    }

    fun isPeriodicRoom(): Boolean {
        return savedStateHandle.get<String>(Constants.IntentKey.PERIODIC_UUID) != null
    }

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }

    private fun intentValueNullable(key: String): String? {
        return savedStateHandle.get<String>(key)
    }

    fun cancelRoom() {
        viewModelScope.launch {
            incLoadingCount()
            val resp = if (isPeriodicRoom()) {
                roomRepository.cancelPeriodic(intentValue(Constants.IntentKey.PERIODIC_UUID))
            } else {
                roomRepository.cancelOrdinary(intentValue(Constants.IntentKey.ROOM_UUID))
            }
            if (resp is Success) {
                _cancelSuccess.value = true
            } else {

            }
            decLoadingCount()
        }
    }
}

data class RoomDetailViewState(
    val roomInfo: UIRoomInfo? = null,
    val periodicRoomInfo: RoomDetailPeriodic? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null
)

data class UIRoomInfo(
    val roomUUID: String,
    val periodicUUID: String? = null,
    val ownerUUID: String = "",
    val title: String = "",
    val beginTime: Long = 0,
    val endTime: Long = 0,
    val roomType: RoomType,
    val roomStatus: RoomStatus = RoomStatus.Idle,
    val isPeriodic: Boolean = false
)

private fun RoomInfo.map(inRoomUUID: String, inPeriodicUUID: String?): UIRoomInfo {
    return UIRoomInfo(
        roomUUID = inRoomUUID,
        periodicUUID = inPeriodicUUID,
        ownerUUID = ownerUUID,
        title = title,
        beginTime = beginTime,
        endTime = endTime,
        roomType = roomType,
        roomStatus = roomStatus
    )
}