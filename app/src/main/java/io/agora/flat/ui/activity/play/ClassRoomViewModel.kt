package io.agora.flat.ui.activity.play

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
import io.agora.flat.common.FlatException
import io.agora.flat.common.android.ClipboardController
import io.agora.flat.common.board.BoardRoom
import io.agora.flat.common.rtm.*
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.impl.Event
import io.agora.flat.di.impl.EventBus
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.event.MessagesAppended
import io.agora.flat.event.NoOptPermission
import io.agora.flat.event.RtmChannelJoined
import io.agora.flat.ui.manager.RecordManager
import io.agora.flat.ui.manager.RoomErrorManager
import io.agora.flat.ui.manager.UserManager
import io.agora.flat.ui.viewmodel.ChatMessageManager
import io.agora.flat.util.coursewareType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.URL
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val cloudStorageRepository: CloudStorageRepository,
    private val roomConfigRepository: RoomConfigRepository,
    private val userManager: UserManager,
    private val recordManager: RecordManager,
    private val messageManager: ChatMessageManager,
    private val roomErrorManager: RoomErrorManager,
    private val rtmApi: RtmApi,
    private val boardRoom: BoardRoom,
    private val syncedClassState: SyncedClassState,
    private val eventbus: EventBus,
    private val clipboard: ClipboardController,
    private val logger: Logger,
) : ViewModel() {
    private var _state = MutableStateFlow<ClassRoomState?>(null)
    val state = _state.asStateFlow()

    private fun takeInitState() = state.filterNotNull().take(1)

    private var _videoUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val videoUsers = _videoUsers.asStateFlow()

    // cloud record state
    val recordState get() = recordManager.observeRecordState()

    val rtmSuccess get() = eventbus.events.filterIsInstance<RtmChannelJoined>().take(1)

    val noOptPermission get() = eventbus.events.filterIsInstance<NoOptPermission>()

    val messageUsers = userManager.observeUsers()

    private var _cloudStorageFiles = MutableStateFlow<List<CloudStorageFile>>(mutableListOf())
    val cloudStorageFiles = _cloudStorageFiles.asStateFlow()

    private var _videoAreaShown = MutableStateFlow(true)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    private var _messageAreaShown = MutableStateFlow(false)
    val messageAreaShown = _messageAreaShown.asStateFlow()

    val messageCount = messageManager.messages.map { it.size }

    private val roomUUID: String = checkNotNull(savedStateHandle[Constants.IntentKey.ROOM_UUID])
    private val periodicUUID: String? = savedStateHandle[Constants.IntentKey.PERIODIC_UUID]
    private val playInfo: RoomPlayInfo? = savedStateHandle[Constants.IntentKey.ROOM_PLAY_INFO]
    private val quickStart: Boolean = savedStateHandle[Constants.IntentKey.ROOM_QUICK_START] ?: false
    private val currentUserUUID = userRepository.getUserUUID()
    private val currentUserName = userRepository.getUsername()

    init {
        viewModelScope.launch {
            try {
                loadRoomState()
            } catch (e: Exception) {
                roomErrorManager.notifyError("fetch class room state error", e)
            }
        }

        viewModelScope.launch {
            takeInitState().collect { state ->
                logger.i("start joining rtm channel")
                try {
                    rtmApi.login(rtmToken = state.rtmToken, channelId = roomUUID, userUUID = currentUserUUID)
                    observerRtmEvent()
                    userManager.initUsers(rtmApi.getMembers().map { it.userId })
                    observerUserState()
                    logger.i("join rtm success")
                    notifyRTMChannelJoined()
                } catch (e: FlatException) {
                    roomErrorManager.notifyError("rtm join exception", e)
                }
            }
        }

        viewModelScope.launch {
            rtmSuccess.collect {
                if (quickStart) startClass()
            }
        }

        /**
         * It can be determined that the status callback is made after the successful joining of the room
         */
        observeSyncedState()
    }

    // any of the job’s children leads to an immediate failure of its parent.
    private suspend fun loadRoomState() = coroutineScope {
        val deferredOne = async { roomRepository.getOrdinaryRoomInfo(roomUUID).getOrThrow().roomInfo }
        val deferredTwo = async {
            return@async playInfo ?: roomRepository.joinRoom(periodicUUID ?: roomUUID).getOrThrow()
        }
        initClassRoomState(deferredOne.await(), deferredTwo.await())
    }

    private suspend fun initClassRoomState(roomInfo: RoomInfo, joinRoomInfo: RoomPlayInfo) {
        val config = roomConfigRepository.getRoomConfig(roomUUID) ?: RoomConfig(roomUUID)
        val isSpeak = when (roomInfo.roomType) {
            RoomType.OneToOne -> true
            RoomType.SmallClass -> true
            RoomType.BigClass -> roomInfo.ownerUUID == currentUserUUID
        }

        val initState = ClassRoomState(
            userUUID = currentUserUUID,
            userName = currentUserName,
            ownerUUID = roomInfo.ownerUUID,
            ownerName = roomInfo.ownerUserName,

            roomUUID = roomUUID,
            title = roomInfo.title,
            roomType = roomInfo.roomType,
            inviteCode = roomInfo.inviteCode,
            beginTime = roomInfo.beginTime,
            endTime = roomInfo.endTime,
            roomStatus = roomInfo.roomStatus,
            region = roomInfo.region,

            classMode = when (roomInfo.roomType) {
                RoomType.BigClass -> ClassModeType.Lecture
                else -> ClassModeType.Interaction
            },

            isSpeak = isSpeak,
            isRaiseHand = false,
            videoOpen = isSpeak && config.enableVideo,
            audioOpen = isSpeak && config.enableAudio,

            boardUUID = joinRoomInfo.whiteboardRoomUUID,
            boardToken = joinRoomInfo.whiteboardRoomToken,
            rtcUID = joinRoomInfo.rtcUID,
            rtcToken = joinRoomInfo.rtcToken,
            rtcShareScreen = joinRoomInfo.rtcShareScreen,
            rtmToken = joinRoomInfo.rtmToken,
        )

        userManager.reset(
            currentUser = RtcUser(
                userUUID = currentUserUUID,
                name = currentUserName,
                avatarURL = userRepository.getUserInfo()!!.avatar,
                rtcUID = initState.rtcUID,
                isSpeak = initState.isSpeak,
                isRaiseHand = initState.isRaiseHand,
                videoOpen = initState.videoOpen,
                audioOpen = initState.audioOpen,
            ),
            ownerUUID = roomInfo.ownerUUID
        )
        recordManager.reset(roomUUID, viewModelScope)

        _state.value = initState
    }

    fun onWhiteboardInit() {
        viewModelScope.launch {
            takeInitState().collect {
                logger.i("start joining board room")
                boardRoom.join(it.boardUUID, it.boardToken, it.region, it.isWritable)
            }
        }
    }

    private fun observeSyncedState() {
        viewModelScope.launch {
            syncedClassState.observeDeviceState().collect {
                it.forEach { entry ->
                    userManager.updateDeviceState(entry.key, entry.value.camera, entry.value.mic)
                }
            }
        }

        viewModelScope.launch {
            syncedClassState.observeClassroomState().collect {
                it.raiseHandUsers?.run {
                    userManager.updateRaiseHandStatus(this)
                }
                if (_state.value != null) {
                    _state.value = _state.value!!.copy(ban = it.ban)
                }
            }
        }

        viewModelScope.launch {
            syncedClassState.observeOnStage().collect {
                userManager.updateOnStage(it)
            }
        }
    }

    private fun observerRtmEvent() {
        viewModelScope.launch {
            rtmApi.observeRtmEvent().collect { handleRtmEvent(it) }
        }
    }

    private suspend fun handleRtmEvent(event: ClassRtmEvent) {
        logger.d("handleClassEvent $event")
        when (event) {
            is RaiseHandEvent -> {
                val state = state.value ?: return
                if (state.isOwner) {
                    syncedClassState.updateRaiseHand(userId = event.sender!!, event.raiseHand)
                    userManager.updateRaiseHandStatus(uuid = event.sender!!, isRaiseHand = event.raiseHand)
                }
            }
            is OnMemberJoined -> {
                userManager.addUser(event.userId)
            }
            is OnMemberLeft -> {
                userManager.removeUser(event.userId)
                // when user left, owner clear state;
                state.value?.let {
                    if (it.isOwner) {
                        syncedClassState.deleteDeviceState(event.userId)
                        syncedClassState.updateOnStage(event.userId, false)
                    }
                }
            }
            is RoomBanEvent -> {
                appendMessage(MessageFactory.createNotice(ban = event.status))
            }
            is ChatMessage -> {
                appendMessage(MessageFactory.createText(sender = event.sender, event.message))
            }
            else -> {
                logger.e("rtm event not handled: $event")
            }
        }
    }

    private fun observerUserState() {
        viewModelScope.launch {
            userManager.observeSelf().filterNotNull().collect {
                logger.d("on current user collected $it")
                val state = _state.value ?: return@collect
                _state.value = state.copy(
                    isSpeak = it.isSpeak,
                    isRaiseHand = it.isRaiseHand,
                    videoOpen = it.videoOpen,
                    audioOpen = it.audioOpen,
                )

                logger.d("change board writable ${_state.value!!.isWritable}")
                viewModelScope.launch {
                    boardRoom.setWritable(_state.value!!.isWritable)
                }
            }
        }

        viewModelScope.launch {
            userManager.observeUsers().collect {
                logger.d("on all users collected")
                val state = _state.value ?: return@collect
                val users = it.filter { user ->
                    when (state.roomType) {
                        RoomType.BigClass -> (state.isCreator(user.userUUID) || user.isSpeak)
                        else -> true
                    }
                }.toMutableList()

                if (!users.containOwner()) {
                    users.add(0, RtcUser(rtcUID = RtcUser.NOT_JOIN_RTC_UID, userUUID = state.ownerUUID))
                }

                _videoUsers.value = users
            }
        }
    }

    private fun List<RtcUser>.containOwner(): Boolean {
        return this.find { _state.value!!.ownerUUID == it.userUUID } != null
    }

    fun enableVideo(enableVideo: Boolean, uuid: String = currentUserUUID) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            userManager.findFirstUser(uuid)?.run {
                if (state.isCurrentUser(uuid)) {
                    updateDeviceState(uuid, enableVideo = enableVideo, enableAudio = audioOpen)
                } else if (state.isOwner && !enableVideo) {
                    updateDeviceState(uuid, enableVideo = enableVideo, enableAudio = audioOpen)
                } else {
                    notifyNoOptPermission()
                }
            }
        }
    }

    fun enableAudio(enableAudio: Boolean, uuid: String = currentUserUUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _state.value ?: return@launch
            userManager.findFirstUser(uuid)?.run {
                if (state.isCurrentUser(uuid)) {
                    updateDeviceState(uuid, enableVideo = this.videoOpen, enableAudio = enableAudio)
                } else if (state.isOwner && !enableAudio) {
                    updateDeviceState(uuid, enableVideo = this.videoOpen, enableAudio = enableAudio)
                } else {
                    notifyNoOptPermission()
                }
            }
        }
    }

    private fun updateDeviceState(userUUID: String, enableVideo: Boolean, enableAudio: Boolean) {
        syncedClassState.updateDeviceState(userUUID, camera = enableVideo, mic = enableAudio)
    }

    fun sendGlobalEvent(event: Event) {
        viewModelScope.launch {
            eventbus.produceEvent(event)
        }
    }

    private fun notifyRTMChannelJoined() {
        viewModelScope.launch {
            eventbus.produceEvent(RtmChannelJoined)
        }
    }

    private fun notifyNoOptPermission() {
        viewModelScope.launch {
            eventbus.produceEvent(NoOptPermission())
        }
    }

    fun setVideoAreaShown(shown: Boolean) {
        _videoAreaShown.value = shown
    }

    fun setMessageAreaShown(shown: Boolean) {
        _messageAreaShown.value = shown
    }

    private fun appendMessage(message: Message) {
        viewModelScope.launch {
            eventbus.produceEvent(MessagesAppended(listOf(message)))
        }
    }

    fun sendRaiseHand() {
        val state = state.value ?: return
        viewModelScope.launch {
            userManager.currentUser?.run {
                if (this.isSpeak) {
                    return@run
                }
                val event = RaiseHandEvent(
                    roomUUID = roomUUID,
                    raiseHand = !isRaiseHand,
                )
                rtmApi.sendPeerCommand(event, state.ownerUUID)
                userManager.updateRaiseHandStatus(userUUID, event.raiseHand)
            }
        }
    }

    fun startClass() {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (roomRepository.startRoomClass(roomUUID).isSuccess) {
                rtmApi.sendChannelCommand(RoomStateEvent(roomUUID = roomUUID, state = RoomStatus.Started))
                _state.value = state.copy(roomStatus = RoomStatus.Started)
            }
        }
    }

    suspend fun pauseClass(): Boolean {
        val result = roomRepository.pauseRoomClass(roomUUID)
        if (result.isSuccess) {
            rtmApi.sendChannelCommand(RoomStateEvent(roomUUID = roomUUID, state = RoomStatus.Paused))
        }
        return result.isSuccess
    }

    suspend fun stopClass(): Boolean {
        val result = roomRepository.stopRoomClass(roomUUID)
        if (result.isSuccess) {
            rtmApi.sendChannelCommand(RoomStateEvent(roomUUID = roomUUID, state = RoomStatus.Stopped))
            recordManager.stopRecord()
        }
        return result.isSuccess
    }

    fun startRecord() {
        recordManager.startRecord()
    }

    fun stopRecord() {
        recordManager.stopRecord()
    }

    fun onCopyText(text: String) {
        clipboard.putText(text)
    }

    fun requestCloudStorageFiles() {
        viewModelScope.launch {
            val result = cloudStorageRepository.listFiles(1)
            if (result.isSuccess) {
                _cloudStorageFiles.value = result.getOrThrow().files.filter {
                    it.convertStep == FileConvertStep.Done || it.convertStep == FileConvertStep.None
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
                    boardRoom.insertImage(file.fileURL, w = imageSize.width, h = imageSize.height)
                }
                CoursewareType.Audio, CoursewareType.Video -> {
                    boardRoom.insertVideo(file.fileURL, file.fileName)
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
                    boardRoom.insertPpt("/${file.taskUUID}/${UUID.randomUUID()}", ppt, file.fileName)
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
                    boardRoom.insertProjectorPpt(file.taskUUID, response.prefix, file.fileName)
                }

                override fun onFailure(e: ConvertException?) {

                }
            })
            .build()
        projectorQuery.startQuery()
    }

    fun closeSpeak(userUUID: String) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner) {
                syncedClassState.updateOnStage(userUUID, false)
            }
            if (state.isCurrentUser(userUUID)) {
                syncedClassState.updateOnStage(userUUID, false)
            }
        }
    }

    fun acceptRaiseHand(userUUID: String) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner) {
                syncedClassState.updateOnStage(userUUID, true)
                userManager.updateSpeakAndRaise(userUUID, isSpeak = true, isRaiseHand = false)
            }
        }
    }

    fun muteChat(muted: Boolean) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner) {
                syncedClassState.updateBan(muted)
                rtmApi.sendChannelCommand(
                    RoomBanEvent(
                        roomUUID = roomUUID,
                        status = muted,
                    )
                )
            }
        }
    }

    fun updateClassMode(classMode: ClassModeType) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            _state.value = state.copy(classMode = classMode)
            syncedClassState.updateClassModeType(classMode)
        }
    }
}

data class ClassRoomState(
    // users
    // 当前用户
    val userUUID: String,
    val userName: String = "",
    // 房间所有者
    val ownerUUID: String,
    // 房间所有者的名称
    val ownerName: String? = ownerUUID.substring(ownerUUID.length - 6),

    val isSpeak: Boolean,
    val isRaiseHand: Boolean,
    val videoOpen: Boolean,
    val audioOpen: Boolean,

    val boardUUID: String,
    val boardToken: String,
    val rtcUID: Int,
    val rtcToken: String,
    val rtcShareScreen: RtcShareScreen,
    val rtmToken: String,

    // class info
    // 房间标题
    val title: String,
    // 房间的 uuid
    val roomUUID: String,
    // 房间开始时间
    val beginTime: Long = 0L,
    // 结束时间
    val endTime: Long = 0L,
    // 房间邀请码
    val inviteCode: String,
    // class state
    // 禁用
    val ban: Boolean = false,
    // 交互模式
    val classMode: ClassModeType,
    // 房间类型
    val roomType: RoomType,
    // 房间状态
    val roomStatus: RoomStatus,
    // 房间区域
    val region: String,
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

    val needShowExitDialog: Boolean
        get() = isOwner && RoomStatus.Idle != roomStatus

    fun isCreator(userId: String): Boolean {
        return userId == this.ownerUUID
    }

    fun isCurrentUser(userId: String): Boolean {
        return userId == this.userUUID
    }
}

data class ImageSize(val width: Int, val height: Int)