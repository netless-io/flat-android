package io.agora.flat.ui.activity.dev

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.Success
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.UserUpdated
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevToolViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    fun removeAllRooms() {
        viewModelScope.launch {
            when (val result = roomRepository.getRoomListAll(1)) {
                is Success -> {
                    result.data.forEach {
                        if (it.ownerUUID == userRepository.getUserInfo()?.uuid) {
                            roomRepository.stopRoomClass(it.roomUUID)
                        } else {
                            if (it.isPeriodic) {
                                roomRepository.cancelPeriodic(it.periodicUUID!!)
                            } else {
                                roomRepository.cancelOrdinary(it.roomUUID)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}