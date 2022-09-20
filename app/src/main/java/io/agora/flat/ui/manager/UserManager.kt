package io.agora.flat.ui.manager

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.common.board.DeviceState
import io.agora.flat.data.model.RoomUser
import io.agora.flat.di.interfaces.RtmApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ActivityRetainedScoped
class UserManager @Inject constructor(
    private val userQuery: UserQuery,
    private val rtmApi: RtmApi
) {
    private var _users = MutableStateFlow<List<RoomUser>>(emptyList())
    val users: List<RoomUser>
        get() = _users.value

    private var _currentUser = MutableStateFlow<RoomUser?>(null)
    val currentUser: RoomUser?
        get() = _currentUser.value

    private var creator: RoomUser? = null
    private var speakingJoiners: MutableList<RoomUser> = mutableListOf()
    private var handRaisingJoiners: MutableList<RoomUser> = mutableListOf()
    private var otherJoiners: MutableList<RoomUser> = mutableListOf()

    // current all users
    private var usersCache = mutableMapOf<String, RoomUser>()

    private var devicesState: Map<String, DeviceState> = emptyMap()
    private var raiseHandState: List<String> = emptyList()

    lateinit var selfUUID: String
    lateinit var ownerUUID: String

    fun observeUsers(): Flow<List<RoomUser>> {
        return _users.asStateFlow()
    }

    fun observeSelf(): Flow<RoomUser?> {
        return _currentUser.asStateFlow()
    }

    fun reset(currentUser: RoomUser, ownerUUID: String) {
        this.selfUUID = currentUser.userUUID
        this.ownerUUID = ownerUUID

        creator = null
        speakingJoiners.clear()
        handRaisingJoiners.clear()
        otherJoiners.clear()

        val updateUsers = mutableMapOf(selfUUID to currentUser)
        if (selfUUID != ownerUUID) {
            updateUsers[ownerUUID] = RoomUser(rtcUID = RoomUser.NOT_JOIN_RTC_UID, userUUID = ownerUUID, isOwner = true)
        }
        updateUserCache(updateUsers)
        sortUser(updateUsers)
    }

    private fun updateUserCache(updateUser: RoomUser) {
        usersCache[updateUser.userUUID] = updateUser
    }

    private fun updateUserCache(updateUsers: Map<String, RoomUser>) {
        usersCache.putAll(updateUsers)
    }

    suspend fun initUsers(uuids: List<String>) {
        val usersInfo = userQuery.loadUsers(uuids)
        usersInfo.forEach { (uuid, roomUser) ->
            val user = usersCache[uuid]?.copy(
                rtcUID = roomUser.rtcUID,
                name = roomUser.name,
                avatarURL = roomUser.avatarURL
            ) ?: RoomUser(
                roomUser.userUUID,
                roomUser.rtcUID,
                roomUser.name,
                roomUser.avatarURL,
            )
            updateUserCache(user)
        }
        sortAndNotify(usersCache)
    }

    private fun sortAndNotify(users: List<RoomUser>) {
        sortAndNotify(users.associateBy { it.userUUID })
    }

    private fun sortAndNotify(users: Map<String, RoomUser>) {
        sortUser(users)
        notifyUsers()
    }

    private fun sortUser(users: Map<String, RoomUser>) {
        if (users.containsKey(selfUUID)) {
            _currentUser.value = users[selfUUID]
        }

        speakingJoiners.removeAll { users.containsKey(it.userUUID) }
        handRaisingJoiners.removeAll { users.containsKey(it.userUUID) }
        otherJoiners.removeAll { users.containsKey(it.userUUID) }

        users.forEach { (_, rtcUser) ->
            when {
                rtcUser.isOwner -> {
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
            usersCache[userUUID] = RoomUser(userUUID = userUUID)
        }
        userQuery.loadUser(userUUID)?.let {
            val user = usersCache[userUUID]?.copy(
                rtcUID = it.rtcUID,
                name = it.name,
                avatarURL = it.avatarURL,
                videoOpen = devicesState[it.userUUID]?.camera ?: false,
                audioOpen = devicesState[it.userUUID]?.mic ?: false,
                isRaiseHand = raiseHandState.contains(it.userUUID)
            ) ?: return
            updateAndNotifyUser(user)
        }
    }

    fun removeUser(userUUID: String) {
        val user = usersCache[userUUID] ?: return
        if (user.isOwner || user.isSpeak) {
            val updateUser = user.copy(rtcUID = RoomUser.NOT_JOIN_RTC_UID)
            updateAndNotifyUser(updateUser)
        } else {
            usersCache.remove(userUUID)
            if (userUUID == ownerUUID) creator = null
            speakingJoiners.removeAll { it.userUUID == userUUID }
            handRaisingJoiners.removeAll { it.userUUID == userUUID }
            otherJoiners.removeAll { it.userUUID == userUUID }
            notifyUsers()
        }
    }

    private fun updateAndNotifyUser(user: RoomUser) {
        updateAndNotifyUser(listOf(user))
    }

    private fun updateAndNotifyUser(users: List<RoomUser>) {
        updateUserCache(users.associateBy { it.userUUID })
        sortAndNotify(users)
    }

    private fun notifyUsers() {
        val ranked = mutableListOf<RoomUser>()
        ranked += speakingJoiners
        ranked += handRaisingJoiners
        creator?.run {
            ranked += this
        }
        ranked += otherJoiners

        _users.value = ranked
    }

    fun findFirstUser(uuid: String): RoomUser? {
        return users.find { it.userUUID == uuid }
    }

    fun updateDeviceState(devicesState: Map<String, DeviceState>) {
        val updateUuids = this.devicesState.keys + devicesState.keys
        this.devicesState = devicesState
        val updateUsers = mutableListOf<RoomUser>()
        updateUuids.forEach { uuid ->
            val user = usersCache[uuid]?.copy(
                audioOpen = devicesState[uuid]?.mic ?: false,
                videoOpen = devicesState[uuid]?.camera ?: false
            )
            if (user != null) {
                updateUsers.add(user)
            }
        }
        updateAndNotifyUser(updateUsers)
    }

    suspend fun updateOnStage(onStages: Map<String, Boolean>) {
        val updateUuids = speakingJoiners.map { it.userUUID }.toMutableSet() + onStages.keys
        val updateUsers = mutableListOf<RoomUser>()
        updateUuids.forEach {
            var user = usersCache[it]?.copy(
                isSpeak = onStages[it] ?: false,
                videoOpen = devicesState[it]?.camera ?: false,
                audioOpen = devicesState[it]?.mic ?: false,
                isRaiseHand = raiseHandState.contains(it)
            )
            if (onStages[it] == true && user == null) {
                user = RoomUser(
                    userUUID = it,
                    name = userQuery.loadUser(it)?.name,
                    isSpeak = onStages[it] ?: false,
                    videoOpen = devicesState[it]?.camera ?: false,
                    audioOpen = devicesState[it]?.mic ?: false,
                    isRaiseHand = raiseHandState.contains(it)
                )
            }
            if (user != null) updateUsers.add(user)
        }
        updateAndNotifyUser(updateUsers)
    }

    fun updateRaiseHandStatus(uuid: String, isRaiseHand: Boolean) {
        usersCache[uuid]?.run { updateAndNotifyUser(copy(isRaiseHand = isRaiseHand)) }
    }

    fun updateRaiseHandStatus(status: List<String>) {
        this.raiseHandState = status
        val updateUsers = users.map {
            it.copy(isRaiseHand = status.contains(it.userUUID))
        }
        updateAndNotifyUser(updateUsers)
    }

    fun handleAllOffStage() {
        val updateUsers = users.filter { it.userUUID != ownerUUID }.map { rtcUser ->
            rtcUser.copy(
                videoOpen = false,
                audioOpen = false,
                isSpeak = false,
                isRaiseHand = false,
            )
        }
        updateAndNotifyUser(updateUsers)
    }

    fun isUserSelf(userId: String): Boolean {
        return userId == selfUUID
    }

    fun isOwner(uuid: String = selfUUID): Boolean {
        return uuid == ownerUUID
    }

    fun isOwnerOnStage(): Boolean {
        return creator?.isOnStage ?: false
    }

    fun getOnStageCount(): Int {
        return users.count { it.isOnStage }
    }
}