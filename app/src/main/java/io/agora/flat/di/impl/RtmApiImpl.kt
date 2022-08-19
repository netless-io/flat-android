package io.agora.flat.di.impl

import android.content.Context
import android.util.Log
import io.agora.flat.common.FlatException
import io.agora.flat.common.FlatRtmException
import io.agora.flat.common.rtm.*
import io.agora.flat.data.AppEnv
import io.agora.flat.data.Success
import io.agora.flat.data.model.RtmQueryMessage
import io.agora.flat.data.repository.MessageRepository
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.RtmApi
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.rtm.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class RtmApiImpl @Inject constructor(
    private val messageRepository: MessageRepository,
    private val appEnv: AppEnv,
    private val logger: Logger
) : RtmApi, StartupInitializer {
    companion object {
        val TAG = RtmApiImpl::class.simpleName

        const val CONNECTION_STATE_DISCONNECTED = 1
        const val CONNECTION_STATE_CONNECTING = 2
        const val CONNECTION_STATE_CONNECTED = 3
        const val CONNECTION_STATE_RECONNECTING = 4
        const val CONNECTION_STATE_ABORTED = 5

        const val CONNECTION_CHANGE_REASON_LOGIN = 1
        const val CONNECTION_CHANGE_REASON_LOGIN_SUCCESS = 2
        const val CONNECTION_CHANGE_REASON_LOGIN_FAILURE = 3
        const val CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT = 4
        const val CONNECTION_CHANGE_REASON_INTERRUPTED = 5
        const val CONNECTION_CHANGE_REASON_LOGOUT = 6
        const val CONNECTION_CHANGE_REASON_BANNED_BY_SERVER = 7
        const val CONNECTION_CHANGE_REASON_REMOTE_LOGIN = 8
    }

    private lateinit var rtmClient: RtmClient
    private var rtmClientListener: RtmClientListener
    private var rtmListeners = mutableListOf<RTMListener>()
    private var messageChannel: RtmChannel? = null

    init {
        rtmClientListener = object : EmptyRtmClientListener {
            override fun onConnectionStateChanged(state: Int, reason: Int) {
                logger.i("Connection state changes to $state reason:$reason")
                if (reason == CONNECTION_CHANGE_REASON_REMOTE_LOGIN) {
                    rtmListeners.forEach { it.onRemoteLogin() }
                }
            }

            override fun onMessageReceived(message: RtmMessage, peerId: String) {
                logger.d("Message received from $peerId ${String(message.rawMessage)}")
                rtmListeners.forEach {
                    it.onClassEvent(ClassRtmEvent.parse(String(message.rawMessage), peerId))
                }
            }
        }
    }

    override fun init(context: Context) {
        try {
            rtmClient = RtmClient.createInstance(context, appEnv.agoraAppId, rtmClientListener)
        } catch (e: Exception) {
            logger.e(e, "RTM SDK init fatal error!")
        }
    }

    private var messageListener = object : EmptyRtmChannelListener {
        override fun onMemberJoined(member: RtmChannelMember) {
            super.onMemberJoined(member)
            rtmListeners.forEach {
                it.onMemberJoined(member.userId, member.channelId)
            }
        }

        override fun onMemberLeft(member: RtmChannelMember) {
            super.onMemberLeft(member)
            rtmListeners.forEach {
                it.onMemberLeft(member.userId, member.channelId)
            }
        }

        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            super.onMessageReceived(message, member)

            when (message.messageType) {
                // chat message
                RtmMessageType.TEXT -> {
                    rtmListeners.forEach {
                        it.onChatMessage(ChatMessage(message.text, member.userId))
                    }
                }
                // command
                RtmMessageType.RAW -> {
                    rtmListeners.forEach {
                        it.onClassEvent(ClassRtmEvent.parse(String(message.rawMessage), member.userId))
                    }
                }
            }
        }
    }

    override fun rtmEngine(): RtmClient {
        return rtmClient
    }

    override suspend fun login(rtmToken: String, channelId: String, userUUID: String): Boolean {
        login(rtmToken, userUUID)
        messageChannel = joinChannel(channelId, messageListener)
        return true
    }

    private suspend fun login(token: String, userId: String): Boolean = suspendCoroutine { cont ->
        rtmClient.login(token, userId, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(e: ErrorInfo) {
                cont.resumeWithException(e.toFlatException())
            }
        })
    }

    override suspend fun logout(): Boolean = suspendCoroutine { cont ->
        rtmClient.logout(object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(e: ErrorInfo) {
                cont.resumeWithException(e.toFlatException())
            }
        })
    }

    private suspend fun joinChannel(channelId: String, listener: RtmChannelListener): RtmChannel = suspendCoroutine {
        val channel = rtmClient.createChannel(channelId, listener)
        channel.join(object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                logger.d("rtm join channel success")
                it.resume(channel)
            }

            override fun onFailure(e: ErrorInfo) {
                logger.i("rtm join channel fail")
                it.resumeWithException(e.toFlatException())
            }
        })
    }

    override suspend fun getMembers(): List<RtmChannelMember> = suspendCoroutine { cont ->
        messageChannel?.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(members: List<RtmChannelMember>) {
                logger.d("get member success $members")
                cont.resume(members)
            }

            override fun onFailure(e: ErrorInfo) {
                logger.d("get member failure $e")
                cont.resume(listOf())
            }
        }) ?: cont.resume(listOf())
    }

    private var sendMessageOptions = SendMessageOptions().apply {
        enableHistoricalMessaging = true
    }

    override suspend fun sendChannelMessage(msg: String): Boolean = suspendCoroutine { cont ->
        val message = rtmClient.createMessage()
        message.text = msg

        messageChannel?.sendMessage(message, sendMessageOptions, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(errorIn: ErrorInfo) {
                cont.resume(false)
            }
        }) ?: cont.resume(false)
    }

    override suspend fun sendChannelCommand(event: ClassRtmEvent) = suspendCoroutine<Boolean> { cont ->
        val message = rtmClient.createMessage()
        message.rawMessage = ClassRtmEvent.toText(event).toByteArray()

        messageChannel?.sendMessage(message, sendMessageOptions, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(error: ErrorInfo) {
                cont.resume(false)
            }
        }) ?: cont.resume(false)
    }

    override suspend fun sendPeerCommand(event: ClassRtmEvent, peerId: String) = suspendCoroutine<Boolean> { cont ->
        Log.d(TAG, "sendPeerCommand ${ClassRtmEvent.toText(event)}")
        val message = rtmClient.createMessage()
        message.rawMessage = ClassRtmEvent.toText(event).toByteArray()

        val option = SendMessageOptions().apply {
            enableOfflineMessaging = true
        }

        rtmClient.sendMessageToPeer(peerId, message, option, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(error: ErrorInfo) {
                cont.resume(false)
            }
        })
    }

    override suspend fun getTextHistory(
        channelId: String,
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
        order: String,
    ): List<RtmQueryMessage> {
        val result = messageRepository.queryHistoryHandle(channelId, startTime, endTime, limit, offset, order)
        if (result is Success) {
            repeat(3) {
                delay(2000)
                val messageResult = messageRepository.getMessageList(handle = result.data)
                if (messageResult is Success) {
                    return messageResult.data
                }
            }
        }
        return listOf()
    }

    override suspend fun getTextHistoryCount(channelId: String, startTime: Long, endTime: Long): Int {
        repeat(3) {
            delay(2000)
            val result = messageRepository.getMessageCount(channelId, startTime, endTime)
            if (result is Success) {
                return result.data
            }
        }
        return -1
    }

    override fun observeRtmEvent(): Flow<ClassRtmEvent> = callbackFlow {
        val listener = object : RTMListener {
            override fun onClassEvent(event: ClassRtmEvent) {
                trySend(event)
            }

            override fun onChatMessage(chatMessage: ChatMessage) {
                trySend(chatMessage)
            }

            override fun onRemoteLogin() {
                trySend(OnRemoteLogin)
            }

            override fun onMemberJoined(userId: String, channelId: String) {
                trySend(OnMemberJoined(userId = userId, channelId = channelId))
            }

            override fun onMemberLeft(userId: String, channelId: String) {
                trySend(OnMemberLeft(userId = userId, channelId = channelId))
            }
        }
        rtmListeners.add(listener)
        awaitClose {
            logger.d("rtm event flow close called")
            rtmListeners.remove(listener)
        }
    }

    internal fun ErrorInfo.toFlatException(): FlatException {
        return FlatRtmException(this.errorDescription, this.errorCode)
    }
}