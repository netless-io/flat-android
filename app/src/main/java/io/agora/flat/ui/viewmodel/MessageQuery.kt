package io.agora.flat.ui.viewmodel

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.common.rtm.Message
import io.agora.flat.common.rtm.MessageFactory
import io.agora.flat.data.model.ORDER_ASC
import io.agora.flat.data.model.ORDER_DESC
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.ui.manager.UserQuery
import javax.inject.Inject

@ActivityRetainedScoped
class MessageQuery @Inject constructor(
    private val rtmApi: RtmApi,
    private val userQuery: UserQuery,
) {
    companion object {
        const val PAGE_LIMIT = 20
    }

    private val messageCache = mutableListOf<Message>()
    private val messages = mutableListOf<Message>()

    private lateinit var roomUUID: String
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var orderAsc: Boolean = true
    private var offset = 0
    private var querying = false

    var hasMore = true

    fun update(roomUUID: String, startTime: Long, endTime: Long, orderAsc: Boolean = true) {
        this.roomUUID = roomUUID
        this.startTime = startTime
        this.endTime = endTime
        this.orderAsc = orderAsc
        userQuery.update(roomUUID)
    }

    suspend fun query(timeOffset: Long): List<Message> {
        val targetTime = startTime + timeOffset
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
            userQuery.loadUsers(msgs.map { it.src }.distinct())
            val chatMessages = msgs.map {
                MessageFactory.createText(it.src, it.payload, it.ms)
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

    suspend fun loadMore(): List<Message> {
        if (querying) {
            return emptyList()
        }
        var result = emptyList<Message>()
        if (hasMore) {
            querying = true
            val msgs = rtmApi.getTextHistory(
                roomUUID,
                startTime,
                endTime,
                PAGE_LIMIT,
                offset,
                if (orderAsc) ORDER_ASC else ORDER_DESC
            )
            if (msgs.size < PAGE_LIMIT) {
                hasMore = false
            } else {
                offset += msgs.size
            }
            userQuery.loadUsers(msgs.map { it.src }.distinct())
            result = msgs.map {
                MessageFactory.createText(it.src, it.payload, it.ms)
            }
            querying = false
        }
        return result
    }
}