package io.music_assistant.client.webrtc

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific WebRTC data channel wrapper.
 *
 * Wraps webrtc-kmp's DataChannel for sending/receiving messages over WebRTC connection.
 * Data channels are the "pipes" that carry application data (MA API messages, Sendspin audio)
 * through the encrypted WebRTC peer connection.
 *
 * Channel States:
 * - "connecting" - Channel is being established
 * - "open" - Channel is ready, can send/receive
 * - "closing" - Channel is shutting down
 * - "closed" - Channel is closed
 *
 * Usage:
 * ```kotlin
 * // Wait for channel to open
 * dataChannel.state.collect { state ->
 *     if (state == "open") {
 *         dataChannel.send("""{"type":"command","data":{...}}""")
 *     }
 * }
 *
 * // Receive messages
 * dataChannel.onMessage { message ->
 *     println("Received: $message")
 * }
 * ```
 */
expect class DataChannelWrapper {
    /**
     * Channel label (e.g., "ma-api", "sendspin").
     * Identifies the purpose of this data channel.
     */
    val label: String

    /**
     * Current state of the data channel.
     * Values: "connecting", "open", "closing", "closed"
     */
    val state: StateFlow<String>

    /**
     * Send text message over the data channel.
     * Channel must be in "open" state.
     *
     * @param message Text message to send (typically JSON for MA API)
     */
    fun send(message: String)

    /**
     * Register callback for incoming messages.
     * Called when remote peer sends data over this channel.
     *
     * @param callback Function to handle received messages
     */
    fun onMessage(callback: (String) -> Unit)

    /**
     * Close the data channel.
     */
    suspend fun close()
}
