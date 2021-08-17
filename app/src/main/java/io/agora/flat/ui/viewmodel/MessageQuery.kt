package io.agora.flat.ui.viewmodel

import io.agora.flat.data.model.ORDER_ASC
import io.agora.flat.data.model.ORDER_DESC
import io.agora.flat.di.interfaces.RtmEngineProvider

class MessageQuery constructor(
    private val roomUUID: String,
    private val startTime: Long,
    private val endTime: Long,
    private val rtmApi: RtmEngineProvider,
    private val userQuery: UserQuery,
    private val orderAsc: Boolean = true,
) {
    private val messageCache = mutableListOf<RTMMessage>()
    private val messages = mutableListOf<RTMMessage>()
    private var hasMore = true
    private var offset = 0
    private var querying = false
    private val PAGE_LIMIT = 20

    suspend fun query(timeOffset: Long): List<RTMMessage> {
        val targetTime = startTime + timeOffset;
        if (querying) {
            return messages
        }
        querying = true
        if (hasMore && messageCache.lastOrNull()?.ts ?: 0 < targetTime) {
            val msgs = rtmApi.getTextHistory(roomUUID, startTime, endTime, PAGE_LIMIT, offset)
            if (msgs.size < PAGE_LIMIT) {
                hasMore = false
            } else {
                offset += msgs.size
            }
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

        if (messages.isNotEmpty() && messages.last().ts > targetTime) {
            messages.clear()
        }

        val start = messages.size
        var end = messageCache.size - 1
        while (end > start && messageCache[end].ts > targetTime) {
            end--
        }
        if (end > start) {
            messages.addAll(messageCache.slice(IntRange(start, end)))
        }

        return messages
    }

    suspend fun loadMore(): List<RTMMessage> {
        if (querying) {
            return emptyList()
        }
        var result = emptyList<RTMMessage>()
        if (hasMore) {
            querying = true
            val msgs = rtmApi.getTextHistory(
                roomUUID,
                startTime,
                endTime,
                PAGE_LIMIT,
                offset,
                order = if (orderAsc) ORDER_ASC else ORDER_DESC)
            if (msgs.size < PAGE_LIMIT) {
                hasMore = false
            } else {
                offset += msgs.size
            }
            val users = userQuery.getUsers(msgs.map { it.src }.distinct())
            result = msgs.map {
                ChatMessage(
                    name = users[it.src]?.name ?: "",
                    message = it.payload,
                    isSelf = userQuery.isSelf(it.src),
                    ts = it.ms
                )
            }
            querying = false
        }
        return result;
    }

    fun hasMoreMessage(): Boolean {
        return hasMore
    }
}