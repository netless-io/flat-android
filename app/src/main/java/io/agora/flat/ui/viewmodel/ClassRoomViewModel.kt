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
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.AppModule
import io.agora.flat.di.interfaces.EventBus
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.event.HomeRefreshEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    private val cloudRecordRepository: CloudRecordRepository,
    private val savedStateHandle: SavedStateHandle,
    private val database: AppDatabase,
    @AppModule.GlobalData private val appKVCenter: AppKVCenter,
    private val rtmApi: RtmEngineProvider,
    private val eventbus: EventBus,
    private val clipboard: ClipboardController
) : ViewModel() {
    private var timer: Job? = null

    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _state: MutableStateFlow<ClassRoomState>
    val state: StateFlow<ClassRoomState>
        get() = _state

    private lateinit var userState: UserState

    private var _videoUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val videoUsers = _videoUsers.asStateFlow()

    private var _messageUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val messageUsers = _messageUsers.asStateFlow()

    private var _messageList = MutableStateFlow<List<RTMMessage>>(emptyList())
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

    private var roomUUID: String
    private var userUUID: String

    fun tickerFlow(period: Long, initialDelay: Long = 0) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    init {
        roomUUID = intentValue(Constants.IntentKey.ROOM_UUID)
        userUUID = appKVCenter.getUserInfo()!!.uuid
        val userName = appKVCenter.getUserInfo()!!.name

        _state = MutableStateFlow(
            ClassRoomState(
                roomUUID = roomUUID,
                userUUID = userUUID,
                currentUser = RtcUser(name = userName, userUUID = userUUID)
            )
        )

        viewModelScope.launch {
            when (val result = roomRepository.joinRoom(roomUUID)) {
                is Success -> {
                    userState = UserState(roomUUID = roomUUID, userUUID = userUUID, ownerUUID = result.data.ownerUUID)
                    observerUserState()
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

            viewModelScope.launch(Dispatchers.IO) {
                _roomConfig.value = database.roomConfigDao().getConfigById(roomUUID) ?: RoomConfig(roomUUID)
            }
        }
    }

    private fun observerUserState() {
        viewModelScope.launch {
            userState.currentUser.filterNotNull().collect {
                _state.value = _state.value.copy(isSpeak = it.isSpeak, currentUser = it.copy())
            }
        }

        viewModelScope.launch {
            userState.users.collect {
                val users = it.filter { value ->
                    when (_state.value.roomType) {
                        RoomType.BigClass -> (value.userUUID == _state.value.ownerUUID || value.isSpeak)
                        else -> true
                    }
                }.toMutableList()

                if (it.isNotEmpty() && !users.containOwner()) {
                    users.add(0, RtcUser(rtcUID = RtcUser.NOT_JOIN_RTC_UID, userUUID = _state.value.ownerUUID))
                }

                _videoUsers.value = users
            }
        }

        viewModelScope.launch {
            userState.users.collect {
                _messageUsers.value = it
            }
        }
    }

    private fun List<RtcUser>.containOwner(): Boolean {
        return this.find { _state.value.ownerUUID == it.userUUID } != null
    }

    fun enableVideo(enable: Boolean, uuid: String = _state.value.userUUID) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isCurrentUser(uuid)) {
                val config = _roomConfig.value.copy(enableVideo = enable)
                database.roomConfigDao().insertOrUpdate(config)
                _roomConfig.value = config
            }

            userState.findUser(uuid)?.run {
                if (isCurrentUser(uuid)) {
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

    fun enableAudio(enable: Boolean, uuid: String = _state.value.userUUID) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isCurrentUser(uuid)) {
                val config = _roomConfig.value.copy(enableAudio = enable)
                database.roomConfigDao().insertOrUpdate(config)
                _roomConfig.value = config
            }

            userState.findUser(uuid)?.run {
                if (isCurrentUser(uuid)) {
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

    suspend fun initRoomUsers(uuids: List<String>): Boolean = suspendCoroutine { cont ->
        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, uuids)) {
                is Success -> {
                    result.data.forEach { (uuid, user) ->
                        run {
                            user.userUUID = uuid
                            if (user.userUUID == _state.value.userUUID) {
                                user.audioOpen = _roomConfig.value.enableAudio
                                user.videoOpen = _roomConfig.value.enableVideo
                            }
                        }
                    }
                    userState.addToCache(result.data)
                    userState.addToCurrent(result.data.values.toList())
                    cont.resume(true)
                }
                is ErrorResult -> {
                    cont.resumeWithException(FlatException(result.error.code, result.error.message))
                }
            }
        }
    }

    private suspend fun sendAndUpdateDeviceState(userUUID: String, enableVideo: Boolean, enableAudio: Boolean) {
        val event = RTMEvent.DeviceState(DeviceStateValue(userUUID, enableVideo, enableAudio))
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

    fun removeRtmMember(userUUID: String) {
        userState.removeUser(userUUID)
    }

    fun addRtmMember(userUUID: String) {
        if (userState.hasCache(userUUID)) {
            userState.addToCurrent(userUUID)
            return
        }

        viewModelScope.launch {
            when (val result = roomRepository.getRoomUsers(roomUUID, listOf(userUUID))) {
                is Success -> {
                    result.data.forEach { (uuid, user) -> user.userUUID = uuid }
                    userState.addToCache(result.data)
                    userState.addToCurrent(result.data.values.toList())
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
        userState.findUser(value.userUUID)?.run {
            userState.updateUser(copy(audioOpen = value.mic, videoOpen = value.camera))
        }
    }

    private fun updateUserState(userUUID: String, state: RTMUserState) {
        userState.findUser(userUUID)?.run {
            userState.updateUser(
                copy(audioOpen = state.mic, videoOpen = state.camera, name = state.name, isSpeak = state.isSpeak)
            )
        }
    }

    private fun updateChannelState(status: ChannelStatusValue) {
        status.uStates.forEach { (userId, s) ->
            userState.findUser(userId)?.copy(
                videoOpen = s.contains(RTMUserProp.Camera.flag, ignoreCase = true),
                audioOpen = s.contains(RTMUserProp.Mic.flag, ignoreCase = true),
                isSpeak = s.contains(RTMUserProp.IsSpeak.flag, ignoreCase = true),
                isRaiseHand = s.contains(RTMUserProp.IsRaiseHand.flag, ignoreCase = true),
            )?.also {
                userState.updateUser(it)
            }
        }
        _state.value = _state.value.copy(classMode = status.rMode, ban = status.ban, roomStatus = status.rStatus)
    }

    fun requestChannelStatus() {
        viewModelScope.launch {
            userState.findFirstOtherUser()?.run {
                val state = RTMUserState(
                    name = _state.value.currentUser.name,
                    camera = _roomConfig.value.enableVideo,
                    mic = _roomConfig.value.enableAudio,
                    isSpeak = isRoomOwner(),
                )
                val event = RTMEvent.RequestChannelStatus(RequestChannelStatusValue(roomUUID, listOf(userUUID), state))

                rtmApi.sendChannelCommand(event)
            }
        }
    }

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            rtmApi.sendChannelMessage(message)
            appendMessage(ChatMessage(name = _state.value.currentUser.name, message = message, isSelf = true))
        }
    }

    private fun appendMessage(message: RTMMessage) {
        _messageList.value = _messageList.value + message
    }

    private fun isRoomOwner(): Boolean {
        return _state.value.ownerUUID == _state.value.userUUID
    }

    private fun isCurrentUser(userUUID: String): Boolean {
        return userUUID == _state.value.userUUID
    }

    fun isCreator(userUUID: String): Boolean {
        return userUUID == _state.value.ownerUUID
    }

    fun onRTMEvent(event: RTMEvent, senderId: String) {
        when (event) {
            is RTMEvent.ChannelMessage -> {
                appendMessage(
                    ChatMessage(
                        userState.findUser(senderId)?.name ?: "",
                        event.text,
                        _state.value.userUUID == senderId
                    )
                )
            }
            is RTMEvent.ChannelStatus -> {
                updateChannelState(event.value)
            }
            is RTMEvent.RequestChannelStatus -> {
                updateUserState(event.value.roomUUID, event.value.user)
                if (event.value.userUUIDs.contains(_state.value.userUUID)) {
                    sendChannelStatus(senderId)
                }
            }
            is RTMEvent.DeviceState -> {
                updateDeviceState(event.value)
            }
            is RTMEvent.AcceptRaiseHand -> {
                if (senderId == _state.value.ownerUUID) {
                    userState.findUser(event.value.userUUID)?.run {
                        userState.updateUser(copy(isSpeak = event.value.accept, isRaiseHand = false))
                    }
                }
            }
            is RTMEvent.BanText -> {
                _state.value = _state.value.copy(ban = event.v)
                appendMessage(NoticeMessage(ban = event.v))
            }
            is RTMEvent.CancelHandRaising -> {
                if (senderId == _state.value.ownerUUID) {
                    userState.cancelHandRaising()
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
                updateRaiseHandStatus(senderId, event)
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
                event.v.forEach {
                    updateSpeakStatus(it)
                }
            }
        }
    }

    private fun updateSpeakStatus(it: SpeakItem) {
        userState.findUser(it.userUUID)?.run {
            userState.updateUser(copy(isSpeak = it.speak))
        }
    }

    private fun updateRaiseHandStatus(userUUID: String, event: RTMEvent.RaiseHand) {
        userState.findUser(userUUID)?.run {
            userState.updateUser(copy(isRaiseHand = event.v))
        }
    }

    private fun sendChannelStatus(senderId: String) {
        viewModelScope.launch {
            val uStates = HashMap<String, String>()
            userState.users.value.forEach {
                uStates[it.userUUID] = StringBuilder().apply {
                    if (it.isSpeak) append(RTMUserProp.IsSpeak.flag)
                    if (it.isRaiseHand) append(RTMUserProp.IsRaiseHand.flag)
                    if (it.videoOpen) append(RTMUserProp.Camera.flag)
                    if (it.audioOpen) append(RTMUserProp.Mic.flag)
                }.toString()
            }
            val channelState = RTMEvent.ChannelStatus(
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

    fun sendRaiseHand() {
        viewModelScope.launch {
            userState.findCurrentUser()?.run {
                if (this.isSpeak) {
                    return@run
                }
                val event = RTMEvent.RaiseHand(!isRaiseHand)
                rtmApi.sendChannelCommand(event)
                updateRaiseHandStatus(_state.value.userUUID, event)
            }
        }
    }

    fun startClass() {
        viewModelScope.launch {
            when (roomRepository.startRoomClass(roomUUID)) {
                is Success -> {
                    rtmApi.sendChannelCommand(RTMEvent.RoomStatus(RoomStatus.Started))
                    _state.value = _state.value.copy(roomStatus = RoomStatus.Started)
                    startRecord()
                }
            }
        }
    }

    fun startRecord() {
        viewModelScope.launch {
            val acquireResp = cloudRecordRepository.acquireRecord(roomUUID)
            if (acquireResp is Success) {
                val startResp = cloudRecordRepository.startRecordWithAgora(roomUUID, acquireResp.data.resourceId)
                if (startResp is Success) {
                    _state.value = _state.value.copy(
                        recordState = RecordState(
                            resourceId = startResp.data.resourceId,
                            sid = startResp.data.sid
                        )
                    )
                    startTimer()
                }
            }
        }
    }

    private fun startTimer() {
        timer = viewModelScope.launch {
            tickerFlow(1000, 1000).collect {
                val recordState = _state.value.recordState
                _state.value = _state.value.copy(recordState = recordState?.copy(recordTime = recordState.recordTime + 1))
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    fun stopRecord() {
        viewModelScope.launch {
            state.value.recordState?.run {
                val resp = cloudRecordRepository.stopRecordWithAgora(
                    roomUUID,
                    resourceId,
                    sid,
                )
                if (resp is Success) {
                    _state.value = _state.value.copy(recordState = null)
                }
                stopTimer()
            }
        }
    }

    suspend fun pauseClass(): Boolean {
        return when (roomRepository.pauseRoomClass(roomUUID)) {
            is Success -> {
                rtmApi.sendChannelCommand(RTMEvent.RoomStatus(RoomStatus.Paused))
                true
            }
            else -> false
        }
    }

    suspend fun stopClass(): Boolean {
        return when (roomRepository.stopRoomClass(roomUUID)) {
            is Success -> {
                rtmApi.sendChannelCommand(RTMEvent.RoomStatus(RoomStatus.Stopped))
                stopRecord()
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
        when (val ext = file.fileURL.substringAfterLast('.').toLowerCase()) {
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
                    val uuid = UUID.randomUUID().toString()
                    onEvent(ClassRoomEvent.InsertPpt("/${file.taskUUID}/${uuid}", ppt))
                }

                override fun onFailure(e: ConvertException) {
                    //
                }
            })
        }.build()
        convert.startConvertTask()
    }

    fun closeSpeak(userUUID: String) {
        viewModelScope.launch {
            if (isRoomOwner() || isCurrentUser(userUUID)) {
                rtmApi.sendChannelCommand(RTMEvent.Speak(listOf(SpeakItem(userUUID, false))))
                updateSpeakStatus(SpeakItem(userUUID, false))
            }
        }
    }

    fun acceptSpeak(userUUID: String) {
        viewModelScope.launch {
            if (isRoomOwner()) {
                rtmApi.sendChannelCommand(RTMEvent.Speak(listOf(SpeakItem(userUUID, true))))
                userState.findUser(userUUID)?.run {
                    userState.updateUser(copy(isSpeak = true, isRaiseHand = false))
                }
            }
        }
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
    val userUUID: String = "",
    // 当前用户
    val currentUser: RtcUser,
    // 房间所有者的名称
    val ownerName: String? = null,
    // 房间标题
    val title: String = "",
    // 房间开始时间
    val beginTime: Long = 0L,
    // 结束时间
    val endTime: Long = 0L,
    // 禁用
    val ban: Boolean = false,
    // 交互模式
    val classMode: ClassModeType = ClassModeType.Interaction,

    val isSpeak: Boolean = false,

    val recordState: RecordState? = null,
) {
    val isWritable: Boolean
        get() {
            return when (roomType) {
                RoomType.BigClass -> {
                    ownerUUID == userUUID || isSpeak
                }
                RoomType.SmallClass -> {
                    classMode == ClassModeType.Interaction
                }
                else -> true
            }
        }

    val isOwner: Boolean
        get() = ownerUUID == userUUID

    val showStartButton: Boolean
        get() {
            return isOwner && RoomStatus.Idle == roomStatus
        }

    val isRecording: Boolean
        get() = recordState != null

    val showRaiseHand: Boolean
        get() {
            return !isOwner
        }

    val needOwnerExitDialog: Boolean
        get() = isOwner && RoomStatus.Idle != roomStatus
}

data class RecordState constructor(
    val resourceId: String,
    val sid: String,
    val recordTime: Long = 0
)

class UserState(
    private val roomUUID: String,
    private val userUUID: String,
    private val ownerUUID: String,
    // 缓存用户信息，降低web服务压力
    private var usersCacheMap: MutableMap<String, RtcUser> = mutableMapOf(),
    private var creator: RtcUser? = null,
    private var speakingJoiners: MutableList<RtcUser> = mutableListOf(),
    private var handRaisingJoiners: MutableList<RtcUser> = mutableListOf(),
    private var otherJoiners: MutableList<RtcUser> = mutableListOf()
) {
    private var _users = MutableStateFlow<List<RtcUser>>(emptyList())
    val users = _users.asStateFlow()

    private var _currentUser = MutableStateFlow<RtcUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun addToCache(userMap: Map<String, RtcUser>) {
        userMap.forEach { (uuid, user) -> usersCacheMap[uuid] = user.copy() }
    }

    fun addToCurrent(userUUID: String) {
        usersCacheMap[userUUID]?.run {
            addToCurrent(listOf(this))
        }
    }

    fun addToCurrent(userList: List<RtcUser>) {
        userList.forEach {
            if (it.userUUID == userUUID) {
                _currentUser.value = it
            }
            when {
                it.userUUID == ownerUUID -> {
                    creator = it
                }
                it.isSpeak -> {
                    val index = speakingJoiners.indexOfFirst { user -> user.userUUID == it.userUUID }
                    if (index >= 0) {
                        speakingJoiners.removeAt(index)
                    }
                    speakingJoiners.add(it)
                }
                it.isRaiseHand -> {
                    val index = handRaisingJoiners.indexOfFirst { user -> user.userUUID == it.userUUID }
                    if (index >= 0) {
                        handRaisingJoiners.removeAt(index)
                    }
                    handRaisingJoiners.add(it)
                }
                else -> {
                    val index = otherJoiners.indexOfFirst { user -> user.userUUID == it.userUUID }
                    if (index >= 0) {
                        otherJoiners.removeAt(index)
                    }
                    otherJoiners.add(it)
                }
            }
        }

        notifyUsers()
    }

    fun hasCache(userUUID: String): Boolean {
        return usersCacheMap.containsKey(userUUID)
    }

    fun removeUser(userUUID: String, notify: Boolean = true) {
        if (userUUID == ownerUUID) {
            creator = null
        }
        speakingJoiners.removeAll { it.userUUID == userUUID }
        handRaisingJoiners.removeAll { it.userUUID == userUUID }
        otherJoiners.removeAll { it.userUUID == userUUID }

        if (notify) {
            notifyUsers()
        }
    }

    fun updateUser(user: RtcUser, notify: Boolean = true) {
        removeUser(user.userUUID, false)
        addToCurrent(listOf(user))
        if (notify) {
            notifyUsers()
        }
    }

    private fun notifyUsers() {
        val ranked = mutableListOf<RtcUser>()
        ranked += speakingJoiners
        ranked += handRaisingJoiners
        creator?.run {
            ranked += this
        }
        ranked += otherJoiners

        _users.value = ranked
    }

    fun findCurrentUser(): RtcUser? {
        return users.value.find { it.userUUID == userUUID }
    }

    fun findFirstOtherUser(): RtcUser? {
        return users.value.find { it.userUUID != userUUID }
    }

    fun findUser(uuid: String): RtcUser? {
        return users.value.find { it.userUUID == uuid }
    }

    fun cancelHandRaising() {
        val list = handRaisingJoiners.toMutableList()
        handRaisingJoiners.clear()
        list.forEach { it.isRaiseHand = false }
        otherJoiners.addAll(list)

        notifyUsers()
    }
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
        const val AREA_ID_USER_LIST = 9
    }

    object RtmChannelJoined : ClassRoomEvent()
    data class StartRoomResult(val success: Boolean) : ClassRoomEvent()

    data class OperatingAreaShown(val areaId: Int) : ClassRoomEvent()
    data class NoOptPermission(val id: Int) : ClassRoomEvent()
    data class InsertImage(val imageUrl: String) : ClassRoomEvent()
    data class InsertPpt(val dirPath: String, val convertedFiles: ConvertedFiles) : ClassRoomEvent()
}

sealed class RTMMessage()
data class ChatMessage(val name: String = "", val message: String = "", val isSelf: Boolean = false) : RTMMessage()
data class NoticeMessage(val ban: Boolean) : RTMMessage()