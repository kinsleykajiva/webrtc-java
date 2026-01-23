package dev.onvoid.webrtc;

public interface Interceptor {
    /**
     * Called for each outgoing RTP packet.
     *
     * @param packet The outgoing RTP packet.
     * @param sender The sender transmitting the packet.
     * @return The intercepted (and potentially modified) RTP packet.
     */
    RTCRtpPacket interceptRTP(RTCRtpPacket packet, RTCRtpSender sender);

    /**
     * Called for each incoming RTP packet.
     *
     * @param packet   The incoming RTP packet.
     * @param receiver The receiver which received the packet.
     * @return The intercepted (and potentially modified) RTP packet.
     */
    RTCRtpPacket interceptIncomingRTP(RTCRtpPacket packet, RTCRtpReceiver receiver);

    /**
     * Called for outgoing RTCP packets.
     *
     * @param packet The outgoing RTCP packet.
     * @return The intercepted (and potentially modified) RTCP packet.
     */
    RTCRtcpPacket interceptRTCP(RTCRtcpPacket packet);

    /**
     * Called for incoming RTCP packets.
     *
     * @param packet The incoming RTCP packet.
     * @return The intercepted (and potentially modified) RTCP packet.
     */
    RTCRtcpPacket interceptIncomingRTCP(RTCRtcpPacket packet);
}
