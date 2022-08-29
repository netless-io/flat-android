package io.agora.flat.event

import java.util.*

data class NoOptPermission(val id: Long = UUID.randomUUID().mostSignificantBits) : Event()
