package io.agora.flat.ui.viewmodel

import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herewhite.sdk.ConverterCallbacks
import com.herewhite.sdk.converter.ConvertType
import com.herewhite.sdk.converter.ConverterV5
import com.herewhite.sdk.converter.ProjectorQuery
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
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.*
import io.agora.flat.di.impl.Event
import io.agora.flat.di.impl.EventBus
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.event.MessagesAppended
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.util.Ticker
import io.agora.flat.util.coursewareType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val cloudRecordRepository: CloudRecordRepository,
    private val roomConfigRepository: RoomConfigRepository,
    private val userManager: UserManager,
    private val messageState: MessageState,
    private val rtmApi: RtmApi,
    private val rtcApi: RtcApi,
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
    private val periodicUUID = savedStateHandle.get<String>(Constants.IntentKey.PERIODIC_UUID)
    private val playInfo = savedStateHandle.get<RoomPlayInfo?>(Constants.IntentKey.ROOM_PLAY_INFO)
    private val quickStart = savedStateHandle.get<Boolean?>(Constants.IntentKey.ROOM_QUICK_START)
    private val currentUserUUID = userRepository.getUserUUID()
    private val currentUserName = userRepository.getUsername()

    private var _roomConfig = MutableStateFlow(RoomConfig(roomUUID))
    val roomConfig = _roomConfig.asStateFlow()

    private var _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow().filterNotNull()

    init {
        viewModelScope.launch {
            when (val result = roomRepository.getOrdinaryRoomInfo(roomUUID)) {
                is Success -> result.data.run {
                    initRoomInfo(result.data.roomInfo)
                }
                is Failure -> {
                    _errorMessage.value = "fetch room info error"
                }
            }
            if (playInfo != null) {
                _roomPlayInfo.value = playInfo
            } else when (val result = roomRepository.joinRoom(periodicUUID ?: roomUUID)) {
                is Success -> _roomPlayInfo.value = result.data
                is Failure -> {
                    _errorMessage.value = when (result.error.code) {
                        FlatErrorCode.Web_RoomNotFound -> stringFetcher.roomNotFound()
                        FlatErrorCode.Web_RoomIsEnded -> stringFetcher.roomIsEnded()
                        else -> stringFetcher.joinRoomError(result.error.code)
                    }
                }
            }
        }

        viewModelScope.launch {
            roomEvent.collect {
                if (it is ClassRoomEvent.RtmChannelJoined) {
                    if (quickStart == true) {
                        startClass()
                    }
                }
            }
        }
    }

    private fun initRoomInfo(roomInfo: RoomInfo) {
        userManager.reset(
            roomUUID = roomUUID,
            userUUID = currentUserUUID,
            ownerUUID = roomInfo.ownerUUID,
            scope = viewModelScope,
        )

        // TODO ClassModeType is a configuration of SmallClass
        val classMode = when (roomInfo.roomType) {
            RoomType.BigClass -> ClassModeType.Lecture
            else -> ClassModeType.Interaction
        }

        _state.value = ClassRoomState(
            roomUUID = roomUUID,
            inviteCode = roomInfo.inviteCode,
            userUUID = currentUserUUID,
            userName = currentUserName,
            roomType = roomInfo.roomType,
            ownerUUID = roomInfo.ownerUUID,
            ownerName = roomInfo.ownerUserName,
            title = roomInfo.title,
            beginTime = roomInfo.beginTime,
            endTime = roomInfo.endTime,
            roomStatus = roomInfo.roomStatus,
            region = roomInfo.region,
            classMode = classMode,
        )

        observerUserState()
    }

    private suspend fun getInitRoomConfig(): RoomConfig {
        val config = roomConfigRepository.getRoomConfig(roomUUID) ?: RoomConfig(roomUUID)
        return config.copy(
            enableAudio = defaultWritable(roomPlayInfo.value!!) && config.enableAudio,
            enableVideo = defaultWritable(roomPlayInfo.value!!) && config.enableVideo,
        )
    }

    private fun observerUserState() {
        viewModelScope.launch {
            userManager.currentUser.filterNotNull().collect {
                _state.value = _state.value.copy(isSpeak = it.isSpeak, isRaiseHand = it.isRaiseHand)
                _roomConfig.value = roomConfig.value.copy(
                    enableVideo = it.videoOpen and it.isSpeak,
                    enableAudio = it.audioOpen and it.isSpeak,
                )
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
                roomConfigRepository.updateRoomConfig(
                    roomConfig.value.copy(enableAudio = enable)
                )
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
                roomConfigRepository.updateRoomConfig(
                    roomConfig.value.copy(enableAudio = enable)
                )
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

    fun notifyRTMChannelJoined() {
        onEvent(ClassRoomEvent.RtmChannelJoined)
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

    private fun updateRequestUserState(userUUID: String, state: RTMUserState) {
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

    suspend fun initChannelStatus() {
        userManager.initUsers(uuids = rtmApi.getMembers().map { it.userId })
        // update local user state
        val initRoomConfig = getInitRoomConfig()
        userManager.updateUserState(
            uuid = currentUserUUID,
            audioOpen = initRoomConfig.enableAudio,
            videoOpen = initRoomConfig.enableVideo,
            name = currentUserName,
            isSpeak = isRoomOwner(),
        )
        userManager.findFirstOtherUser()?.run {
            val state = RTMUserState(
                name = userRepository.getUsername(),
                camera = roomConfig.value.enableVideo,
                mic = roomConfig.value.enableAudio,
                isSpeak = isRoomOwner(),
            )

            val event = RTMEvent.RequestChannelStatus(
                RequestChannelStatusValue(roomUUID, listOf(userUUID), state)
            )
            rtmApi.sendChannelCommand(event)
        }
    }

    private fun appendMessage(message: Message) {
        viewModelScope.launch {
            eventbus.produceEvent(MessagesAppended(listOf(message)))
        }
    }

    private fun isRoomOwner(): Boolean {
        return _state.value.ownerUUID == currentUserUUID
    }

    private fun isCurrentUser(userId: String): Boolean {
        return userId == currentUserUUID
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
                updateRequestUserState(senderId, event.value.user)
                if (event.value.userUUIDs.contains(currentUserUUID)) {
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
                userManager.handleAllOffStage()
            }
            is RTMEvent.AllOffStage -> {
                userManager.handleAllOffStage()
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
                }
                else -> {; }
            }
        }
    }

    val width = 120 * 3
    val height = 360 * 3

    private var startRecordJob: Job? = null

    fun startRecord() {
        if (videoUsers.value.isEmpty()) {
            startRecordJob = viewModelScope.launch {
                videoUsers.collect {
                    if (videoUsers.value.isNotEmpty()) {
                        startRecord()
                    }
                }
            }
            return
        }
        startRecordJob?.cancel()

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
            Ticker.tickerFlow(1000, 1000).collect {
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
                    _cloudStorageFiles.value = res.data.files.filter {
                        it.convertStep == FileConvertStep.Done || it.convertStep == FileConvertStep.None
                    }
                }
            }
        }
    }

    fun insertCourseware(file: CloudStorageFile) {
        viewModelScope.launch {
            // "正在插入课件……"
            when (file.fileURL.coursewareType()) {
                CoursewareType.Image -> {
                    val imageSize = loadImageSize(file.fileURL)
                    onEvent(ClassRoomEvent.InsertImage(file.fileURL, imageSize.width, imageSize.height))
                }
                CoursewareType.Audio, CoursewareType.Video -> {
                    onEvent(ClassRoomEvent.InsertVideo(file.fileURL, file.fileName))
                }
                CoursewareType.DocStatic -> {
                    insertDocs(file, false)
                }
                CoursewareType.DocDynamic -> {
                    if (file.resourceType == ResourceType.WhiteboardConvert) {
                        insertDocs(file, true)
                    } else if (file.resourceType == ResourceType.WhiteboardProjector) {
                        insertProjectorDocs(file)
                    }
                }
                else -> {
                    // Not Support Mobile
                }
            }
        }
    }

    /**
     * This code is used as an example, the application needs to manage io and async itself.
     * The application may get the image width and height from the api
     *
     * @param src
     */
    private suspend fun loadImageSize(src: String): ImageSize {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                URL(src).openStream().use {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(it, null, options)
                    ImageSize(options.outWidth, options.outHeight)
                }
            } catch (e: IOException) {
                ImageSize(720, 360)
            }
        }
    }

    private fun insertDocs(file: CloudStorageFile, dynamic: Boolean) {
        val convert = ConverterV5.Builder().apply {
            setResource(file.fileURL)
            setType(if (dynamic) ConvertType.Dynamic else ConvertType.Static)
            setTaskUuid(file.taskUUID)
            setTaskToken(file.taskToken)
            setPoolInterval(2000)
            setCallback(object : ConverterCallbacks {
                override fun onProgress(progress: Double, convertInfo: ConversionInfo?) {
                }

                override fun onFinish(ppt: ConvertedFiles, convertInfo: ConversionInfo) {
                    onEvent(ClassRoomEvent.InsertPpt(
                        "/${file.taskUUID}/${UUID.randomUUID()}",
                        ppt,
                        file.fileName
                    ))
                }

                override fun onFailure(e: ConvertException) {
                }
            })
        }.build()
        convert.startConvertTask()
    }

    private fun insertProjectorDocs(file: CloudStorageFile) {
        val projectorQuery = ProjectorQuery.Builder()
            .setTaskToken(file.taskToken)
            .setTaskUuid(file.taskUUID)
            .setPoolInterval(2000)
            .setCallback(object : ProjectorQuery.Callback {
                override fun onProgress(progress: Double, convertInfo: ProjectorQuery.QueryResponse) {

                }

                override fun onFinish(response: ProjectorQuery.QueryResponse) {
                    onEvent(ClassRoomEvent.InsertProjectorPpt(
                        file.taskUUID,
                        response.prefix,
                        file.fileName
                    ))
                }

                override fun onFailure(e: ConvertException?) {

                }
            })
            .build()
        projectorQuery.startQuery()
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

    fun acceptRaiseHand(userUUID: String) {
        viewModelScope.launch {
            if (isRoomOwner()) {
                rtmApi.sendChannelCommand(RTMEvent.AcceptRaiseHand(AcceptRaiseHandValue(userUUID, true)))
                userManager.updateSpeakAndRaise(userUUID, isSpeak = true, isRaiseHand = false)
            }
        }
    }

    fun updateClassMode(classMode: ClassModeType) {
        viewModelScope.launch {
            rtmApi.sendChannelCommand(RTMEvent.ClassMode(classMode))
            _state.value = _state.value.copy(classMode = classMode)
        }
    }

    fun defaultWritable(playInfo: RoomPlayInfo): Boolean {
        return when (playInfo.roomType) {
            RoomType.OneToOne -> true
            RoomType.SmallClass -> true
            RoomType.BigClass -> playInfo.ownerUUID == currentUserUUID
        }
    }
}

data class ClassRoomState(
    // 房间的 uuid
    val roomUUID: String = "",
    // 房间邀请码
    val inviteCode: String = "",
    // 房间类型
    val roomType: RoomType = RoomType.BigClass,
    // 房间状态
    val roomStatus: RoomStatus = RoomStatus.Idle,
    //
    val region: String = "",
    // 房间所有者
    val ownerUUID: String = "ownerUUID",

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
    val classMode: ClassModeType = ClassModeType.Lecture,

    // 当前用户
    // TODO currentUserID
    val userUUID: String = "userUUID",
    val userName: String = "",
    val isSpeak: Boolean = ownerUUID == userUUID,
    val isRaiseHand: Boolean = false,

    val recordState: RecordState? = null,
) {
    val isWritable: Boolean
        get() {
            if (ban) return isOwner
            return when (roomType) {
                RoomType.BigClass -> {
                    isOwner || isSpeak
                }
                RoomType.SmallClass -> {
                    isOwner || isSpeak || classMode == ClassModeType.Interaction
                }
                RoomType.OneToOne -> true
            }
        }

    val isOwner: Boolean
        get() = ownerUUID == userUUID

    val isRecording: Boolean
        get() = recordState != null

    val showChangeClassMode: Boolean
        get() = roomType == RoomType.SmallClass

    val showRaiseHand: Boolean
        get() {
            if (ban) return false
            return !isWritable and when (roomType) {
                RoomType.OneToOne -> false
                RoomType.BigClass -> !isOwner
                RoomType.SmallClass -> !isOwner && classMode == ClassModeType.Lecture
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

data class ImageSize(val width: Int, val height: Int)

sealed class ClassRoomEvent {
    object RtmChannelJoined : ClassRoomEvent()
    data class StartRoomResult(val success: Boolean) : ClassRoomEvent()

    data class NoOptPermission(val id: Int) : ClassRoomEvent()
    data class InsertImage(val imageUrl: String, val width: Int, val height: Int) : ClassRoomEvent()
    data class InsertPpt(val dirPath: String, val convertedFiles: ConvertedFiles, val title: String) : ClassRoomEvent()
    data class InsertProjectorPpt(val taskUuid: String, val prefixUrl: String, val title: String) : ClassRoomEvent()
    data class InsertVideo(val videoUrl: String, val title: String) : ClassRoomEvent()
}