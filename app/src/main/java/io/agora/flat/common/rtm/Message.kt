package io.agora.flat.common.rtm

data class Message constructor(
    // 消息类型
    var type: MessageType,
    // 消息体
    var body: MessageBody,
    // 发送者标识
    var sender: String,
    // 目标标识
    var target: String = NO_TARGET,
    // 时间戳
    var ts: Long = 0,
    // 消息唯一标识
    var uuid: String? = null,
) {
    companion object {
        const val SYSTEM_SENDER = "System"
        const val NO_TARGET = "NoTarget"
    }
}

sealed class MessageBody
data class NoticeMessageBody constructor(val ban: Boolean) : MessageBody()
data class TextMessageBody constructor(val message: String) : MessageBody()

enum class MessageType {
    Notice,
    Text,
    Image,
}

object MessageFactory {
    fun createText(sender: String, message: String, ts: Long = 0): Message {
        return Message(MessageType.Text, TextMessageBody(message = message), sender = sender, ts = ts)
    }

    fun createNotice(ban: Boolean): Message {
        return Message(MessageType.Notice, NoticeMessageBody(ban = ban), sender = Message.SYSTEM_SENDER)
    }
}