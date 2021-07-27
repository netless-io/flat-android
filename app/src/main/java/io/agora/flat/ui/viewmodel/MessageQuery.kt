package io.agora.flat.ui.viewmodel

import io.agora.flat.di.interfaces.RtmEngineProvider

class MessageQuery constructor(
    private val roomUUID: String,
    private val startTime: Long,
    private val endTime: Long,
    private val rtmApi: RtmEngineProvider,
    private val userQuery: UserQuery,
    private val interval: Long = 60000,
) {
    private val messageCache = mutableListOf<RTMMessage>()
    private val messages = mutableListOf<RTMMessage>()
    private var segment = 0
    private var querying = false

    suspend fun query(time: Long): List<RTMMessage> {
        if (querying) {
            return messages
        }

        val segs = (time / interval + 1).toInt()
        for (i in segment until segs) {
            querying = true
            var msgs = rtmApi.getTextHistory(
                roomUUID,
                startTime + i * interval,
                startTime + (i + 1) * interval,
            )
            segment++
            val users = userQuery.getUsers(msgs.map { it.src }.distinct())
            val chatMessages = msgs.map {
                ChatMessage(
                    name = users[it.src]?.name ?: "",
                    message = it.payload,
                    isSelf = userQuery.isSelf(it.src),
                    ts = it.ms
                )
            }
            messageCache.addAll(chatMessages)
        }
        querying = false

        if (messages.isNotEmpty() && messages.last().ts > startTime + time) {
            messages.clear()
        }

        val start = messages.size
        var end = messageCache.size - 1
        while (end > start && messageCache[end].ts > startTime + time) {
            end--
        }
        if (end > start) {
            messages.addAll(messageCache.slice(IntRange(start, end)))
        }

        return messages
    }
}