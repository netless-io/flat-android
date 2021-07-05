package io.agora.flat.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herewhite.sdk.ConverterCallbacks
import com.herewhite.sdk.converter.ConvertType
import com.herewhite.sdk.converter.ConverterV5
import com.herewhite.sdk.domain.ConversionInfo
import com.herewhite.sdk.domain.ConvertException
import com.herewhite.sdk.domain.ConvertedFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.ClipboardController
import io.agora.flat.common.FlatException
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.AppModule
import io.agora.flat.di.interfaces.EventBus
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.event.HomeRefreshEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val savedStateHandle: SavedStateHandle,
    private val database: AppDatabase,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter,
    private val rtmApi: RtmEngineProvider,
    private val eventbus: EventBus,
    val clipboard: ClipboardController
) : ViewModel() {
    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _state: MutableStateFlow<ClassRoomState>
    val state: StateFlow<ClassRoomState>
        get() = _state

    // 缓存用户信息，降低web服务压力
    private var _usersCacheMap: MutableMap<String, RtcUser> = mutableMapOf()
    private var _usersMap = MutableStateFlow<Map<String, RtcUser>>(emptyMap())
    val usersMap = _usersMap.asStateFlow()

    private var _messageList = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messageList = _messageList.asStateFlow()

    private var _cloudStorageFiles = MutableStateFlow<List<CloudStorageFile>>(mutableListOf())
    val cloudStorageFiles = _cloudStorageFiles.asStateFlow()

    private var _videoAreaShown = MutableStateFlow(true)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    private var _messageAreaShown = MutableStateFlow(false)
    val messageAreaShown = _messageAreaShown.asStateFlow()

    private var _roomEvent = MutableStateFlow<ClassRoomEvent?>(null)
    val roomEvent = _roomEvent
    private val eventId = AtomicInteger(0)

    // TODO 是否能处理变量
    private var _roomConfig = MutableStateFlow(RoomConfig(""))
    val roomConfig = _roomConfig.asStateFlow()

    var roomUUID: String

    init {
        roomUUID = intentValue(Constants.IntentKey.ROOM_UUID)
        _state = MutableStateFlow(
            ClassRoomState(
                roomUUID = roomUUID,
                currentUserUUID = appKVCenter.getUserInfo()!!.uuid,
                currentUserName = appKVCenter.getUserInfo()!!.name
            )
        )

        viewModelScope.launch {
            when (val result =
                roomRepository.joinRoom(roomUUID)) {
                is Success -> {
                    _roomPlayInfo.value = result.data
                }
            }
        }

        viewModelScope.launch {
            when (val result = roomRepository.getOrdinaryRoomInfo(roomUUID)) {
                is Success -> result.data.run {
                    _state.value = _state.value.copy(
                        roomType = roomInfo.roomType,
                        ownerUUID = roomInfo.ownerUUID,
                        ownerName = roomInfo.ownerName,
                        title = roomInfo.title,
                        beginTime = roomInfo.beginTime,
                        endTime = roomInfo.endTime,
                        roomStatus = roomInfo.roomStatus,
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _roomConfig.value = database.roomConfigDao().getConfigById(roomUUID) ?: RoomConfig(roomUUID)
        }
    }

    fun enableVideo(enable: Boolean, uuid: String = _state.value.currentUserUUID) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uuid == _state.value.currentUserUUID) {
                val config = _roomConfig.value.copy(enableVideo = enable)
                database.roomConfigDao().insertOrUpdate(config)
                _roomConfig.value = config
            }

            _usersMap.value[uuid]?.run {
                if (uuid == _state.value.currentUserUUID) {
                    sendAndUpdateDeviceState(uuid, enableVideo = enable, enableAudio = audioOpen)
                } else if (isRoomOwner() && !enable) {
                    sendAndUpdateDeviceState(uuid, enableVideo = enable, enableAudio = audioOpen)
                } else {
                    // no permission
                    onEvent(ClassRoomEvent.NoOptPermission(eventId.incrementAndGet()))
                }
            }
        }
    }

    fun enableAudio(enable: Boolean, uuid: String = _state.value.currentUserUUID) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uuid == _state.value.currentUserUUID) {
                val config = _roomConfig.value.copy(enableAudio = enable)
                database.roomConfigDao().insertOrUpdate(config)
                _roomConfig.value = config
            }

            _usersMap.value[uuid]?.run {
                if (uuid == _state.value.currentUserUUID) {
                    sendAndUpdateDeviceState(uuid, enableVideo = this.videoOpen, enableAudio = enable)
                } else if (isRoomOwner() && !enable) {
                    sendAndUpdateDeviceState(uuid, enableVideo = this.videoOpen, enableAudio = enable)
                } else {
                    // no permission
                    onEvent(ClassRoomEvent.NoOptPermission(eventId.incrementAndGet()))
                }
            }
        }
    }

    suspend fun initRoomUsers(usersUUIDs: List<String>): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, usersUUIDs)) {
                is Success -> {
                    result.data.forEach { (uuid, user) ->
                        run {
                            user.userUUID = uuid
                            if (user.userUUID == _state.value.currentUserUUID) {
                                user.audioOpen = _roomConfig.value.enableAudio
                                user.videoOpen = _roomConfig.value.enableVideo
                            }
                        }
                    }

                    addToCache(result.data)
                    addToCurrent(result.data)
                    cont.resume(true)
                }
                is ErrorResult -> {
                    cont.resumeWithException(FlatException(result.error.code, result.error.message))
                }
            }
        }
    }

    private suspend fun sendAndUpdateDeviceState(userUUID: String, enableVideo: Boolean, enableAudio: Boolean) {
        val event = RTMEvent.DeviceState(
            DeviceStateValue(
                userUUID = userUUID,
                camera = enableVideo,
                mic = enableAudio
            )
        )
        rtmApi.sendChannelCommand(event)
        updateDeviceState(event.value)
    }

    private fun onEvent(event: ClassRoomEvent) {
        viewModelScope.launch {
            _roomEvent.value = event
        }
    }

    fun notifyOperatingAreaShown(areaId: Int) {
        onEvent(ClassRoomEvent.OperatingAreaShown(areaId))
        if (areaId != ClassRoomEvent.AREA_ID_MESSAGE) {
            _messageAreaShown.value = false
        }
    }

    fun notifyRTMChannelJoined() {
        onEvent(ClassRoomEvent.RtmChannelJoined)
    }

    private fun addToCurrent(userMap: Map<String, RtcUser>) {
        val map = _usersMap.value.toMutableMap()
        userMap.forEach { (uuid, user) -> map[uuid] = user.copy() }
        _usersMap.value = map
    }

    private fun addToCache(userMap: Map<String, RtcUser>) {
        userMap.forEach { (uuid, user) -> _usersCacheMap[uuid] = user.copy() }
    }

    fun removeRtmMember(userUUID: String) {
        val map = _usersMap.value.toMutableMap()
        map.remove(userUUID)
        _usersMap.value = map
    }

    fun addRtmMember(userUUID: String) {
        if (_usersCacheMap.containsKey(userUUID)) {
            addToCurrent(mapOf(userUUID to _usersCacheMap[userUUID]!!))
            return
        }
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, listOf(userUUID))) {
                is Success -> {
                    result.data.forEach { (uuid, user) -> user.userUUID = uuid }
                    addToCache(result.data)
                    addToCurrent(result.data)
                }
                is ErrorResult -> {
                }
            }
        }
    }

    private fun intentValue(key: String): String {
        return savedStateHandle.get<String>(key)!!
    }

    fun setVideoShown(shown: Boolean) {
        _videoAreaShown.value = shown
    }

    fun setMessageAreaShown(shown: Boolean) {
        _messageAreaShown.value = shown
    }

    // RTCCommand Handle
    private fun updateDeviceState(value: DeviceStateValue) {
        val user = _usersMap.value[value.userUUID]
        if (user != null) {
            val map = _usersMap.value.toMutableMap()
            map[value.userUUID] = user.copy(audioOpen = value.mic, videoOpen = value.camera)
            _usersMap.value = map
        }
    }

    private fun updateUserState(userUUID: String, state: RTMUserState) {
        val user = _usersMap.value[userUUID]
        if (user != null) {
            val map = _usersMap.value.toMutableMap()
            map[userUUID] = user.copy(
                audioOpen = state.mic,
                videoOpen = state.camera,
                name = state.name,
                isSpeak = state.isSpeak
            )
            _usersMap.value = map
        }
    }

    private fun updateChannelState(status: ChannelStatusValue) {
        val map = _usersMap.value.toMutableMap()
        status.uStates.forEach { (userId, s) ->
            run {
                usersMap.value[userId]?.copy(
                    videoOpen = s.contains(RTMUserProp.Camera.flag, ignoreCase = true),
                    audioOpen = s.contains(RTMUserProp.Mic.flag, ignoreCase = true),
                    isSpeak = s.contains(RTMUserProp.IsSpeak.flag, ignoreCase = true),
                    isRaiseHand = s.contains(RTMUserProp.IsRaiseHand.flag, ignoreCase = true),
                )?.also { map[userId] = it }
            }
        }
        _usersMap.value = map
        _state.value = _state.value.copy(classMode = status.rMode, ban = status.ban, roomStatus = status.rStatus)
    }

    fun requestChannelStatus() {
        viewModelScope.launch {
            val user = _usersMap.value.values.firstOrNull { user ->
                user.userUUID != _state.value.currentUserUUID
            }
            user?.run {
                val roomUUID = roomUUID;
                val userInfo = appKVCenter.getUserInfo()!!
                val state = RTMUserState(
                    userInfo.name,
                    camera = _roomConfig.value.enableVideo,
                    mic = _roomConfig.value.enableAudio,
                    isSpeak = isRoomOwner(),
                )
                val value = RequestChannelStatusValue(roomUUID, listOf(userUUID), state)
                val event = RTMEvent.RequestChannelStatus(value)

                rtmApi.sendChannelCommand(event)
            }
        }
    }

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            rtmApi.sendChannelMessage(message)

            _messageList.value = _messageList.value + ChatMessage(
                _state.value.currentUserName,
                message,
                true
            )
        }
    }

    private fun isRoomOwner(): Boolean {
        return _state.value.ownerUUID == _state.value.currentUserUUID
    }

    fun onRTMEvent(event: RTMEvent, senderId: String) {
        when (event) {
            is RTMEvent.ChannelMessage -> {
                _messageList.value = _messageList.value + ChatMessage(
                    _usersMap.value[senderId]?.name ?: "",
                    event.text,
                    _state.value.currentUserUUID == senderId
                )
            }
            is RTMEvent.ChannelStatus -> {
                updateChannelState(event.value)
            }
            is RTMEvent.RequestChannelStatus -> {
                updateUserState(event.value.roomUUID, event.value.user)
                if (event.value.userUUIDs.contains(_state.value.currentUserUUID)) {
                    sendChannelStatus(senderId)
                }
            }
            is RTMEvent.DeviceState -> {
                updateDeviceState(event.value)
            }
            is RTMEvent.AcceptRaiseHand -> {
                if (senderId == _state.value.ownerUUID) {
                    val user = _usersMap.value[event.value.userUUID]
                    user?.run {
                        val map = _usersMap.value.toMutableMap()
                        map[senderId] = copy(isSpeak = event.value.accept)
                        _usersMap.value = map
                    }
                }
            }
            is RTMEvent.BanText -> _state.value = _state.value.copy(ban = event.v)
            is RTMEvent.CancelAllHandRaising -> {
                if (senderId == _state.value.ownerUUID) {
                    val map = _usersMap.value.toMutableMap()
                    map.forEach { (uuid, u) -> map[uuid] = u.copy(isRaiseHand = false) }
                    _usersMap.value = map
                }
            }
            is RTMEvent.ClassMode -> {
                if (senderId == _state.value.ownerUUID) {
                    _state.value = state.value.copy(classMode = event.classModeType)
                }
            }
            is RTMEvent.Notice -> {

            }
            is RTMEvent.RaiseHand -> {
                val user = _usersMap.value[senderId]
                user?.run {
                    val map = _usersMap.value.toMutableMap()
                    map[senderId] = copy(isRaiseHand = event.v)
                    _usersMap.value = map
                }
            }
            is RTMEvent.RoomStatus -> {
                if (senderId == _state.value.ownerUUID) {
                    _state.value = _state.value.copy(roomStatus = event.roomStatus)
                    if (_state.value.roomStatus == RoomStatus.Stopped) {
                        viewModelScope.launch {
                            eventbus.produceEvent(HomeRefreshEvent())
                        }
                    }
                }
            }
            is RTMEvent.Speak -> {
                val user = _usersMap.value[senderId]
                user?.run {
                    val map = _usersMap.value.toMutableMap()
                    map[senderId] = copy(isSpeak = event.v)
                    _usersMap.value = map
                }
            }
        }
    }

    private fun sendChannelStatus(senderId: String) {
        viewModelScope.launch {
            val uStates = HashMap<String, String>()
            usersMap.value.values.forEach {
                uStates[it.userUUID] = StringBuilder().apply {
                    if (it.isSpeak) append(RTMUserProp.IsSpeak.flag)
                    if (it.isRaiseHand) append(RTMUserProp.IsRaiseHand.flag)
                    if (it.videoOpen) append(RTMUserProp.Camera.flag)
                    if (it.audioOpen) append(RTMUserProp.Mic.flag)
                }.toString()
            }
            var channelState = RTMEvent.ChannelStatus(
                ChannelStatusValue(
                    ban = _state.value.ban,
                    rStatus = _state.value.roomStatus,
                    rMode = _state.value.classMode,
                    uStates = uStates
                )
            )
            rtmApi.sendPeerCommand(channelState, senderId)
        }
    }

    suspend fun startRoomClass(): Boolean {
        return when (roomRepository.startRoomClass(roomUUID)) {
            is Success -> {
                rtmApi.sendChannelCommand(RTMEvent.RoomStatus(RoomStatus.Started))
                _state.value = _state.value.copy(roomStatus = RoomStatus.Started)
                true
            }
            else -> false
        }
    }

    suspend fun pauseRoomClass(): Boolean {
        return when (roomRepository.pauseRoomClass(roomUUID)) {
            is Success -> {
                rtmApi.sendChannelCommand(RTMEvent.RoomStatus(RoomStatus.Paused))
                true
            }
            else -> false
        }
    }

    suspend fun stopRoomClass(): Boolean {
        return when (roomRepository.stopRoomClass(roomUUID)) {
            is Success -> {
                rtmApi.sendChannelCommand(RTMEvent.RoomStatus(RoomStatus.Stopped))
                true
            }
            else -> false
        }
    }

    fun onCopyText(text: String) {
        clipboard.putText(text)
    }

    fun requestCloudStorageFiles() {
        viewModelScope.launch {
            when (val res = cloudStorageRepository.getFileList(1)) {
                is Success -> {
                    _cloudStorageFiles.value = res.data.files
                }
            }
        }
    }

    fun insertCourseware(file: CloudStorageFile) {
        if (file.convertStep == FileConvertStep.Converting) {
            // "正在转码中，请稍后再试"
            return
        }
        // "正在插入课件……"
        val ext = file.fileURL.substringAfterLast('.').toLowerCase()
        when (ext) {
            "jpg", "jpeg", "png", "webp" -> {
                onEvent(ClassRoomEvent.InsertImage(file.fileURL))
            }
            "doc", "docx", "ppt", "pptx", "pdf" -> {
                insertDocs(file, ext)
            }
        }
    }

    private fun insertDocs(file: CloudStorageFile, ext: String) {
        val convert = ConverterV5.Builder().apply {
            setResource(file.fileURL)
            setType(if (ext == "pptx") ConvertType.Dynamic else ConvertType.Static)
            setTaskUuid(file.taskUUID)
            setTaskToken(file.taskToken)
            setCallback(object : ConverterCallbacks {
                override fun onProgress(progress: Double, convertInfo: ConversionInfo?) {

                }

                override fun onFinish(ppt: ConvertedFiles, convertInfo: ConversionInfo) {
                    val uuid = UUID.randomUUID().toString();
                    onEvent(ClassRoomEvent.InsertPpt("/${file.taskUUID}/${uuid}", ppt))
                }

                override fun onFailure(e: ConvertException) {
                    //
                }
            })
        }.build()
        convert.startConvertTask()
    }
}

data class ClassRoomState(
    // 房间的 uuid
    val roomUUID: String = "",
    // 房间类型
    val roomType: RoomType = RoomType.SmallClass,
    // 房间状态
    val roomStatus: RoomStatus = RoomStatus.Idle,
    // 房间所有者
    val ownerUUID: String = "",
    // 当前用户
    val currentUserUUID: String = "",
    // 当前用户名
    val currentUserName: String = "",
    // 房间所有者的名称
    val ownerName: String? = null,
    // 房间标题
    val title: String = "",
    // 房间开始时间
    val beginTime: Long = 0L,
    // 结束时间
    val endTime: Long = 0L,
    // 禁用
    val ban: Boolean = true,
    // 交互模式
    val classMode: ClassModeType = ClassModeType.Interaction,
) {
    val isWritable: Boolean
        get() = ownerUUID == currentUserUUID || classMode == ClassModeType.Interaction

    val isOwner: Boolean
        get() = ownerUUID == currentUserUUID

    val showStartButton: Boolean
        get() {
            return isOwner && RoomStatus.Idle == roomStatus
        }

    val needOwnerExitDialog: Boolean
        get() = isOwner && RoomStatus.Idle != roomStatus
}

sealed class ClassRoomEvent {
    companion object {
        const val AREA_ID_APPLIANCE = 1
        const val AREA_ID_PAINT = 2
        const val AREA_ID_SETTING = 3
        const val AREA_ID_MESSAGE = 4
        const val AREA_ID_CLOUD_STORAGE = 5
        const val AREA_ID_VIDEO_OP_CALL_OUT = 6
        const val AREA_ID_INVITE_DIALOG = 7
        const val AREA_ID_OWNER_EXIT_DIALOG = 8
    }

    object RtmChannelJoined : ClassRoomEvent()
    data class StartRoomResult(val success: Boolean) : ClassRoomEvent()

    data class OperatingAreaShown(val areaId: Int) : ClassRoomEvent()
    data class NoOptPermission(val id: Int) : ClassRoomEvent()
    data class InsertImage(val imageUrl: String) : ClassRoomEvent()
    data class InsertPpt(val dirpath: String, val convertedFiles: ConvertedFiles) : ClassRoomEvent()
}

data class ChatMessage(val name: String, val message: String, val isSelf: Boolean)