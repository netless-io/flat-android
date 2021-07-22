package io.agora.flat.data.model

data class MessageQueryHistoryReq(
    val filter: MessageQueryFilter,
    val offset: Int = 0,
    val limit: Int = 100,
    val order: String = "asc",
)

data class MessageQueryFilter(
    val source: String? = null,
    val destination: String? = null,
    val start_time: String,
    val end_time: String,
)
