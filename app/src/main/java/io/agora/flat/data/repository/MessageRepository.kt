package io.agora.flat.data.repository

import io.agora.flat.Constants
import io.agora.flat.common.FlatException
import io.agora.flat.data.*
import io.agora.flat.data.api.MessageService
import io.agora.flat.data.api.MiscService
import io.agora.flat.data.model.MessageQueryFilter
import io.agora.flat.data.model.MessageQueryHistoryReq
import io.agora.flat.data.model.RtmQueryMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageService: MessageService,
    private val miscService: MiscService,
    private val userRepository: UserRepository,
) {
    private var rtmToken: String? = null

    private val dateFormat: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
    }

    suspend fun queryHistoryHandle(
        channel: String,
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
        order: String,
    ): Result<String> {
        val start: String = dateFormat.get().format(startTime)
        val end: String = dateFormat.get().format(endTime)
        return withContext(Dispatchers.IO) {
            if (rtmToken == null) {
                try {
                    val tokenResult = miscService.generateRtmToken().executeWithRetry().toResult()
                    if (tokenResult is Success) {
                        rtmToken = tokenResult.data.token
                    }
                } catch (e: Exception) {
                    return@withContext ErrorResult(e)
                }
            }

            val result = messageService.queryHistory(Constants.AGORA_APP_ID,
                MessageQueryHistoryReq(
                    filter = MessageQueryFilter(destination = channel, start_time = start, end_time = end),
                    limit = limit,
                    offset = offset,
                    order = order,
                ),
                userRepository.getUserInfo()!!.uuid,
                rtmToken!!
            ).executeOnce()

            try {
                val location = result.bodyOrThrow().location;
                if (!location.isNullOrEmpty()) {
                    val handle = location.replace(Regex("^.*/query/"), "")
                    Success(data = handle)
                } else {
                    ErrorResult(FlatException(0, ""))
                }
            } catch (e: Exception) {
                ErrorResult(e)
            }
        }
    }

    suspend fun getMessageList(handle: String): Result<List<RtmQueryMessage>> {
        return withContext(Dispatchers.IO) {
            val result = messageService.getMessageList(
                Constants.AGORA_APP_ID,
                handle,
                userRepository.getUserInfo()!!.uuid,
                rtmToken!!
            ).executeOnce()

            try {
                val code = result.bodyOrThrow().code;
                if (code == "ok") {
                    return@withContext Success(data = result.bodyOrThrow().messages)
                } else {
                    ErrorResult(FlatException(0, ""))
                }
            } catch (e: Exception) {
                ErrorResult(e)
            }
        }
    }

    suspend fun getMessageCount(channel: String, startTime: Long, endTime: Long): Result<Int> {
        val start: String = dateFormat.get().format(startTime)
        val end: String = dateFormat.get().format(endTime)

        return withContext(Dispatchers.IO) {
            if (rtmToken == null) {
                try {
                    val tokenResult = miscService.generateRtmToken().executeWithRetry().toResult()
                    if (tokenResult is Success) {
                        rtmToken = tokenResult.data.token
                    }
                } catch (e: Exception) {
                    return@withContext ErrorResult(e)
                }
            }

            val result = messageService.getMessageCount(
                Constants.AGORA_APP_ID,
                source = null,
                destination = channel,
                startTime = start,
                endTime = end,
                userRepository.getUserInfo()!!.uuid,
                rtmToken!!
            ).executeOnce()

            try {
                val code = result.bodyOrThrow().code;
                if (code == "ok") {
                    return@withContext Success(data = result.bodyOrThrow().count)
                } else {
                    ErrorResult(FlatException(0, ""))
                }
            } catch (e: Exception) {
                ErrorResult(e)
            }
        }
    }
}