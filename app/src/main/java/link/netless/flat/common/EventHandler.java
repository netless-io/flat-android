package link.netless.flat.common;

import io.agora.rtc.IRtcEngineEventHandler;

public interface EventHandler {
    void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed);

    void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats);

    void onJoinChannelSuccess(String channel, int uid, int elapsed);

    void onUserOffline(int uid, int reason);

    void onUserJoined(int uid, int elapsed);

    default void onLastmileQuality(int quality) {
    }

    default void onLastmileProbeResult(IRtcEngineEventHandler.LastmileProbeResult result) {
    }

    default void onLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats stats) {
    }

    default void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
    }

    default void onNetworkQuality(int uid, int txQuality, int rxQuality) {
    }

    default void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats) {
    }

    default void onRemoteAudioStats(IRtcEngineEventHandler.RemoteAudioStats stats) {
    }
}
