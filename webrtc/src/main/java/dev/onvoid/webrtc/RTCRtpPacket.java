package dev.onvoid.webrtc;

import java.nio.ByteBuffer;
import java.util.List;

public class RTCRtpPacket {
    private RTCRtpHeader header;
    private ByteBuffer payload;
    private List<RTCRtpHeaderExtensionParameters> extensions;

    public RTCRtpPacket(RTCRtpHeader header, ByteBuffer payload, List<RTCRtpHeaderExtensionParameters> extensions) {
        this.header = header;
        this.payload = payload;
        this.extensions = extensions;
    }

    public RTCRtpHeader getHeader() {
        return header;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    public List<RTCRtpHeaderExtensionParameters> getExtensions() {
        return extensions;
    }
}
