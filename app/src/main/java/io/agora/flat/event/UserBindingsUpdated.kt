package io.agora.flat.event

import java.util.*

/**
 * notify this event when account user info updated
 * workaround for observe [io.agora.flat.data.repository.UserRepository]
 */
data class UserBindingsUpdated(val id: Long = UUID.randomUUID().mostSignificantBits) : Event()