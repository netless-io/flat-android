package io.agora.flat.ui.viewmodel

import android.graphics.BitmapFactory
import android.util.Log
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
import io.agora.flat.common.android.StringFetcher
import io.agora.flat.common.board.BoardRoom
import io.agora.flat.common.rtm.*
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.impl.Event
import io.agora.flat.di.impl.EventBus
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.event.MessagesAppended
import io.agora.flat.event.NoOptPermission
import io.agora.flat.event.RtmChannelJoined
import io.agora.flat.ui.manager.RecordManager
import io.agora.flat.ui.manager.RoomErrorManager
import io.agora.flat.util.ClassroomTrace
import io.agora.flat.util.coursewareType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val stringFetcher: StringFetcher,
) : ViewModel() {
    private var _roomPlayInfo = MutableStateFlow<RoomPlayInfo?>(null)
    val roomPlayInfo = _roomPlayInfo.asStateFlow()

    private var _state = MutableStateFlow<ClassRoomState?>(null)
    val state = _state.asStateFlow()

    private var _videoUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val videoUsers = _videoUsers.asStateFlow()

    // cloud record state
    val recordState get() = recordManager.observeRecordState()

    val rtmSuccess get() = eventbus.events.filterIsInstance<RtmChannelJoined>().take(1)

    val noOptPermission get() = eventbus.events.filterIsInstance<NoOptPermission>()

    private var _messageUsers = MutableStateFlow<List<RtcUser>>(emptyList())
    val messageUsers = _messageUsers.asStateFlow()

    private var _cloudStorageFiles = MutableStateFlow<List<CloudStorageFile>>(mutableListOf())
    val cloudStorageFiles = _cloudStorageFiles.asStateFlow()

    private var _videoAreaShown = MutableStateFlow(true)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    private var _messageAreaShown = MutableStateFlow(false)
    val messageAreaShown = _messageAreaShown.asStateFlow()

    val messageCount = messageManager.messages.map { it.size }

    private val roomUUID = savedStateHandle.get<String>(Constants.IntentKey.ROOM_UUID)!!
    private val periodicUUID = savedStateHandle.get<String>(Constants.IntentKey.PERIODIC_UUID)
    private val playInfo = savedStateHandle.get<RoomPlayInfo?>(Constants.IntentKey.ROOM_PLAY_INFO)
    private val quickStart = savedStateHandle.get<Boolean>(Constants.IntentKey.ROOM_QUICK_START) ?: false
    private val currentUserUUID = userRepository.getUserUUID()
    private val currentUserName = userRepository.getUsername()

    init {
        viewModelScope.launch {
            val result = roomRepository.getOrdinaryRoomInfo(roomUUID)
            if (result.isSuccess) {
                initRoomInfo(result.getOrThrow().roomInfo)
            } else {
                roomErrorManager.notifyError("fetch room info error", result.asFailure().exception)
            }
        }

        viewModelScope.launch {
            takeInitState().collect {
                var info: RoomPlayInfo? = playInfo
                if (info == null) {
                    val result = roomRepository.joinRoom(periodicUUID ?: roomUUID)
                    if (result.isSuccess) {
                        info = result.getOrThrow()
                    } else {
                        roomErrorManager.notifyError("fetch join room info", result.asFailure().exception)
                    }
                }
                _roomPlayInfo.value = info
            }
        }

        viewModelScope.launch {
            rtmSuccess.collect {
                if (quickStart) startClass()
            }
        }

        viewModelScope.launch {
            getJoinRoomInfo().collect {
                joinRtmChannel(channelId = roomUUID, rtmToken = it.rtmToken)
            }
        }

        /**
         * It can be determined that the status callback is made after the successful joining of the room
         */
        observeSyncedState()
    }

    private fun takeInitState() = state.filterNotNull().take(1)
    private fun getJoinRoomInfo() = roomPlayInfo.filterNotNull().take(1)

    fun onWhiteboardInit() {
        viewModelScope.launch {
            getJoinRoomInfo().collect {
                ClassroomTrace.trace("start joining board room")
                boardRoom.join(it.whiteboardRoomUUID, it.whiteboardRoomToken, it.region, it.defaultWritable())
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
            syncedClassState.observeRaiseHand().collect {
                userManager.updateRaiseHandStatus(it)
            }
        }

        viewModelScope.launch {
            syncedClassState.observeOnStage().collect {
                userManager.updateOnStage(it)
            }
        }
    }

    private fun joinRtmChannel(rtmToken: String, channelId: String) {
        viewModelScope.launch {
            try {
                rtmApi.login(rtmToken, channelId, userRepository.getUserUUID())
                // TODO store events
                val eventFlow = rtmApi.observeClassEvent()
                initUsersStatus()
                launch {
                    eventFlow.collect {
                        handleClassEvent(it)
                    }
                }
                ClassroomTrace.trace("rtm joined success")
                notifyRTMChannelJoined()
            } catch (e: FlatException) {
                roomErrorManager.notifyError("rtm join exception", e)
            }
        }
    }

    private fun initRoomInfo(roomInfo: RoomInfo) {
        userManager.reset(userUUID = currentUserUUID, ownerUUID = roomInfo.ownerUUID)
        recordManager.reset(roomUUID, viewModelScope)

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
            isSpeak = roomInfo.ownerUUID == currentUserUUID,
            isRaiseHand = false,
            videoOpen = false,
            audioOpen = false,
        )
    }

    private fun observerUserState() {
        viewModelScope.launch {
            userManager.currentUser.filterNotNull().collect {
                ClassroomTrace.trace("currentUser collect")
                val state = _state.value ?: return@collect
                _state.value = state.copy(
                    isSpeak = it.isSpeak,
                    isRaiseHand = it.isRaiseHand,
                    videoOpen = it.videoOpen,
                    audioOpen = it.audioOpen
                )
                boardRoom.setWritable(state.isOwner || it.isSpeak)
            }
        }

        viewModelScope.launch {
            userManager.users.collect {
                ClassroomTrace.trace("users collect")
                val state = _state.value ?: return@collect
                val users = it.filter { user ->
                    when (state.roomType) {
                        RoomType.BigClass -> (state.isCreator(user.userUUID) || user.isSpeak)
                        else -> true
                    }
                }.toMutableList()

                if (it.isNotEmpty() && !users.containOwner()) {
                    users.add(0, RtcUser(rtcUID = RtcUser.NOT_JOIN_RTC_UID, userUUID = state.ownerUUID))
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

    private suspend fun initUsersStatus() {
        ClassroomTrace.trace("init users status")
        val state = _state.value ?: return

        val config = roomConfigRepository.getRoomConfig(roomUUID) ?: RoomConfig(roomUUID)
        val audioOpen = roomPlayInfo.value!!.defaultWritable() && config.enableAudio
        val videoOpen = roomPlayInfo.value!!.defaultWritable() && config.enableVideo

        userManager.initUsers(rtmApi.getMembers().map { it.userId })
        // init current user state
        userManager.updateUserState(
            uuid = currentUserUUID,
            audioOpen = audioOpen,
            videoOpen = videoOpen,
            name = currentUserName,
            isSpeak = state.isOwner,
        )
        observerUserState()
    }

    private fun appendMessage(message: Message) {
        viewModelScope.launch {
            eventbus.produceEvent(MessagesAppended(listOf(message)))
        }
    }

    private suspend fun handleClassEvent(event: ClassRtmEvent) {
        Log.e("ClassRoomViewModel", "handleClassEvent $event")
        when (event) {
            is OnStageEventWithSender -> {
                val state = state.value ?: return
                val sender = event.sender!!
                // from owner to audience
                if (state.isCreator(sender)) {
                    userManager.updateSpeakAndRaise(
                        currentUserUUID,
                        isSpeak = event.onStage,
                        isRaiseHand = false,
                    )
                } else {
                    // from audience stage off
                    if (state.isOwner && !event.onStage) {
                        syncedClassState.updateOnStage(sender, false)
                        userManager.updateSpeakAndRaise(sender, isSpeak = false, isRaiseHand = false)
                    }
                }
            }
            is RaiseHandEventWithSender -> {
                userManager.updateRaiseHandStatus(uuid = event.sender!!, isRaiseHand = event.raiseHand)
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
            is ChatMessage -> {
                appendMessage(MessageFactory.createText(sender = event.sender, event.message))
            }
            else -> {
                Log.e("RTM", "$event not handled!!!")
            }
        }
    }

    fun sendRaiseHand() {
        val state = state.value ?: return
        viewModelScope.launch {
            userManager.currentUser.value?.run {
                if (this.isSpeak) {
                    return@run
                }
                val event = RaiseHandEventWithSender(
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
            val result = cloudStorageRepository.getFileList(1)
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
                sendOnStageCommand(userUUID, false)
                syncedClassState.updateOnStage(userUUID, false)
            }
            if (state.isCurrentUser(userUUID)) {
                sendOnStageCommand(state.ownerUUID, onStage = false)
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

    private suspend fun sendOnStageCommand(userUUID: String, onStage: Boolean) {
        val event = OnStageEventWithSender(
            roomUUID = roomUUID,
            onStage = onStage
        )
        rtmApi.sendPeerCommand(event, userUUID)
    }

    fun updateClassMode(classMode: ClassModeType) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            _state.value = state.copy(classMode = classMode)
            syncedClassState.updateClassModeType(classMode)
        }
    }

    private fun RoomPlayInfo.defaultWritable(): Boolean {
        return when (roomType) {
            RoomType.OneToOne -> true
            RoomType.SmallClass -> true
            RoomType.BigClass -> ownerUUID == currentUserUUID
        }
    }
}

data class ClassRoomState(
    // 房间的 uuid
    val roomUUID: String,
    // 房间邀请码
    val inviteCode: String,
    // 房间类型
    val roomType: RoomType,
    // 房间状态
    val roomStatus: RoomStatus,
    // 房间区域
    val region: String,
    // 房间所有者
    val ownerUUID: String,
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
    val classMode: ClassModeType,
    // 当前用户
    val userUUID: String,
    val userName: String = "",
    val isSpeak: Boolean,
    val isRaiseHand: Boolean,
    val videoOpen: Boolean,
    val audioOpen: Boolean,
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

    val needOwnerExitDialog: Boolean
        get() = isOwner && RoomStatus.Idle != roomStatus

    fun isCreator(userId: String): Boolean {
        return userId == this.ownerUUID
    }

    fun isCurrentUser(userId: String): Boolean {
        return userId == this.userUUID
    }
}

data class ImageSize(val width: Int, val height: Int)