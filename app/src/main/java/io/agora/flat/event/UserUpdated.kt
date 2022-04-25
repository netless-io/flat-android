package io.agora.flat.event

import io.agora.flat.di.impl.Event

/**
 * notify this event when account user info updated
 * workaround for observe [io.agora.flat.data.repository.UserRepository]
 */
object UserUpdated : Event()