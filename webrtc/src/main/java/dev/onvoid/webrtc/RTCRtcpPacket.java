package dev.onvoid.webrtc;

import java.nio.ByteBuffer;

public class RTCRtcpPacket {
    private ByteBuffer buffer;

    public RTCRtcpPacket(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
