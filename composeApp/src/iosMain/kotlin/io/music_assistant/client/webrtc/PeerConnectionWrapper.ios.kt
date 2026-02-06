package io.music_assistant.client.webrtc

import co.touchlab.kermit.Logger
import io.music_assistant.client.webrtc.model.IceCandidateData
import io.music_assistant.client.webrtc.model.IceServer
import io.music_assistant.client.webrtc.model.SessionDescription

/**
 * iOS implementation of PeerConnectionWrapper.
 *
 * TODO: Implement using webrtc-kmp iOS support
 */
actual class PeerConnectionWrapper actual constructor(
    private val onIceCandidate: (IceCandidateData) -> Unit,
    private val onDataChannel: (DataChannelWrapper) -> Unit,
    private val onConnectionStateChange: (state: String) -> Unit
) {
    private val logger = Logger.withTag("PeerConnectionWrapper[iOS]")

    actual suspend fun initialize(iceServers: List<IceServer>) {
        logger.w { "iOS WebRTC not yet implemented" }
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual suspend fun createOffer(): SessionDescription {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual suspend fun setRemoteAnswer(answer: SessionDescription) {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual suspend fun addIceCandidate(candidate: IceCandidateData) {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual fun createDataChannel(label: String): DataChannelWrapper {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual suspend fun close() {
        // No-op for now
    }
}
