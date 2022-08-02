package io.agora.flat.data.model

import io.agora.flat.BuildConfig

data class JoinRoomReq(val uuid: String, val version: String = BuildConfig.VERSION_NAME)
