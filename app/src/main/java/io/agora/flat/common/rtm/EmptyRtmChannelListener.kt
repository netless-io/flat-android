package io.agora.flat.common.rtm

import android.util.Log
import io.agora.flat.di.impl.RtmApiImpl
import io.agora.rtm.*

interface EmptyRtmChannelListener : RtmChannelListener {
    override fun onMemberCountUpdated(count: Int) {
        Log.d(RtmApiImpl.TAG, "onMemberCountUpdated")
    }

    override fun onAttributesUpdated(attributes: MutableList<RtmChannelAttribute>) {
        Log.d(RtmApiImpl.TAG, "onAttributesUpdated")
    }

    override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
        Log.d(RtmApiImpl.TAG, "onMessageReceived ${message.text} ${member.userId} ${member.channelId}")
    }

    override fun onImageMessageReceived(imageMessage: RtmImageMessage, member: RtmChannelMember) {
        Log.d(RtmApiImpl.TAG, "onImageMessageReceived")
    }

    override fun onFileMessageReceived(fileMessage: RtmFileMessage, member: RtmChannelMember) {
        Log.d(RtmApiImpl.TAG, "onFileMessageReceived")
    }

    override fun onMemberJoined(member: RtmChannelMember) {
        Log.d(RtmApiImpl.TAG, "onMemberJoined ${member.userId}")
    }

    override fun onMemberLeft(member: RtmChannelMember) {
        Log.d(RtmApiImpl.TAG, "onMemberLeft ${member.userId}")
    }
}