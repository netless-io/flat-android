package io.agora.flat.ui.manager

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.common.board.BoardRoom
import io.agora.flat.common.board.DeviceState
import io.agora.flat.data.model.RtcUser
import io.agora.flat.di.interfaces.RtcApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ActivityRetainedScoped
class UserManager @Inject constructor(
    private val userQuery: UserQuery,
    private val rtcApi: RtcApi,
    private val boardRoom: BoardRoom,
) {
    private var _users = MutableStateFlow<List<RtcUser>>(emptyList())
    val users: List<RtcUser>
        get() {
            return _users.value
        }

    private var _currentUser = MutableStateFlow<RtcUser?>(null)
    val currentUser: RtcUser?
        get() {
            return _currentUser.value
        }

    private var creator: RtcUser? = null
    private var speakingJoiners: MutableList<RtcUser> = mutableListOf()
    private var handRaisingJoiners: MutableList<RtcUser> = mutableListOf()
    private var otherJoiners: MutableList<RtcUser> = mutableListOf()

    // current all users
    private var usersCache = mutableMapOf<String, RtcUser>()

    private lateinit var currentUserUUID: String
    private lateinit var ownerUUID: String

    fun observeUsers(): Flow<List<RtcUser>> {
        return _users.asStateFlow()
    }

    fun observeSelf(): Flow<RtcUser?> {
        return _currentUser.asStateFlow()
    }

    fun reset(currentUser: RtcUser, ownerUUID: String) {
        this.currentUserUUID = currentUser.userUUID
        this.ownerUUID = ownerUUID

        creator = null
        speakingJoiners.clear()
        handRaisingJoiners.clear()
        otherJoiners.clear()
        sortUser(mapOf(currentUser.userUUID to currentUser))
    }

    suspend fun initUsers(uuids: List<String>) {
        val usersInfo = userQuery.loadUsers(uuids)
        usersInfo.forEach { (uuid, roomUser) ->
            usersCache[uuid] = usersCache[uuid]?.copy(
                rtcUID = roomUser.rtcUID,
                name = roomUser.name,
                avatarURL = roomUser.avatarURL
            ) ?: RtcUser(
                roomUser.userUUID,
                roomUser.rtcUID,
                roomUser.name,
                roomUser.avatarURL
            )
        }
        sortAndNotify(usersCache)
    }

    private fun sortAndNotify(users: List<RtcUser>) {
        sortAndNotify(users.associateBy { it.userUUID })
    }

    private fun sortAndNotify(users: Map<String, RtcUser>) {
        sortUser(users)
        notifyUsers()
    }

//    private fun sortUser(rtcUser: RtcUser) {
//        if (rtcUser.userUUID == currentUserUUID) {
//            _currentUser.value = rtcUser.copy()
//        }
//
//        speakingJoiners.removeAll { it.userUUID == rtcUser.userUUID }
//        handRaisingJoiners.removeAll { it.userUUID == rtcUser.userUUID }
//        otherJoiners.removeAll { it.userUUID == rtcUser.userUUID }
//
//        when {
//            rtcUser.userUUID == ownerUUID -> {
//                creator = rtcUser
//            }
//            rtcUser.isSpeak -> {
//                speakingJoiners.add(rtcUser)
//            }
//            rtcUser.isRaiseHand -> {
//                handRaisingJoiners.add(rtcUser)
//            }
//            else -> {
//                otherJoiners.add(rtcUser)
//            }
//        }
//    }

    private fun sortUser(users: Map<String, RtcUser>) {
        if (users.containsKey(currentUserUUID)) {
            _currentUser.value = users[currentUserUUID]
        }

        speakingJoiners.removeAll { users.containsKey(it.userUUID) }
        handRaisingJoiners.removeAll { users.containsKey(it.userUUID) }
        otherJoiners.removeAll { users.containsKey(it.userUUID) }

        users.forEach { (_, rtcUser) ->
            when {
                rtcUser.userUUID == ownerUUID -> {
                    creator = rtcUser
                }
                rtcUser.isSpeak -> {
                    speakingJoiners.add(rtcUser)
                }
                rtcUser.isRaiseHand -> {
                    handRaisingJoiners.add(rtcUser)
                }
                else -> {
                    otherJoiners.add(rtcUser)
                }
            }
        }
    }

    suspend fun addUser(userUUID: String) {
        if (usersCache[userUUID] == null) {
            usersCache[userUUID] = RtcUser(userUUID = userUUID)
        }
        userQuery.loadUser(userUUID)?.let {
            updateUser(usersCache[userUUID]!!.copy(rtcUID = it.rtcUID, name = it.name, avatarURL = it.avatarURL))
        }
    }

    fun removeUser(userUUID: String) {
        usersCache.remove(userUUID)

        if (userUUID == ownerUUID) creator = null
        speakingJoiners.removeAll { it.userUUID == userUUID }
        handRaisingJoiners.removeAll { it.userUUID == userUUID }
        otherJoiners.removeAll { it.userUUID == userUUID }
        notifyUsers()
    }

    private fun updateUser(user: RtcUser) {
        usersCache[user.userUUID] = user
        sortAndNotify(mapOf(user.userUUID to user))
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

    fun findFirstUser(uuid: String): RtcUser? {
        return users.find { it.userUUID == uuid }
    }

    fun cancelHandRaising() {
        val updateUsers = handRaisingJoiners.toMutableList()
        handRaisingJoiners.clear()
        updateUsers.forEach { it.isRaiseHand = false }
        sortAndNotify(updateUsers)
    }

    fun updateDeviceState(uuid: String, videoOpen: Boolean, audioOpen: Boolean) {
        usersCache[uuid]?.apply {
            updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen))
            updateRtcStream(rtcUID, audioOpen, videoOpen)
        }
    }

    fun updateDeviceState(devicesState: Map<String, DeviceState>) {
        val updateUsers = mutableListOf<RtcUser>()
        devicesState.forEach { (uuid, state) ->
            usersCache[uuid]?.run {
                val user = this.copy(audioOpen = state.mic, videoOpen = state.camera)
                updateUsers.add(user)
                usersCache[uuid] = user
                updateRtcStream(this.rtcUID, state.mic, state.camera)
            }
        }
        sortAndNotify(updateUsers)
    }

    fun updateOnStage(onStages: Map<String, Boolean>) {
        val updateUsers = users.map {
            it.copy(isSpeak = onStages[it.userUUID] ?: false)
        }
        updateUsers.forEach {
            usersCache[it.userUUID] = it
        }
        sortAndNotify(updateUsers)
    }

    fun updateRaiseHandStatus(uuid: String, isRaiseHand: Boolean) {
        usersCache[uuid]?.run { updateUser(copy(isRaiseHand = isRaiseHand)) }
    }

    fun updateRaiseHandStatus(status: List<String>) {
        val updateUsers = users.map {
            it.copy(isRaiseHand = status.contains(it.userUUID))
        }
        updateUsers.forEach {
            usersCache[it.userUUID] = it
        }
        sortAndNotify(updateUsers)
    }

    // fun updateSpeakAndRaise(uuid: String, isSpeak: Boolean, isRaiseHand: Boolean) {
    //     usersCache[uuid]?.run {
    //         updateUser(copy(isSpeak = isSpeak, isRaiseHand = isRaiseHand))
    //     }
    // }

    fun handleAllOffStage() {
        val updateUsers = users.filter { it.userUUID != ownerUUID }.map { rtcUser ->
            rtcUser.copy(
                videoOpen = false,
                audioOpen = false,
                isSpeak = false,
                isRaiseHand = false,
            )
        }
        updateUsers.forEach {
            usersCache[it.userUUID] = it
            updateRtcStream(rtcUID = it.rtcUID, audioOpen = it.audioOpen, videoOpen = it.videoOpen)
        }
        if (currentUserUUID != ownerUUID) {
            boardRoom.setWritable(false)
        }
        sortAndNotify(updateUsers)
    }

    private fun updateRtcStream(rtcUID: Int, audioOpen: Boolean, videoOpen: Boolean) {
        if (rtcUID == currentUser?.rtcUID) {
            rtcApi.updateLocalStream(audio = audioOpen, video = videoOpen)
        } else {
            rtcApi.updateRemoteStream(rtcUid = rtcUID, audio = audioOpen, video = videoOpen)
        }
    }
}