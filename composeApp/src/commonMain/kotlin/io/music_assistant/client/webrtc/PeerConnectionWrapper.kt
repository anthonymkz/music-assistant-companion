package io.music_assistant.client.webrtc

import io.music_assistant.client.webrtc.model.IceCandidateData
import io.music_assistant.client.webrtc.model.IceServer
import io.music_assistant.client.webrtc.model.SessionDescription

/**
 * Platform-specific WebRTC peer connection wrapper.
 *
 * Abstracts webrtc-kmp library's expect/actual PeerConnection class.
 * This wrapper provides a common interface for WebRTCConnectionManager
 * while delegating to platform-specific WebRTC implementations.
 *
 * Lifecycle:
 * 1. Create instance with callbacks
 * 2. Call initialize(iceServers) to set up connection
 * 3. Call createOffer() to generate SDP offer
 * 4. Exchange offer/answer via signaling
 * 5. Call setRemoteAnswer() with server's SDP answer
 * 6. As ICE candidates arrive, call addIceCandidate()
 * 7. Server creates data channel → onDataChannel callback fires
 * 8. Connection established
 * 9. Call close() when done
 */
expect class PeerConnectionWrapper(
    onIceCandidate: (IceCandidateData) -> Unit,
    onDataChannel: (DataChannelWrapper) -> Unit,
    onConnectionStateChange: (state: String) -> Unit
) {
    /**
     * Initialize peer connection with ICE servers from signaling server.
     * Must be called before createOffer().
     *
     * @param iceServers STUN/TURN servers for NAT traversal
     */
    suspend fun initialize(iceServers: List<IceServer>)

    /**
     * Create SDP offer to send to remote peer via signaling.
     *
     * @return SessionDescription with type "offer" and SDP string
     */
    suspend fun createOffer(): SessionDescription

    /**
     * Set remote peer's SDP answer received via signaling.
     *
     * @param answer SessionDescription from remote peer
     */
    suspend fun setRemoteAnswer(answer: SessionDescription)

    /**
     * Add ICE candidate received from remote peer via signaling.
     * Called multiple times as candidates are discovered.
     *
     * @param candidate ICE candidate data
     */
    suspend fun addIceCandidate(candidate: IceCandidateData)

    /**
     * Create outgoing data channel.
     * Note: For Music Assistant, server creates the channel, so this is rarely used.
     *
     * @param label Channel label (e.g., "ma-api", "sendspin")
     * @return DataChannelWrapper for the created channel
     */
    fun createDataChannel(label: String): DataChannelWrapper

    /**
     * Close peer connection and cleanup resources.
     */
    suspend fun close()
}
