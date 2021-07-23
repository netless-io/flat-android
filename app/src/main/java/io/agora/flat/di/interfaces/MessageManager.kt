package io.agora.flat.di.interfaces

import io.agora.flat.data.model.RtmQueryMessage

/**
 * 提供聊天历史消息获取，回放消息
 */
interface MessageManager {

}

class MessageQuery constructor(
    val roomUUID: String,
    val startTime: Long,
    val endTime: Long,
    val rtmApi: RtmEngineProvider,
    private val interval: Long = 60000,
) {
    val messageCache = mutableListOf<RtmQueryMessage>()
    val messages = mutableListOf<RtmQueryMessage>()
    var segment = 0
    var querying = false

    suspend fun query(time: Long): List<RtmQueryMessage> {
        if (querying) {
            return messages
        }
        var segs = (time / interval + 1).toInt()
        while (segs > segment) {
            querying = true
            var msgs = rtmApi.getTextHistory(
                roomUUID,
                startTime + segment * interval,
                startTime + (segment + 1) * interval,
            )
            segment++
            messageCache.addAll(msgs)
        }
        querying = false

        if (messages.isNotEmpty() && messages.last().ms > startTime + time) {
            messages.clear()
        }
        val start = messages.size
        var end = messageCache.size - 1
        while (end > start && messageCache[end].ms > startTime + time) {
            end--
        }
        if (end > start) {
            messages.addAll(messageCache.slice(IntRange(start, end)))
        }

        return messages
    }
}