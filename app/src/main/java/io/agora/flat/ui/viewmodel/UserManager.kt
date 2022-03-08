package io.agora.flat.ui.viewmodel

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.data.model.RTMUserProp
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.RoomConfigRepository
import io.agora.flat.di.interfaces.RtcApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ActivityRetainedScoped
class UserManager @Inject constructor(
    private val roomConfigRepository: RoomConfigRepository,
    private val userQuery: UserQuery,
    private val rtcApi: RtcApi,
) {
    private var _users = MutableStateFlow<List<RtcUser>>(emptyList())
    val users = _users.asStateFlow()

    private var _currentUser = MutableStateFlow<RtcUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private var creator: RtcUser? = null
    private var speakingJoiners: MutableList<RtcUser> = mutableListOf()
    private var handRaisingJoiners: MutableList<RtcUser> = mutableListOf()
    private var otherJoiners: MutableList<RtcUser> = mutableListOf()

    private lateinit var roomUUID: String
    private lateinit var userUUID: String
    private lateinit var ownerUUID: String
    private lateinit var scope: CoroutineScope

    fun reset(roomUUID: String, userUUID: String, ownerUUID: String, scope: CoroutineScope) {
        this.roomUUID = roomUUID
        this.userUUID = userUUID
        this.ownerUUID = ownerUUID
        this.scope = scope

        creator = null
        speakingJoiners.clear()
        handRaisingJoiners.clear()
        otherJoiners.clear()
        notifyUsers()
    }

    suspend fun init(uuids: List<String>): Boolean = suspendCoroutine { cont ->
        scope.launch {
            val userMap = userQuery.loadUsers(uuids)
            userMap[userUUID]?.run {
                val config = roomConfigRepository.getRoomConfig(roomUUID)
                this.audioOpen = config?.enableAudio ?: false
                this.videoOpen = config?.enableVideo ?: false
            }
            sortAndNotify(userMap.values.toList())
            cont.resume(true)
        }
    }

    private fun sortAndNotify(users: List<RtcUser>) {
        users.forEach {
            if (it.userUUID == userUUID) {
                _currentUser.value = it.copy()
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

    fun addUser(userUUID: String) {
        scope.launch {
            userQuery.loadUsers(listOf(userUUID)).run {
                sortAndNotify(values.toList())
            }
        }
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

    private fun updateUser(user: RtcUser) {
        removeUser(user.userUUID, false)
        sortAndNotify(listOf(user))
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

    fun findFirstOtherUser(): RtcUser? {
        return users.value.find { it.userUUID != userUUID }
    }

    fun findFirstUser(uuid: String): RtcUser? {
        return users.value.find { it.userUUID == uuid }
    }

    fun username(uuid: String): String {
        return findUser(uuid)?.name ?: ""
    }

    fun cancelHandRaising() {
        val updateUsers = handRaisingJoiners.toMutableList()
        handRaisingJoiners.clear()
        updateUsers.forEach { it.isRaiseHand = false }

        sortAndNotify(updateUsers)
    }

    fun updateDeviceState(uuid: String, videoOpen: Boolean, audioOpen: Boolean) {
        findUser(uuid)?.apply {
            updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen))

            updateRtcStream(rtcUID, audioOpen, videoOpen)
        }
    }

    fun updateUserState(uuid: String, audioOpen: Boolean, videoOpen: Boolean, name: String, isSpeak: Boolean) {
        findUser(uuid)?.apply {
            updateUser(copy(audioOpen = audioOpen, videoOpen = videoOpen, name = name, isSpeak = isSpeak))
        }
    }

    fun updateSpeakStatus(uuid: String, isSpeak: Boolean) {
        findUser(uuid)?.run {
            updateUser(copy(isSpeak = isSpeak))
        }
    }

    fun updateRaiseHandStatus(uuid: String, isRaiseHand: Boolean) {
        findUser(uuid)?.run {
            updateUser(copy(isRaiseHand = isRaiseHand))
        }
    }

    fun updateSpeakAndRaise(uuid: String, isSpeak: Boolean, isRaiseHand: Boolean) {
        findUser(uuid)?.run {
            updateUser(copy(isSpeak = isSpeak, isRaiseHand = isRaiseHand))
        }
    }

    fun updateUserStates(uStates: HashMap<String, String>) {
        val defaultFlag = ""
        val updateUsers = users.value.toMutableList().map { rtcUser ->
            val s = uStates[rtcUser.userUUID] ?: defaultFlag
            rtcUser.copy(
                videoOpen = s.contains(RTMUserProp.Camera.flag, ignoreCase = true),
                audioOpen = s.contains(RTMUserProp.Mic.flag, ignoreCase = true),
                isSpeak = s.contains(RTMUserProp.IsSpeak.flag, ignoreCase = true),
                isRaiseHand = s.contains(RTMUserProp.IsRaiseHand.flag, ignoreCase = true)
            )
        }
        updateUsers.forEach {
            updateRtcStream(it.rtcUID, it.audioOpen, it.videoOpen)
        }
        sortAndNotify(updateUsers)
    }

    private fun findUser(uuid: String): RtcUser? {
        return users.value.find { it.userUUID == uuid }
    }

    private fun updateRtcStream(rtcUID: Int, audioOpen: Boolean, videoOpen: Boolean) {
        rtcApi.rtcEngine().muteRemoteAudioStream(rtcUID, !audioOpen)
        rtcApi.rtcEngine().muteRemoteVideoStream(rtcUID, !videoOpen)
    }
}