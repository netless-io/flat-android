package io.agora.flat.data.model

const val ORDER_ASC = "asc"
const val ORDER_DESC = "desc"

data class MessageQueryHistoryReq(
    val filter: MessageQueryFilter,
    val offset: Int = 0,
    val limit: Int = 100,
    val order: String = ORDER_ASC,
)

data class MessageQueryFilter(
    val source: String? = null,
    val destination: String? = null,
    val start_time: String,
    val end_time: String,
)
