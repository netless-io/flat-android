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
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.android.ClipboardController
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.common.rtm.Message
import io.agora.flat.common.rtm.MessageFactory
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.*
import io.agora.flat.di.impl.Event
import io.agora.flat.di.impl.EventBus
import io.agora.flat.di.interfaces.RtcEngineProvider
import io.agora.flat.di.interfaces.RtmEngineProvider
import io.agora.flat.event.MessagesAppended
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.util.fileSuffix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.collections.HashMap

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val roomConfigRepository: RoomConfigRepository,
    private val userManager: UserManager,
    private val messageState: MessageState,
    private val rtmApi: RtmEngineProvider,
    private val rtcApi: RtcEngineProvider,
    private val eventbus: EventBus,
    private val clipboard: ClipboardController,
    private val stringFetcher: StringFetcher,
) : ViewModel() {
    private var timer: Job? = null

    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _state = MutableStateFlow(ClassRoomState.Init)
    val state = _state.asStateFlow()

    private var _videoUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val videoUsers = _videoUsers.asStateFlow()

    private var _messageUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val messageUsers = _messageUsers.asStateFlow()

    private var _cloudStorageFiles = MutableStateFlow<List<CloudStorageFile>>(mutableListOf())
    val cloudStorageFiles = _cloudStorageFiles.asStateFlow()

    private var _videoAreaShown = MutableStateFlow(true)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    private var _messageAreaShown = MutableStateFlow(false)
    val messageAreaShown = _messageAreaShown.asStateFlow()
    val messageCount = messageState.messages.map { it.size }

    private var _roomEvent = MutableStateFlow<ClassRoomEvent?>(null)
    val roomEvent = _roomEvent
    private val eventId = AtomicInteger(0)

    private val roomUUID = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)!!
    private val playInfo = savedStateHandle.get<RoomPlayInfo?>(Constants.IntentKey.ROOM_PLAY_INFO)
    private val userUUID = userRepository.getUserUUID()
    private val userName = userRepository.getUsername()

    private var _roomConfig = MutableStateFlow(RoomConfig(roomUUID))
    val roomConfig = _roomConfig.asStateFlow()

    private var _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow().filterNotNull()

    private fun tickerFlow(period: Long, initialDelay: Long = 0) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    init {
        viewModelScope.launch {
            when (val result = roomRepository.getOrdinaryRoomInfo(roomUUID)) {
                is Success -> result.data.run {
                    initRoomInfo(result.data.roomInfo)
                }
            }
        }

        viewModelScope.launch {
            if (playInfo != null) {
                _roomPlayInfo.value = playInfo
            } else when (val result = roomRepository.joinRoom(roomUUID)) {
                is Success -> {
                    _roomPlayInfo.value = result.data
                }
                is ErrorResult -> {
                    _errorMessage.value = when (result.error.code) {
                        FlatErrorCode.Web_RoomNotFound -> stringFetcher.roomNotFound()
                        FlatErrorCode.Web_RoomIsEnded -> stringFetcher.roomIsEnded()
                        else -> stringFetcher.joinRoomError(result.error.code)
                    }
                }
            }
        }

        viewModelScope.launch {
            updateRoomConfig(roomConfigRepository.getRoomConfig(roomUUID) ?: RoomConfig(roomUUID))
        }
    }

    private fun initRoomInfo(roomInfo: RoomInfo) {
        _state.value = ClassRoomState(
            roomUUID = roomUUID,
            userUUID = userUUID,
            userName = userName,
            roomType = roomInfo.roomType,
            ownerUUID = roomInfo.ownerUUID,
            ownerName = roomInfo.ownerName,
            title = roomInfo.title,
            beginTime = roomInfo.beginTime,
            endTime = roomInfo.endTime,
            roomStatus = roomInfo.roomStatus,
        )
        userManager.reset(
            roomUUID = roomUUID,
            userUUID = userUUID,
            ownerUUID = roomInfo.ownerUUID,
            scope = viewModelScope,
        )
        observerUserState()
    }

    private suspend fun updateRoomConfig(config: RoomConfig) {
        _roomConfig.value = config
        roomConfigRepository.updateRoomConfig(config)

        rtcApi.rtcEngine().muteLocalAudioStream(!config.enableAudio)
        rtcApi.rtcEngine().muteLocalVideoStream(!config.enableVideo)
    }

    private fun observerUserState() {
        viewModelScope.launch {
            userManager.currentUser.filterNotNull().collect {
                _state.value = _state.value.copy(isSpeak = it.isSpeak, isRaiseHand = it.isRaiseHand)
            }
        }

        viewModelScope.launch {
            userManager.users.collect {
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
            userManager.users.collect {
                _messageUsers.value = it
            }
        }
    }

    private fun List<RtcUser>.containOwner(): Boolean {
        return this.find { _state.value.ownerUUID == it.userUUID } != null
    }

    fun enableVideo(enable: Boolean, uuid: String = _state.value.userUUID) {
        viewModelScope.launch {
            if (isCurrentUser(uuid)) {
                val config = roomConfig.value.copy(enableVideo = enable)
                updateRoomConfig(config)
            }

            userManager.findFirstUser(uuid)?.run {
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
                val config = roomConfig.value.copy(enableAudio = enable)
                updateRoomConfig(config)
            }

            userManager.findFirstUser(uuid)?.run {
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

    fun sendGlobalEvent(event: Event) {
        viewModelScope.launch {
            eventbus.produceEvent(event)
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

    suspend fun initRoomUsers(uuids: List<String>): Boolean {
        return userManager.init(uuids = uuids)
    }

    fun removeRtmMember(userUUID: String) {
        userManager.removeUser(userUUID)
    }

    fun addRtmMember(userUUID: String) {
        userManager.addUser(userUUID)
    }

    fun setVideoAreaShown(shown: Boolean) {
        _videoAreaShown.value = shown
    }

    fun setMessageAreaShown(shown: Boolean) {
        _messageAreaShown.value = shown
    }

    // RTCCommand Handle
    private fun updateDeviceState(value: DeviceStateValue) {
        userManager.updateDeviceState(uuid = value.userUUID, audioOpen = value.mic, videoOpen = value.camera)
    }

    private fun updateUserState(userUUID: String, state: RTMUserState) {
        userManager.updateUserState(
            uuid = userUUID,
            audioOpen = state.mic,
            videoOpen = state.camera,
            name = state.name,
            isSpeak = state.isSpeak,
        )
    }

    private fun updateChannelState(status: ChannelStatusValue) {
        // update room state
        _state.value = _state.value.copy(classMode = status.rMode, ban = status.ban, roomStatus = status.rStatus)

        userManager.updateUserStates(status.uStates)
    }

    fun requestChannelStatus() {
        viewModelScope.launch {
            userManager.findFirstOtherUser()?.run {
                val state = RTMUserState(
                    name = userRepository.getUsername(),
                    camera = roomConfig.value.enableVideo,
                    mic = roomConfig.value.enableAudio,
                    isSpeak = isRoomOwner(),
                )

                val event = RTMEvent.RequestChannelStatus(RequestChannelStatusValue(roomUUID, listOf(userUUID), state))
                rtmApi.sendChannelCommand(event)
            }
        }
    }

    private fun appendMessage(message: Message) {
        viewModelScope.launch {
            eventbus.produceEvent(MessagesAppended(listOf(message)))
        }
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
                appendMessage(MessageFactory.createText(sender = senderId, event.text))
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
                    userManager.updateSpeakAndRaise(
                        event.value.userUUID,
                        isSpeak = event.value.accept,
                        isRaiseHand = false,
                    )
                }
            }
            is RTMEvent.BanText -> {
                _state.value = _state.value.copy(ban = event.v)
                appendMessage(MessageFactory.createNotice(ban = event.v))
            }
            is RTMEvent.CancelHandRaising -> {
                if (senderId == _state.value.ownerUUID) {
                    userManager.cancelHandRaising()
                }
            }
            is RTMEvent.ClassMode -> {
                if (senderId == _state.value.ownerUUID) {
                    _state.value = _state.value.copy(classMode = event.classModeType)
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
                            eventbus.produceEvent(RoomsUpdated)
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
        userManager.updateSpeakStatus(uuid = it.userUUID, isSpeak = it.speak)
    }

    private fun updateRaiseHandStatus(userUUID: String, event: RTMEvent.RaiseHand) {
        userManager.updateRaiseHandStatus(uuid = userUUID, isRaiseHand = event.v)
    }

    private fun sendChannelStatus(senderId: String) {
        viewModelScope.launch {
            val uStates = HashMap<String, String>()
            userManager.users.value.forEach {
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
            userManager.currentUser.value?.run {
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
                else -> {; }
            }
        }
    }

    val width = 120 * 3
    val height = 360 * 3

    fun startRecord() {
        viewModelScope.launch {
            val acquireResp = cloudRecordRepository.acquireRecord(roomUUID)
            if (acquireResp is Success) {
                val transcodingConfig = TranscodingConfig(
                    width,
                    height,
                    15,
                    500,
                    mixedVideoLayout = 3,
                    layoutConfig = getLayoutConfig(),
                    backgroundConfig = getBackgroundConfig()
                )
                val startResp = cloudRecordRepository.startRecordWithAgora(
                    roomUUID,
                    acquireResp.data.resourceId,
                    transcodingConfig
                )
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

    private fun updateRecordLayout() {
        viewModelScope.launch {
            val config = UpdateLayoutClientRequest(
                layoutConfig = getLayoutConfig(),
                backgroundConfig = getBackgroundConfig(),
            )
            cloudRecordRepository.updateRecordLayoutWithAgora(roomUUID, _state.value.recordState!!.resourceId, config)
        }
    }

    private fun getBackgroundConfig(): List<BackgroundConfig> {
        return _videoUsers.value.map { user: RtcUser ->
            BackgroundConfig(uid = user.rtcUID.toString(), image_url = user.avatarURL)
        }
    }

    private fun getLayoutConfig(): List<LayoutConfig> {
        return _videoUsers.value.mapIndexed { index: Int, user: RtcUser ->
            LayoutConfig(
                uid = user.rtcUID.toString(),
                x_axis = 12f / 120,
                y_axis = (8f + index * 80) / 360,
                width = 96f / 120,
                height = 72f / 360,
            )
        }
    }

    private fun startTimer() {
        timer = viewModelScope.launch {
            tickerFlow(1000, 1000).collect {
                val recordState = _state.value.recordState
                _state.value =
                    _state.value.copy(recordState = recordState?.copy(recordTime = recordState.recordTime + 1))
                updateRecordLayout()
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    fun stopRecord() {
        viewModelScope.launch {
            _state.value.recordState?.run {
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
                    _cloudStorageFiles.value = res.data.files.asReversed()
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
        when (val ext = file.fileURL.fileSuffix()) {
            "jpg", "jpeg", "png", "webp" -> {
                onEvent(ClassRoomEvent.InsertImage(file.fileURL))
            }
            "doc", "docx", "ppt", "pptx", "pdf" -> {
                insertDocs(file, ext)
            }
            "mp4" -> {
                onEvent(ClassRoomEvent.InsertVideo(file.fileURL, file.fileName))
            }
            else -> {
                // Not Support Mobile
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
                    onEvent(ClassRoomEvent.InsertPpt("/${file.taskUUID}/${uuid}", ppt, file.fileName))
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
                userManager.updateSpeakAndRaise(userUUID, isSpeak = true, isRaiseHand = false)
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

    // 房间所有者的名称
    val ownerName: String? = null,
    // 房间标题
    val title: String? = null,
    // 房间开始时间
    val beginTime: Long = 0L,
    // 结束时间
    val endTime: Long = 0L,
    // 禁用
    val ban: Boolean = false,
    // 交互模式
    val classMode: ClassModeType = ClassModeType.Interaction,

    // 当前用户
    val userUUID: String = "",
    val userName: String = "",
    val isSpeak: Boolean = false,
    val isRaiseHand: Boolean = false,

    val recordState: RecordState? = null,
) {
    val isWritable: Boolean
        get() = when (roomType) {
            RoomType.BigClass -> {
                isOwner || isSpeak
            }
            RoomType.SmallClass -> {
                classMode == ClassModeType.Interaction
            }
            else -> true
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
            if (isOwner) {
                return false
            }
            return when (roomType) {
                RoomType.OneToOne -> false
                RoomType.BigClass -> true
                RoomType.SmallClass -> classMode == ClassModeType.Interaction
            }
        }

    val needOwnerExitDialog: Boolean
        get() = isOwner && RoomStatus.Idle != roomStatus

    companion object {
        val Init = ClassRoomState()
    }
}

data class RecordState constructor(
    val resourceId: String,
    val sid: String,
    val recordTime: Long = 0,
)

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
    data class InsertPpt(val dirPath: String, val convertedFiles: ConvertedFiles, val title: String) : ClassRoomEvent()
    data class InsertVideo(val videoUrl: String, val title: String) : ClassRoomEvent()
    data class ShowDot(val id: Int) : ClassRoomEvent()
}