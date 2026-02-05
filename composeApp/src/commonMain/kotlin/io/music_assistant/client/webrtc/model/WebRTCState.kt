package io.music_assistant.client.webrtc.model

/**
 * Overall WebRTC connection state for UI display and logic.
 */
sealed class WebRTCConnectionState {
    /** Initial state, no connection attempt */
    data object Idle : WebRTCConnectionState()

    /** Connecting to signaling server */
    data object ConnectingToSignaling : WebRTCConnectionState()

    /** Negotiating WebRTC peer connection (exchanging SDP offers/answers) */
    data class NegotiatingPeerConnection(val sessionId: String) : WebRTCConnectionState()

    /** Gathering ICE candidates for NAT traversal */
    data class GatheringIceCandidates(val sessionId: String) : WebRTCConnectionState()

    /** WebRTC peer connection established and data channels open */
    data class Connected(
        val sessionId: String,
        val remoteId: RemoteId
    ) : WebRTCConnectionState()

    /** Connection failed or error occurred */
    data class Error(val error: WebRTCError) : WebRTCConnectionState()

    /** Disconnecting from WebRTC */
    data object Disconnecting : WebRTCConnectionState()
}

/**
 * Categorized errors for WebRTC connections.
 */
sealed class WebRTCError {
    /** Error connecting to or communicating with signaling server */
    data class SignalingError(val message: String, val cause: Throwable? = null) : WebRTCError()

    /** Remote ID not found or invalid */
    data class RemoteIdNotFound(val remoteId: RemoteId) : WebRTCError()

    /** Error during WebRTC peer connection establishment */
    data class PeerConnectionError(val message: String, val cause: Throwable? = null) : WebRTCError()

    /** ICE connection failed (couldn't find path through NAT) */
    data class IceConnectionFailed(val reason: String) : WebRTCError()

    /** Data channel error */
    data class DataChannelError(val message: String, val channel: String? = null) : WebRTCError()

    /** Generic connection error */
    data class ConnectionError(val message: String, val cause: Throwable? = null) : WebRTCError()
}

/**
 * RTCPeerConnection state.
 * Maps to WebRTC standard PeerConnectionState.
 */
enum class RTCPeerConnectionState {
    /** Initial state */
    NEW,

    /** Connection is being established */
    CONNECTING,

    /** Connection is active */
    CONNECTED,

    /** Connection was disconnected but might recover */
    DISCONNECTED,

    /** Connection failed and won't recover */
    FAILED,

    /** Connection was explicitly closed */
    CLOSED
}

/**
 * RTCIceConnectionState for tracking NAT traversal.
 * Maps to WebRTC standard IceConnectionState.
 */
enum class RTCIceConnectionState {
    /** Initial state, no ICE checks started */
    NEW,

    /** Gathering ICE candidates */
    CHECKING,

    /** At least one ICE candidate pair succeeded */
    CONNECTED,

    /** All ICE candidate pairs succeeded */
    COMPLETED,

    /** ICE connection failed */
    FAILED,

    /** ICE connection was disconnected */
    DISCONNECTED,

    /** ICE connection was closed */
    CLOSED
}

/**
 * RTCDataChannel state.
 * Maps to WebRTC standard DataChannelState.
 */
enum class RTCDataChannelState {
    /** Data channel is being established */
    CONNECTING,

    /** Data channel is open and ready for messages */
    OPEN,

    /** Data channel is closing */
    CLOSING,

    /** Data channel is closed */
    CLOSED
}

/**
 * ICE candidate type for diagnostics and monitoring.
 */
enum class IceCandidateType {
    /** Local network address (direct connection) */
    HOST,

    /** Server-reflexive address from STUN (through NAT) */
    SRFLX,

    /** Peer-reflexive address */
    PRFLX,

    /** Relayed address from TURN server */
    RELAY,

    /** Unknown or unparsed type */
    UNKNOWN
}
