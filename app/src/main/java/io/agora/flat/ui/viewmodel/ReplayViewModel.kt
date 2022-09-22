package io.agora.flat.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.rtm.Message
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RecordInfo
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReplayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val messageQuery: MessageQuery,
) : ViewModel() {
    private val roomUUID: String = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)!!
    private var _state = MutableStateFlow(ReplayUiState(roomUUID = roomUUID))
    val state: StateFlow<ReplayUiState>
        get() = _state

    init {
        viewModelScope.launch {
            when (val roomResp = roomRepository.getOrdinaryRoomInfo(roomUUID)) {
                is Success -> {
                    val roomInfo = roomResp.data.roomInfo
                    if (roomInfo.roomStatus != RoomStatus.Stopped) {
                        return@launch
                    }
                    var videoUrl: String? = null
                    var recordInfo: RecordInfo? = null
                    when (val recordResp = cloudRecordRepository.getRecordInfo(roomUUID)) {
                        is Success -> {
                            messageQuery.update(roomUUID, roomInfo.beginTime, roomInfo.endTime)
                            recordInfo = recordResp.data
                            videoUrl = recordResp.data.recordInfo.firstOrNull()?.videoURL
                        }
                        is Failure -> {
                            // request record info error
                            // LOCAL_REPLAY_ERROR_REQUEST_RECORD
                        }
                    }
                    if (videoUrl != null) {
                        when (val usersResp = roomRepository.getRoomUsers(roomUUID, null)) {
                            is Success -> {
                                val users = usersResp.data.map {
                                    RelayUiUser(
                                        userUUID = it.key,
                                        rtcUID = it.value.rtcUID,
                                        name = it.value.name,
                                        avatarURL = it.value.avatarURL,
                                        videoUrl = videoUrl.replace(
                                            ".m3u8",
                                            "__uid_s_${it.value.rtcUID}__uid_e_av.m3u8"
                                        ),
                                    )
                                }.filter { user ->
                                    withContext(Dispatchers.IO) {
                                        UrlUtils.isResourceExisted(user.videoUrl)
                                    }
                                }
                                Log.e("Aderan", users.toString())
                                _state.value = _state.value.copy(
                                    users = users,
                                    recordInfo = recordInfo,
                                    roomInfo = roomInfo
                                )
                            }
                            is Failure -> {}
                        }
                    }
                }
                is Failure -> {}
            }

//            if (resp is Success) {
//                val roomInfo = resp.data.roomInfo
//                if (roomInfo.roomStatus != RoomStatus.Stopped) {
//                    return@launch
//                }
//                var videoUrl: String? = null
//                when (val recordResp = cloudRecordRepository.getRecordInfo(roomUUID)) {
//                    is Failure -> {
//
//                    }
//                    is Success -> {
//                        _state.value = _state.value.copy(roomInfo = roomInfo, recordInfo = recordResp.data)
//                        videoUrl = recordResp.data.recordInfo.firstOrNull()?.videoURL
//                    }
//                }
//                if (videoUrl != null) {
//                    when (val usersResp = roomRepository.getRoomUsers(roomUUID, null)) {
//                        is Failure -> {}
//                        is Success -> {
//                            val users = usersResp.data.values
//                                .map {
//                                    RelayUiUser(
//                                        userUUID = it.userUUID,
//                                        rtcUID = it.rtcUID,
//                                        name = it.name,
//                                        avatarURL = it.avatarURL,
//                                        videoUrl = videoUrl.replace(".m3u8", "__uid_s_${it.rtcUID}__uid_e_av.m3u8"),
//                                    )
//                                }
//                                .filter { user ->
//                                    withContext(Dispatchers.IO) {
//                                        UrlUtils.isResourceExisted(user.videoUrl)
//                                    }
//                                }
//                            Log.e("Aderan", users.toString())
//                        }
//                    }
//                }
//            }
        }

//        viewModelScope.launch {
//            val result = cloudRecordRepository.getRecordInfo(roomUUID)
//            if (result is Success) {
//                _state.value = _state.value.copy(recordInfo = result.data)
//            }
//        }
    }

    fun updateTime(time: Long) {
        // viewModelScope.launch {
        //     val msgs = messageQuery.query(time)
        //     _state.value = _state.value.copy(messages = msgs.toList())
        // }
    }
}

data class ReplayUiState(
    val roomUUID: String,
    val roomInfo: RoomInfo? = null,
    val recordInfo: RecordInfo? = null,
    val users: List<RelayUiUser> = listOf(),
    val messages: List<Message> = listOf(),
    val isPlayer: Boolean = false,
) {
    val duration: Long
        get() = (roomInfo?.run { endTime - beginTime }) ?: 0L
}

data class RelayUiUser(
    val userUUID: String,
    val rtcUID: Int,
    val name: String,
    val avatarURL: String,
    val videoUrl: String,
)