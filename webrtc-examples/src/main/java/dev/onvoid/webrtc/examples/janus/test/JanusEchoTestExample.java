package dev.onvoid.webrtc.examples.janus.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * JanusEchoTestExample
 * 
 * This example connects to the Janus EchoTest plugin via WebSockets
 * (wss://janus.conf.meetecho.com/ws) and demonstrates the Audio Interceptor
 * feature.
 * 
 * It performs the following steps:
 * 1. Connects to Janus via WebSocket.
 * 2. Creates a Janus session.
 * 3. Attaches to the janus.plugin.echotest plugin.
 * 4. Creates a WebRTC PeerConnection with an Audio Interceptor.
 * 5. Negotiates a Send/Recv audio stream.
 * 6. Logs intercepted packets.
 */
public class JanusEchoTestExample {

    private static final String JANUS_URL = "wss://janus.conf.meetecho.com/ws";
    private static final String ECHO_PLUGIN = "janus.plugin.echotest";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CompletableFuture<JsonNode>> transactions = new ConcurrentHashMap<>();

    private WebSocket webSocket;
    private long sessionId;
    private long handleId;

    private PeerConnectionFactory factory;
    private RTCPeerConnection peerConnection;

    public static void main(String[] args) throws Exception {
        new JanusEchoTestExample().run();
    }

    public void run() throws Exception {
        System.out.println("Starting Janus Echo Test Example...");

        // 1. Connect to WebSocket
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.webSocket = client.newWebSocketBuilder()
                .subprotocols("janus-protocol")
                .buildAsync(URI.create(JANUS_URL), new JanusWebSocketListener())
                .get();

        System.out.println("Connected to Janus.");

        // 2. Create Janus Session
        JsonNode sessionResponse = sendJanusRequest("create", null);
        this.sessionId = sessionResponse.get("data").get("id").asLong();
        System.out.println("Session created: " + sessionId);

        // 3. Attach to EchoTest Plugin
        Map<String, Object> attachData = Map.of("plugin", ECHO_PLUGIN);
        JsonNode attachResponse = sendJanusRequest("attach", attachData);
        this.handleId = attachResponse.get("data").get("id").asLong();
        System.out.println("Plugin attached: " + handleId);

        // 4. Initialize WebRTC
        this.factory = new PeerConnectionFactory();

        RTCConfiguration config = new RTCConfiguration();
        // Janus usually doesn't strictly need STUN for echo tests if public, but good
        // practice.
        // RTCIceServer stun = new RTCIceServer();
        // stun.urls.add("stun:stun.l.google.com:19302");
        // config.iceServers.add(stun);

        this.peerConnection = factory.createPeerConnection(config, new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(RTCIceCandidate candidate) {
                // Send trickle ICE to Janus
                sendTrickleCandidate(candidate);
            }

            @Override
            public void onIceConnectionChange(RTCIceConnectionState state) {
                System.out.println("ICE Connection State: " + state);
            }
        });

        // 5. Add Audio Interceptor
        peerConnection.getInterceptorRegistry().add(new Interceptor() {
            @Override
            public RTCRtpPacket interceptRTP(RTCRtpPacket packet, RTCRtpSender sender) {
                // System.out.println("[INTERCEPT] Outgoing Seq: " +
                // packet.getHeader().sequenceNumber);
                return packet;
            }

            @Override
            public RTCRtpPacket interceptIncomingRTP(RTCRtpPacket packet, RTCRtpReceiver receiver) {
                System.out.println("[INTERCEPT] Incoming Echo Seq: " + packet.getHeader().sequenceNumber);
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
        });

        // 6. Add Audio Track (Transceiver)
        RTCRtpTransceiverInit init = new RTCRtpTransceiverInit();
        init.direction = RTCRtpTransceiverDirection.SEND_RECV;
        peerConnection.addTransceiver(MediaType.AUDIO, init); // Use MediaType.AUDIO

        // 7. Create Offer and Send to Janus
        peerConnection.createOffer(new RTCOfferOptions(), new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        sendOfferToJanus(description);
                    }

                    @Override
                    public void onFailure(String error) {
                        System.err.println("SetLocalDescription failed: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                System.err.println("CreateOffer failed: " + error);
            }
        });

        // Keep running
        System.out.println("WebRTC Negotiation started. Press Enter to exit.");
        System.in.read();
    }

    private void sendOfferToJanus(RTCSessionDescription offer) {
        Map<String, Object> jsep = Map.of(
                "type", offer.sdpType.toString().toLowerCase(),
                "sdp", offer.sdp);
        Map<String, Object> body = Map.of("audio", true, "video", false); // Audio Only

        Map<String, Object> msg = Map.of(
                "janus", "message",
                "session_id", sessionId,
                "handle_id", handleId,
                "transaction", UUID.randomUUID().toString(),
                "body", body,
                "jsep", jsep);

        try {
            webSocket.sendText(mapper.writeValueAsString(msg), true);
            System.out.println("Offer sent to Janus.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTrickleCandidate(RTCIceCandidate candidate) {
        Map<String, Object> candidateMap = Map.of(
                "candidate", candidate.sdp,
                "sdpMid", candidate.sdpMid,
                "sdpMLineIndex", candidate.sdpMLineIndex);
        Map<String, Object> msg = Map.of(
                "janus", "trickle",
                "session_id", sessionId,
                "handle_id", handleId,
                "transaction", UUID.randomUUID().toString(),
                "candidate", candidateMap);
        try {
            webSocket.sendText(mapper.writeValueAsString(msg), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JsonNode sendJanusRequest(String method, Object data) throws Exception {
        String transaction = UUID.randomUUID().toString();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        transactions.put(transaction, future);

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("janus", method);
        request.put("transaction", transaction);
        if (sessionId != 0)
            request.put("session_id", sessionId);
        if (handleId != 0)
            request.put("handle_id", handleId);
        if (data instanceof Map)
            request.putAll((Map) data);

        webSocket.sendText(mapper.writeValueAsString(request), true);

        return future.get(10, TimeUnit.SECONDS);
    }

    private class JanusWebSocketListener implements WebSocket.Listener {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            builder.append(data);
            if (last) {
                String fullMessage = builder.toString();
                builder.setLength(0);
                handleJanusMessage(fullMessage);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        private void handleJanusMessage(String message) {
            try {
                JsonNode node = mapper.readTree(message);
                String janus = node.get("janus").asText();
                String transaction = node.has("transaction") ? node.get("transaction").asText() : null;

                if (transaction != null && transactions.containsKey(transaction)) {
                    transactions.remove(transaction).complete(node);
                } else if ("event".equals(janus) && node.has("jsep")) {
                    handleRemoteAnswer(node.get("jsep"));
                } else if ("keepalive".equals(janus)) {
                    // Ignore
                } else {
                    System.out.println("Janus Event/Message: " + janus);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleRemoteAnswer(JsonNode jsep) {
            String type = jsep.get("type").asText();
            String sdp = jsep.get("sdp").asText();
            RTCSdpType sdpType = RTCSdpType.valueOf(type.toUpperCase());
            RTCSessionDescription description = new RTCSessionDescription(sdpType, sdp);

            peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
                @Override
                public void onSuccess() {
                    System.out.println("WebRTC Connected (Remote Answer set)");
                }

                @Override
                public void onFailure(String error) {
                    System.err.println("SetRemoteDescription failed: " + error);
                }
            });
        }
    }
}
