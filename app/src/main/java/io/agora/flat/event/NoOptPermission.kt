package io.agora.flat.event

import io.agora.flat.di.impl.Event
import java.util.*

data class NoOptPermission(val id: Long = UUID.randomUUID().mostSignificantBits) : Event()
