package dev.onvoid.webrtc;

import java.util.List;

public class RTCRtpHeader {
    public int payloadType;
    public int sequenceNumber;
    public long timestamp;
    public long ssrc;
    public List<Long> csrcs;

    public RTCRtpHeader() {
    }

    public RTCRtpHeader(int payloadType, int sequenceNumber, long timestamp, long ssrc, List<Long> csrcs) {
        this.payloadType = payloadType;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.ssrc = ssrc;
        this.csrcs = csrcs;
    }
}
