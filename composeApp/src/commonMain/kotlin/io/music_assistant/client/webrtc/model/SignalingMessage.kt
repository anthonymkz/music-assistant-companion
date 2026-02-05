package io.music_assistant.client.webrtc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Messages exchanged with the WebRTC signaling server.
 *
 * The signaling server (wss://signaling.music-assistant.io/ws) routes messages between
 * clients and Music Assistant gateways to establish WebRTC peer connections.
 */
@Serializable
sealed interface SignalingMessage {
    val type: String

    /**
     * Client → Signaling Server
     * Request connection to a Music Assistant server by Remote ID.
     */
    @Serializable
    @SerialName("connect")
    data class Connect(
        @SerialName("remoteId") val remoteId: String,
        override val type: String = "connect"
    ) : SignalingMessage

    /**
     * Signaling Server → Client
     * Confirms session is ready and provides ICE servers and session ID.
     */
    @Serializable
    @SerialName("session-ready")
    data class SessionReady(
        @SerialName("sessionId") val sessionId: String,
        @SerialName("iceServers") val iceServers: List<IceServer>,
        override val type: String = "session-ready"
    ) : SignalingMessage

    /**
     * Client → Signaling Server
     * SDP offer from client to gateway.
     */
    @Serializable
    @SerialName("offer")
    data class Offer(
        @SerialName("sessionId") val sessionId: String,
        @SerialName("data") val data: SessionDescription,
        override val type: String = "offer"
    ) : SignalingMessage

    /**
     * Signaling Server → Client
     * SDP answer from gateway to client.
     */
    @Serializable
    @SerialName("answer")
    data class Answer(
        @SerialName("sessionId") val sessionId: String,
        @SerialName("data") val data: SessionDescription,
        override val type: String = "answer"
    ) : SignalingMessage

    /**
     * Bidirectional: Client ↔ Signaling Server ↔ Gateway
     * ICE candidate exchange for NAT traversal.
     */
    @Serializable
    @SerialName("ice-candidate")
    data class IceCandidate(
        @SerialName("sessionId") val sessionId: String,
        @SerialName("data") val data: IceCandidateData,
        override val type: String = "ice-candidate"
    ) : SignalingMessage

    /**
     * Signaling Server → Client
     * Error during connection or signaling process.
     */
    @Serializable
    @SerialName("error")
    data class Error(
        @SerialName("error") val error: String,
        @SerialName("sessionId") val sessionId: String? = null,
        override val type: String = "error"
    ) : SignalingMessage

    /**
     * Signaling Server → Client
     * Notification that the gateway disconnected.
     */
    @Serializable
    @SerialName("client-disconnected")
    data class ClientDisconnected(
        @SerialName("sessionId") val sessionId: String,
        override val type: String = "client-disconnected"
    ) : SignalingMessage

    /**
     * Signaling Server → Client
     * Gateway registered successfully (for gateway use, clients shouldn't receive this).
     */
    @Serializable
    @SerialName("registered")
    data class Registered(
        @SerialName("remoteId") val remoteId: String,
        override val type: String = "registered"
    ) : SignalingMessage
}

/**
 * ICE server configuration for STUN/TURN.
 */
@Serializable
data class IceServer(
    @SerialName("urls") val urls: List<String>,
    @SerialName("username") val username: String? = null,
    @SerialName("credential") val credential: String? = null
) {
    constructor(url: String) : this(urls = listOf(url))
}

/**
 * Session Description Protocol (SDP) for WebRTC offer/answer.
 */
@Serializable
data class SessionDescription(
    @SerialName("sdp") val sdp: String,
    @SerialName("type") val type: String // "offer" or "answer"
)

/**
 * ICE candidate data for NAT traversal.
 *
 * Example candidate string:
 * "candidate:0 1 UDP 2113937151 192.168.1.100 51472 typ host"
 */
@Serializable
data class IceCandidateData(
    @SerialName("candidate") val candidate: String,
    @SerialName("sdpMid") val sdpMid: String?,
    @SerialName("sdpMLineIndex") val sdpMLineIndex: Int?
)
