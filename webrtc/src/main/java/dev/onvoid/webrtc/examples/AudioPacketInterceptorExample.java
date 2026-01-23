package dev.onvoid.webrtc.examples;

import dev.onvoid.webrtc.*;
import java.nio.ByteBuffer;

/**
 * AudioPacketInterceptorExample
 * 
 * This example demonstrates how to use the RTP/RTCP Interceptor Framework
 * specifically
 * for audio streams in the WebRTC-Java project.
 * 
 * The interceptor allows you to:
 * 1. Monitor every single audio packet (RTP and RTCP) passing through.
 * 2. Modify the payload or headers of the audio packets in real-time.
 * 3. Drop packets selectively based on custom logic.
 * 
 * Interceptors are called on high-performance native threads. It is CRITICAL to
 * avoid blocking or heavy computations inside these methods to prevent audio
 * stuttering.
 */
public class AudioPacketInterceptorExample {

    public static void main(String[] args) {
        // 1. Create a PeerConnectionFactory.
        // Note: The interceptor mechanism is automatically enabled by default in
        // the modified PeerConnectionFactory constructors.
        PeerConnectionFactory factory = new PeerConnectionFactory();

        // 2. Define standard PeerConnection configuration and observer.
        RTCConfiguration config = new RTCConfiguration();
        PeerConnectionObserver observer = new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(RTCIceCandidate candidate) {
                System.out.println("New ICE Candidate: " + candidate.sdp);
            }
            // Implement other necessary callbacks if needed
        };

        // 3. Create the PeerConnection.
        RTCPeerConnection peerConnection = factory.createPeerConnection(config, observer);

        // 4. Access the InterceptorRegistry and add your custom interceptors.
        // Multiple interceptors can be added; they will be called in a chain.
        peerConnection.getInterceptorRegistry().add(new AudioLoggingInterceptor());
        peerConnection.getInterceptorRegistry().add(new AudioModificationInterceptor());

        System.out.println("PeerConnection created with Audio Interceptors registered.");
    }

    /**
     * A simple interceptor that logs outgoing audio RTP packet metadata.
     */
    static class AudioLoggingInterceptor implements Interceptor {

        @Override
        public RTCRtpPacket interceptRTP(RTCRtpPacket packet, RTCRtpSender sender) {
            RTCRtpHeader header = packet.getHeader();

            // Accessing RTP header fields: Payload Type, Seq Num, Timestamp, SSRC.
            System.out.printf("[OUTGOING] Audio RTP | SSRC: %d | Seq: %d | TS: %d%n",
                    header.ssrc, header.sequenceNumber, header.timestamp);

            // Accessing the payload size via ByteBuffer
            ByteBuffer payload = packet.getPayload();
            System.out.println("  Payload size: " + payload.remaining() + " bytes");

            // Return the packet to continue the chain
            return packet;
        }

        @Override
        public RTCRtpPacket interceptIncomingRTP(RTCRtpPacket packet, RTCRtpReceiver receiver) {
            // Monitor incoming audio from the remote peer.
            RTCRtpHeader header = packet.getHeader();
            System.out.printf("[INCOMING] Audio RTP | Seq: %d%n", header.sequenceNumber);
            return packet;
        }

        @Override
        public RTCRtcpPacket interceptRTCP(RTCRtcpPacket packet) {
            // Monitor outgoing RTCP packets (Sender Reports, etc.)
            System.out.println("[OUTGOING] Audio RTCP Packet intercepted.");
            return packet;
        }

        @Override
        public RTCRtcpPacket interceptIncomingRTCP(RTCRtcpPacket packet) {
            // Monitor incoming RTCP packets (Receiver Reports, Bye, etc.)
            System.out.println("[INCOMING] Audio RTCP Packet intercepted.");
            return packet;
        }
    }

    /**
     * An interceptor that demonstrates conditional packet modification.
     */
    static class AudioModificationInterceptor implements Interceptor {

        @Override
        public RTCRtpPacket interceptRTP(RTCRtpPacket packet, RTCRtpSender sender) {
            // Example: Drop every 100th packet to simulate packet loss for testing jitter
            // buffers.
            if (packet.getHeader().sequenceNumber % 100 == 0) {
                System.out.println("!!! Simulate loss: Dropping packet " + packet.getHeader().sequenceNumber);
                return null; // Returning null drops the packet in the native pipeline.
            }

            // Example: Modify payload (Advanced)
            // ByteBuffer payload = packet.getPayload();
            // In a real scenario, you could process the raw audio data here (e.g., gain
            // adjustment, custom encryption).

            return packet;
        }

        @Override
        public RTCRtpPacket interceptIncomingRTP(RTCRtpPacket packet, RTCRtpReceiver receiver) {
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
}
