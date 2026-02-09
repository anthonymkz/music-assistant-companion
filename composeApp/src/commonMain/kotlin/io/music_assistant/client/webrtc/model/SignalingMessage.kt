package io.music_assistant.client.webrtc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("connect-request")
    data class ConnectRequest(
        @SerialName("remoteId") val remoteId: String
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "connect-request"
    }

    /**
     * Signaling Server → Client
     * Connection accepted. Provides session ID, remote ID, and ICE servers.
     */
    @Serializable
    @SerialName("connected")
    data class Connected(
        @SerialName("sessionId") val sessionId: String? = null,
        @SerialName("remoteId") val remoteId: String? = null,
        @SerialName("iceServers") val iceServers: List<IceServer> = emptyList()
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "connected"
    }

    /**
     * Client → Signaling Server
     * SDP offer from client to gateway.
     */
    @Serializable
    @SerialName("offer")
    data class Offer(
        @SerialName("remoteId") val remoteId: String,
        @SerialName("sessionId") val sessionId: String,
        @SerialName("data") val data: SessionDescription
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "offer"
    }

    /**
     * Signaling Server → Client
     * SDP answer from gateway to client.
     */
    @Serializable
    @SerialName("answer")
    data class Answer(
        @SerialName("sessionId") val sessionId: String,
        @SerialName("data") val data: SessionDescription
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "answer"
    }

    /**
     * Bidirectional: Client ↔ Signaling Server ↔ Gateway
     * ICE candidate exchange for NAT traversal.
     * Note: remoteId is only present in outgoing messages (client → gateway).
     * Gateway responses omit remoteId and only include sessionId.
     */
    @Serializable
    @SerialName("ice-candidate")
    data class IceCandidate(
        @SerialName("remoteId") val remoteId: String? = null,
        @SerialName("sessionId") val sessionId: String,
        @SerialName("data") val data: IceCandidateData
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "ice-candidate"
    }

    /**
     * Signaling Server → Client
     * Error during connection or signaling process.
     */
    @Serializable
    @SerialName("error")
    data class Error(
        @SerialName("error") val error: String,
        @SerialName("sessionId") val sessionId: String? = null
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "error"
    }

    /**
     * Signaling Server → Client
     * Notification that the peer (gateway) disconnected.
     */
    @Serializable
    @SerialName("peer-disconnected")
    data class PeerDisconnected(
        @SerialName("sessionId") val sessionId: String? = null
    ) : SignalingMessage {
        @SerialName("type")
        override val type: String = "peer-disconnected"
    }

    /**
     * Signaling Server → Client
     * Keepalive ping. Client must respond with Pong to stay connected.
     */
    @Serializable
    @SerialName("ping")
    data class Ping(
        @SerialName("type")
        override val type: String = "ping"
    ) : SignalingMessage

    /**
     * Client → Signaling Server
     * Keepalive pong response.
     */
    @Serializable
    @SerialName("pong")
    data class Pong(
        @SerialName("type")
        override val type: String = "pong"
    ) : SignalingMessage

    /**
     * Unknown message type (forward compatibility).
     * Received when server sends a message type we don't recognize.
     * Allows client to continue operating when server protocol is extended.
     */
    @Serializable
    data class Unknown(
        override val type: String
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
)

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
