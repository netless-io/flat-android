package io.agora.flat.data.model

data class WeChatSetAuthIdReq constructor(
    // for csrf
    val authUUID: String,
)
