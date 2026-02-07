package io.music_assistant.client.api

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.music_assistant.client.webrtc.model.RemoteId

/**
 * Connection mode for Music Assistant server.
 *
 * Supports two connection types:
 * - Direct: WebSocket connection to local/remote server (host:port)
 * - WebRTC: Peer-to-peer connection via signaling server (Remote ID)
 */
sealed interface ConnectionMode {
    /**
     * Direct WebSocket connection to Music Assistant server.
     *
     * @param host Server hostname or IP address
     * @param port Server port
     * @param isTls Whether to use TLS (wss://) or not (ws://)
     */
    data class Direct(
        val host: String,
        val port: Int,
        val isTls: Boolean,
    ) : ConnectionMode {
        val webUrl: String = URLBuilder(
            protocol = if (isTls) URLProtocol.HTTPS else URLProtocol.HTTP,
            host = host,
            port = port,
        ).buildString()
    }

    /**
     * WebRTC peer-to-peer connection via signaling server.
     *
     * @param remoteId Remote ID of the Music Assistant server (26-char alphanumeric)
     */
    data class WebRTC(
        val remoteId: RemoteId,
    ) : ConnectionMode
}
