package io.agora.flat.ui.manager

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.common.board.BoardRoom
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
        sortUser(currentUser)
    }

    suspend fun initUsers(uuids: List<String>) {
        val userMap = userQuery.loadUsers(uuids).mapValues {
            RtcUser(
                it.value.userUUID,
                it.value.rtcUID,
                it.value.name,
                it.value.avatarURL
            )
        }
        userMap.forEach { (uuid, user) ->
            usersCache[uuid] = usersCache[uuid]?.copy(
                rtcUID = user.rtcUID,
                name = user.name,
                avatarURL = user.avatarURL
            ) ?: user
        }
        sortAndNotify(userMap.values.toList())
    }

    private fun sortAndNotify(users: List<RtcUser>) {
        users.forEach(this::sortUser)
        notifyUsers()
    }

    private fun sortUser(rtcUser: RtcUser) {
        if (rtcUser.userUUID == currentUserUUID) {
            _currentUser.value = rtcUser.copy()
        }

        speakingJoiners.removeAll { it.userUUID == rtcUser.userUUID }
        handRaisingJoiners.removeAll { it.userUUID == rtcUser.userUUID }
        otherJoiners.removeAll { it.userUUID == rtcUser.userUUID }

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
        sortUser(user)
        notifyUsers()
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

    fun updateOnStage(onStages: Map<String, Boolean>) {
        val usersList = users.map {
            it.copy(isSpeak = onStages[it.userUUID] ?: false)
        }
        usersList.forEach {
            usersCache[it.userUUID] = it
        }
        sortAndNotify(usersList)
    }

    fun updateRaiseHandStatus(uuid: String, isRaiseHand: Boolean) {
        usersCache[uuid]?.run { updateUser(copy(isRaiseHand = isRaiseHand)) }
    }

    fun updateRaiseHandStatus(status: List<String>) {
        val usersList = users.map {
            it.copy(isRaiseHand = status.contains(it.userUUID))
        }
        usersList.forEach {
            usersCache[it.userUUID] = it
        }
        sortAndNotify(usersList)
    }

    fun updateSpeakAndRaise(uuid: String, isSpeak: Boolean, isRaiseHand: Boolean) {
        usersCache[uuid]?.run {
            updateUser(copy(isSpeak = isSpeak, isRaiseHand = isRaiseHand))
        }
    }

    fun handleAllOffStage() {
        val updateUsers = users.filter { it.userUUID != ownerUUID }.toMutableList().map { rtcUser ->
            rtcUser.copy(
                videoOpen = false,
                audioOpen = false,
                isSpeak = false,
                isRaiseHand = false,
            )
        }
        updateUsers.forEach {
            updateRtcStream(rtcUID = it.rtcUID, audioOpen = it.audioOpen, videoOpen = it.videoOpen)
        }
        boardRoom.setWritable(false)
        updateUsers.forEach {
            usersCache[it.userUUID] = it
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