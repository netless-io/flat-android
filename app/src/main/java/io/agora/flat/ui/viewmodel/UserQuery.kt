package io.agora.flat.ui.viewmodel

import io.agora.flat.data.Success
import io.agora.flat.data.model.RtcUser
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.data.repository.UserRepository

class UserQuery(val roomUUID: String, val userRepository: UserRepository, val roomRepository: RoomRepository) {
    private var userMap = mutableMapOf<String, RtcUser>()

    fun isSelf(uuid: String): Boolean {
        return userRepository.getUserUUID() == uuid
    }

    suspend fun getUsers(uuids: List<String>): Map<String, RtcUser> {
        val filter = uuids.filter { !userMap.containsKey(it) }
        if (filter.isNotEmpty()) {
            val result = roomRepository.getRoomUsers(roomUUID, filter)
            if (result is Success) {
                result.data.forEach {
                    it.value.userUUID = it.key
                }
                userMap.putAll(result.data)
            }
        }
        return userMap.filter { uuids.contains(it.key) }
    }
}