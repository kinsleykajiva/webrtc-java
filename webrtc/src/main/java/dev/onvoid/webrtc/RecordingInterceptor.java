package dev.onvoid.webrtc;

/**
 * A simple example interceptor that logs the sequence numbers of outgoing
 * audio RTP packets.
 */
public class RecordingInterceptor implements Interceptor {

    @Override
    public RTCRtpPacket interceptRTP(RTCRtpPacket packet, RTCRtpSender sender) {
        RTCRtpHeader header = packet.getHeader();
        System.out.println("Outgoing Audio RTP: SSRC=" + header.ssrc +
                ", Seq=" + header.sequenceNumber +
                ", TS=" + header.timestamp);
        return packet;
    }

    @Override
    public RTCRtpPacket interceptIncomingRTP(RTCRtpPacket packet, RTCRtpReceiver receiver) {
        // No-op for incoming packets in this example
        return packet;
    }

    @Override
    public RTCRtcpPacket interceptRTCP(RTCRtcpPacket packet) {
        return packet;
    }

    @Override
    public RTCRtcpPacket interceptIncomingRTCP(RTCRtcpPacket packet) {
        return packet;
    }
}
