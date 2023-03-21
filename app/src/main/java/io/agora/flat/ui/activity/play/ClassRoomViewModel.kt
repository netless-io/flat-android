package io.agora.flat.ui.activity.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.Constants
import io.agora.flat.common.FlatException
import io.agora.flat.common.android.ClipboardController
import io.agora.flat.common.board.AgoraBoardRoom
import io.agora.flat.common.board.DeviceState
import io.agora.flat.common.rtc.RtcJoinOptions
import io.agora.flat.common.rtm.*
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.di.interfaces.SyncedClassState
import io.agora.flat.event.*
import io.agora.flat.ui.manager.RecordManager
import io.agora.flat.ui.manager.RoomErrorManager
import io.agora.flat.ui.manager.RoomOverlayManager
import io.agora.flat.ui.manager.UserManager
import io.agora.flat.ui.viewmodel.ChatMessageManager
import io.agora.flat.ui.viewmodel.RtcVideoController
import io.agora.flat.util.toInviteCodeDisplay
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val userManager: UserManager,
    private val recordManager: RecordManager,
    private val messageManager: ChatMessageManager,
    private val roomErrorManager: RoomErrorManager,
    private val rtmApi: RtmApi,
    private val rtcApi: RtcApi,
    private val rtcVideoController: RtcVideoController,
    private val boardRoom: AgoraBoardRoom,
    private val syncedClassState: SyncedClassState,
    private val eventbus: EventBus,
    private val clipboard: ClipboardController,
    private val appEnv: AppEnv,
    private val appKVCenter: AppKVCenter,
    private val logger: Logger,
) : ViewModel() {
    private var _state = MutableStateFlow<ClassRoomState?>(null)
    val state = _state.asStateFlow()

    private fun takeInitState() = state.filterNotNull().take(1)

    private var _videoUsers = MutableStateFlow<List<RoomUser>>(emptyList())
    val videoUsers = _videoUsers.asStateFlow()
    val videoUsersMap = videoUsers.map { it.associateBy { user -> user.userUUID } }

    // cloud record state
    val recordState get() = recordManager.observeRecordState()

    val noOptPermission get() = eventbus.events.filterIsInstance<NoOptPermission>()

    val teacher = userManager.observeUsers().map { it.firstOrNull { user -> user.isOwner } }
    val students = userManager.observeUsers().map {
        it.filter { user -> !user.isOwner && (user.isJoined || user.isOnStage) }
            .map { user -> user.copy(isRaiseHand = !user.isOnStage && user.isRaiseHand) }
    }

    private var _videoAreaShown = MutableStateFlow(true)
    val videoAreaShown = _videoAreaShown.asStateFlow()

    val messageAreaShown = RoomOverlayManager.observeShowId().map { it == RoomOverlayManager.AREA_ID_MESSAGE }
    val messageCount = messageManager.messages.map { it.size }.distinctUntilChanged()

    private val roomUUID: String = checkNotNull(savedStateHandle[Constants.IntentKey.ROOM_UUID])
    private val periodicUUID: String? = savedStateHandle[Constants.IntentKey.PERIODIC_UUID]
    private val playInfo: RoomPlayInfo? = savedStateHandle[Constants.IntentKey.ROOM_PLAY_INFO]
    private val quickStart: Boolean = savedStateHandle[Constants.IntentKey.ROOM_QUICK_START] ?: false
    private val currentUserUUID = userRepository.getUserUUID()
    private val currentUserName = userRepository.getUsername()
    private val preferDeviceState = appKVCenter.getDeviceStatePreference()
    private var syncedStoreReady = false
    private var onStageLimit = RtcApi.MAX_CAPACITY

    init {
        viewModelScope.launch {
            try {
                loadAndInitRoomState()
            } catch (e: Exception) {
                roomErrorManager.notifyError("fetch class room state error", e)
            }
        }

        /**
         * It can be determined that the status callback is made after the successful joining of the room
         */
        observeSyncedState()
    }

    fun loginThirdParty() {
        viewModelScope.launch {
            takeInitState().collect {
                joinRtm()
                joinRtc()
                joinBoard()
                syncInitState()
                observerUserState()
                if (quickStart) startClass()
            }
        }
    }

    private suspend fun loadAndInitRoomState() = coroutineScope {
        val deferredOne = async { roomRepository.getOrdinaryRoomInfo(roomUUID).getOrThrow().roomInfo }
        val deferredTwo = async {
            return@async playInfo ?: roomRepository.joinRoom(periodicUUID ?: roomUUID).getOrThrow()
        }
        initRoomState(deferredOne.await(), deferredTwo.await())
    }

    private fun initRoomState(roomInfo: RoomInfo, joinRoomInfo: RoomPlayInfo) {
        val isOwner = roomInfo.ownerUUID == currentUserUUID

        val initState = ClassRoomState(
            userUUID = currentUserUUID,
            userName = currentUserName,
            ownerUUID = roomInfo.ownerUUID,
            ownerName = roomInfo.ownerName,
            isOwner = isOwner,

            roomUUID = roomUUID,
            title = roomInfo.title,
            roomType = roomInfo.roomType,
            inviteCode = roomInfo.inviteCode,
            beginTime = roomInfo.beginTime,
            endTime = roomInfo.endTime,
            roomStatus = roomInfo.roomStatus,
            region = roomInfo.region,

            isOnStage = isOwner,
            isRaiseHand = false,
            allowDraw = isOwner,
            videoOpen = isOwner && preferDeviceState.camera,
            audioOpen = isOwner && preferDeviceState.mic,

            boardUUID = joinRoomInfo.whiteboardRoomUUID,
            boardToken = joinRoomInfo.whiteboardRoomToken,
            rtcUID = joinRoomInfo.rtcUID,
            rtcToken = joinRoomInfo.rtcToken,
            rtcShareScreen = joinRoomInfo.rtcShareScreen,
            rtmToken = joinRoomInfo.rtmToken,
        )

        userManager.reset(
            currentUser = RoomUser(
                userUUID = currentUserUUID,
                name = currentUserName,
                avatarURL = userRepository.getUserInfo()!!.avatar,
                rtcUID = initState.rtcUID,
                isOnStage = isOwner,
                isRaiseHand = initState.isRaiseHand,
                videoOpen = initState.videoOpen,
                audioOpen = initState.audioOpen,
                isOwner = isOwner,
                allowDraw = isOwner,
            ),
            ownerUUID = roomInfo.ownerUUID
        )
        recordManager.reset(roomUUID, viewModelScope)

        onStageLimit = when (roomInfo.roomType) {
            RoomType.OneToOne -> 2
            RoomType.SmallClass -> RtcApi.MAX_CAPACITY
            RoomType.BigClass -> 2
        }

        _state.value = initState
    }

    private suspend fun joinBoard() {
        logger.i("[BOARD] start joining board room")
        state.value?.let {
            boardRoom.join(it.boardUUID, it.boardToken, it.region, it.isOwner)
        }
    }

    private suspend fun joinRtm() {
        logger.i("[RTM] start join channel")
        state.value?.run {
            try {
                rtmApi.login(rtmToken = rtmToken, channelId = roomUUID, userUUID = userUUID)
                observerRtmEvent()
                val userIds = rtmApi.getMembers().map { it.userId }
                userManager.initUsers(userIds)
            } catch (e: FlatException) {
                roomErrorManager.notifyError("rtm join exception", e)
            }
        }
    }

    private fun joinRtc() {
        logger.i("[RTC] start join rtc")
        state.value?.run {
            rtcVideoController.setupUid(uid = rtcUID, ssUid = rtcShareScreen.uid)
            rtcApi.joinChannel(RtcJoinOptions(rtcToken, roomUUID, rtcUID, audioOpen = audioOpen, videoOpen = videoOpen))
        }
    }

    private fun observeSyncedState() {
        viewModelScope.launch {
            syncedClassState.observeDeviceState().collect {
                userManager.updateDeviceState(it)
            }
        }

        viewModelScope.launch {
            syncedClassState.observeClassroomState().collect {
                userManager.updateRaiseHandStatus(it.raiseHandUsers)
                updateBanState(it.ban)
            }
        }

        viewModelScope.launch {
            syncedClassState.observeOnStage().collect {
                userManager.updateOnStage(it)
                syncedStoreReady = true
            }
        }

        viewModelScope.launch {
            syncedClassState.observeWhiteboard().collect {
                userManager.updateAllowDraw(it)
            }
        }
    }

    private fun updateBanState(ban: Boolean) {
        _state.value?.run {
            _state.value = this.copy(ban = ban)
        }
    }

    private fun observerRtmEvent() {
        viewModelScope.launch {
            rtmApi.observeRtmEvent().collect {
                handleRtmEvent(it)
            }
        }
    }

    private suspend fun handleRtmEvent(event: ClassRtmEvent) {
        logger.d("[RTM] handle event $event")
        when (event) {
            is RaiseHandEvent -> {
                val state = state.value ?: return
                if (state.isOwner && syncedStoreReady) {
                    syncedClassState.updateRaiseHand(userId = event.sender!!, event.raiseHand)
                }
            }
            is RoomStateEvent -> {
                state.value?.run {
                    _state.value = this.copy(roomStatus = event.status)
                }
            }
            is RequestDeviceEvent -> {
                eventbus.produceEvent(RequestDeviceReceived(mic = event.mic, camera = event.camera))
            }
            is RequestDeviceResponseEvent -> {
                val uuid = event.sender ?: return
                val name = userManager.findFirstUser(uuid)?.name ?: uuid
                eventbus.produceEvent(
                    RequestDeviceResponseReceived(
                        username = name,
                        mic = event.mic,
                        camera = event.camera
                    )
                )
            }
            is NotifyDeviceOffEvent -> {
                eventbus.produceEvent(NotifyDeviceOffReceived(mic = event.mic, camera = event.camera))
            }
            is OnMemberJoined -> {
                userManager.addUser(event.userId)
            }
            is OnMemberLeft -> {
                userManager.removeUser(event.userId)
            }
            is RoomBanEvent -> {
                appendMessage(MessageFactory.createNotice(ban = event.status))
            }
            is ChatMessage -> {
                appendMessage(MessageFactory.createText(sender = event.sender, event.message))
            }
            else -> {
                logger.w("[RTM] event not handled: $event")
            }
        }
    }

    private fun syncInitState() {
        viewModelScope.launch {
            syncedClassState.observeSyncedReady().collect {
                val state = _state.value ?: return@collect
                if (state.isOnStage) {
                    syncedClassState.updateDeviceState(
                        userId = state.userUUID,
                        camera = state.videoOpen,
                        mic = state.audioOpen
                    )
                }
            }
        }
    }

    private fun observerUserState() {
        viewModelScope.launch {
            userManager.observeSelf().filterNotNull().collect {
                logger.d("[USERS] current user state changed $it")
                val state = _state.value ?: return@collect
                try {
                    boardRoom.setWritable(it.isOnStage || it.allowDraw)
                    boardRoom.setAllowDraw(it.allowDraw)

                    if (it.isOnStage && !state.isOnStage) {
                        syncedClassState.updateDeviceState(
                            userId = it.userUUID,
                            camera = preferDeviceState.camera,
                            mic = preferDeviceState.mic
                        )
                    }

                    _state.value = state.copy(
                        isOnStage = it.isOnStage,
                        isRaiseHand = it.isRaiseHand,
                        allowDraw = it.allowDraw,
                        videoOpen = it.videoOpen,
                        audioOpen = it.audioOpen,
                    )
                } catch (e: Exception) {
                    logger.e(e, "[USERS] self change error")
                }
            }
        }

        viewModelScope.launch {
            userManager.observeUsers().collect {
                logger.d("[USERS] users changed $it")
                val users = it.filter { user -> user.isOwner || user.isOnStage }.toMutableList()
                val devicesMap = getUpdateDevices(videoUsers.value, users)
                devicesMap.forEach { (rtcUID, state) ->
                    updateRtcStream(rtcUID, audioOpen = state.mic, videoOpen = state.camera)
                }
                _videoUsers.value = users
            }
        }

        viewModelScope.launch {
            if (userManager.isOwner() && state.value!!.roomType == RoomType.OneToOne) {
                combine(
                    syncedClassState.observeSyncedReady(),
                    userManager.observeUsers()
                ) { ready, users ->
                    if (ready) users else null
                }.filterNotNull()
                    .onCompletion {
                        logger.d("[USERS] one to one observeUsers done")
                    }
                    .collect {
                        val users = it.filter { user -> user.isJoined && !user.isOwner }
                        if (users.isEmpty()) return@collect
                        val count = users.count { user -> user.isOnStage }
                        if (count == 0) {
                            it.filter { user -> !user.isOnStage }.randomOrNull()?.run {
                                syncedClassState.updateOnStage(userUUID, true)
                            }
                        }
                        cancel()
                    }
            }
        }
    }

    private fun getUpdateDevices(
        newUsers: List<RoomUser>,
        oldUsers: List<RoomUser>
    ): MutableMap<Int, DeviceState> {
        val oldUsersMap = newUsers.associateBy { user -> user.userUUID }
        val newUsersMap = oldUsers.associateBy { user -> user.userUUID }

        val devicesMap = mutableMapOf<Int, DeviceState>()
        newUsersMap.forEach { (_, user) ->
            if (user.isJoined) {
                devicesMap[user.rtcUID] = DeviceState(
                    camera = user.isOnStage && user.videoOpen,
                    mic = user.isOnStage && user.audioOpen
                )
            }
        }
        oldUsersMap.forEach { (uuid, user) ->
            if (!newUsersMap.contains(uuid) && user.isJoined) {
                devicesMap[user.rtcUID] = DeviceState(camera = false, mic = false)
            }
        }
        return devicesMap
    }

    private fun updateRtcStream(rtcUID: Int, audioOpen: Boolean, videoOpen: Boolean) {
        if (rtcUID == userManager.currentUser?.rtcUID) {
            rtcApi.updateLocalStream(audio = audioOpen, video = videoOpen)
        } else {
            rtcApi.updateRemoteStream(rtcUid = rtcUID, audio = audioOpen, video = videoOpen)
        }
    }

    fun canControlDevice(uuid: String): Boolean {
        val state = _state.value ?: return false
        return state.isOwner || (state.isOnStage && uuid == state.userUUID)
    }

    fun enableVideo(enableVideo: Boolean, uuid: String = currentUserUUID) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            userManager.findFirstUser(uuid)?.run {
                if (userManager.isUserSelf(uuid)) {
                    updateDeviceState(uuid, enableVideo = enableVideo, enableAudio = audioOpen)
                } else {
                    if (state.isOwner) {
                        if (enableVideo) {
                            rtmApi.sendPeerCommand(RequestDeviceEvent(roomUUID = roomUUID, camera = true), uuid)
                            eventbus.produceEvent(RequestDeviceSent(camera = true))
                        } else {
                            updateDeviceState(uuid, enableVideo = false, enableAudio = audioOpen)
                        }
                    }
                }
            }
        }
    }

    fun enableAudio(enableAudio: Boolean, uuid: String = currentUserUUID) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            userManager.findFirstUser(uuid)?.run {
                if (userManager.isUserSelf(uuid)) {
                    updateDeviceState(uuid, enableVideo = videoOpen, enableAudio = enableAudio)
                } else if (state.isOwner) {
                    if (enableAudio) {
                        rtmApi.sendPeerCommand(RequestDeviceEvent(roomUUID = roomUUID, mic = true), uuid)
                        eventbus.produceEvent(RequestDeviceSent(mic = true))
                    } else {
                        updateDeviceState(uuid, enableVideo = videoOpen, enableAudio = false)
                    }
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

    fun setVideoAreaShown(shown: Boolean) {
        _videoAreaShown.value = shown
    }

    private fun appendMessage(message: Message) {
        viewModelScope.launch {
            eventbus.produceEvent(MessagesAppended(listOf(message)))
        }
    }

    fun raiseHand() {
        viewModelScope.launch {
            userManager.currentUser?.run {
                if (isOnStage || !userManager.isOwnerOnStage()) return@run
                val raiseHand = !isRaiseHand
                rtmApi.sendPeerCommand(
                    RaiseHandEvent(roomUUID = roomUUID, raiseHand = raiseHand),
                    userManager.ownerUUID
                )
                userManager.updateRaiseHandStatus(userUUID, raiseHand)
            }
        }
    }

    private fun startClass() {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (roomRepository.startRoomClass(roomUUID).isSuccess) {
                rtmApi.sendChannelCommand(RoomStateEvent(roomUUID = roomUUID, status = RoomStatus.Started))
                _state.value = state.copy(roomStatus = RoomStatus.Started)
            }
        }
    }

    suspend fun pauseClass(): Boolean {
        val result = roomRepository.pauseRoomClass(roomUUID)
        if (result.isSuccess) {
            rtmApi.sendChannelCommand(RoomStateEvent(roomUUID = roomUUID, status = RoomStatus.Paused))
        }
        return result.isSuccess
    }

    suspend fun stopClass(): Boolean {
        val result = roomRepository.stopRoomClass(roomUUID)
        if (result.isSuccess) {
            rtmApi.sendChannelCommand(RoomStateEvent(roomUUID = roomUUID, status = RoomStatus.Stopped))
            recordManager.stopRecord()
        }
        return result.isSuccess
    }

    suspend fun startRecord() {
        recordManager.startRecord()
    }

    suspend fun stopRecord() {
        recordManager.stopRecord()
    }

    fun setClipboard(text: String) {
        clipboard.putText(text)
    }

    fun updateOnStage(uuid: String, onstage: Boolean) {
        viewModelScope.launch {
            if (onstage) {
                acceptRaiseHand(uuid)
            } else {
                closeSpeak(uuid)
            }
        }
    }

    fun updateAllowDraw(uuid: String, allowDraw: Boolean) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner) {
                syncedClassState.updateWhiteboard(uuid, allowDraw)
            }
            if (userManager.isUserSelf(uuid) && !allowDraw) {
                syncedClassState.updateWhiteboard(uuid, false)
            }
        }
    }

    private fun closeSpeak(uuid: String) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner || userManager.isUserSelf(uuid)) {
                syncedClassState.deleteDeviceState(uuid)
                syncedClassState.updateOnStage(uuid, false)
            }
        }
    }

    fun acceptRaiseHand(uuid: String) {
        viewModelScope.launch {
            if (!userManager.isOwner()) return@launch
            if (userManager.getOnStageCount() < onStageLimit) {
                syncedClassState.updateOnStage(uuid, true)
                syncedClassState.updateRaiseHand(uuid, false)
            } else {
                // notify
            }
        }
    }

    fun cancelRaiseHand(uuid: String) {
        viewModelScope.launch {
            if (!userManager.isOwner()) return@launch
            syncedClassState.updateRaiseHand(uuid, false)
        }
    }

    fun getInviteInfo(): InviteInfo? {
        val state = state.value ?: return null
        return InviteInfo(
            username = currentUserName,
            roomTitle = state.title,
            link = appEnv.baseInviteUrl + "/join/" + state.roomUUID,
            roomUuid = state.inviteCode.toInviteCodeDisplay(),
            beginTime = state.beginTime,
            endTime = state.endTime,
        )
    }

    fun isOwner(): Boolean {
        return userManager.isOwner()
    }

    fun isSelf(userId: String): Boolean {
        return userManager.isUserSelf(userId)
    }

    fun isOnStageAllowable(): Boolean {
        return userManager.getOnStageCount() < onStageLimit
    }

    fun canShowCallOut(userId: String): Boolean {
        return userManager.isOwner() || userManager.isUserSelf(userId)
    }

    fun canDragUser(): Boolean {
        return userManager.isOwner()
    }

    fun stageOffAll() {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner) {
                syncedClassState.stageOffAll()
            }
        }
    }

    fun muteAllMic() {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            if (state.isOwner) {
                val userIds = _videoUsers.value.filter { !it.isOwner }.map { user -> user.userUUID }
                syncedClassState.muteDevicesMic(userIds)
            }
        }
    }

    fun refuseCamera() {
        viewModelScope.launch {
            rtmApi.sendPeerCommand(
                RequestDeviceResponseEvent(roomUUID = roomUUID, camera = false),
                userManager.ownerUUID
            )
        }
    }

    fun agreeCamera() {
        viewModelScope.launch {
            enableVideo(true)
            rtmApi.sendPeerCommand(
                RequestDeviceResponseEvent(roomUUID = roomUUID, camera = true),
                userManager.ownerUUID
            )
        }
    }

    fun refuseMic() {
        viewModelScope.launch {
            rtmApi.sendPeerCommand(
                RequestDeviceResponseEvent(roomUUID = roomUUID, mic = false),
                userManager.ownerUUID
            )
        }
    }

    fun agreeMic() {
        viewModelScope.launch {
            enableAudio(true)
            rtmApi.sendPeerCommand(
                RequestDeviceResponseEvent(roomUUID = roomUUID, mic = true),
                userManager.ownerUUID
            )
        }
    }
}

data class ClassRoomState(
    // users
    val userUUID: String,
    val userName: String = "",
    val ownerUUID: String,
    val ownerName: String,
    val isOwner: Boolean,

    // current user state
    val isOnStage: Boolean,
    val isRaiseHand: Boolean,
    val allowDraw: Boolean,
    val videoOpen: Boolean,
    val audioOpen: Boolean,

    // current user interaction params
    val boardUUID: String,
    val boardToken: String,
    val rtcUID: Int,
    val rtcToken: String,
    val rtcShareScreen: RtcShareScreen,
    val rtmToken: String,

    // class room info
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
    // 房间类型
    val roomType: RoomType,
    // 房间区域
    val region: String,
    // 禁言
    val ban: Boolean = false,
    // 房间状态
    val roomStatus: RoomStatus,
)

data class ImageSize(val width: Int, val height: Int)