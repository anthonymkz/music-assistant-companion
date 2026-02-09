package io.music_assistant.client.webrtc

import com.shepeliev.webrtckmp.DataChannelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific WebRTC data channel wrapper.
 *
 * Wraps webrtc-kmp's DataChannel for sending/receiving messages over WebRTC connection.
 * Data channels are the "pipes" that carry application data (MA API messages, Sendspin audio)
 * through the encrypted WebRTC peer connection.
 *
 * Channel States:
 * - DataChannelState.Connecting - Channel is being established
 * - DataChannelState.Open - Channel is ready, can send/receive
 * - DataChannelState.Closing - Channel is shutting down
 * - DataChannelState.Closed - Channel is closed
 *
 * Usage:
 * ```kotlin
 * // Wait for channel to open
 * dataChannel.state.collect { state ->
 *     if (state == DataChannelState.Open) {
 *         dataChannel.send("""{"type":"command","data":{...}}""")
 *     }
 * }
 *
 * // Receive messages (with error handling)
 * dataChannel.messages
 *     .catch { e -> logger.error(e) { "Error receiving messages" } }
 *     .collect { message -> println("Received: $message") }
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
     * Values: DataChannelState.Connecting, Open, Closing, Closed
     */
    val state: StateFlow<DataChannelState>

    /**
     * Incoming text messages from remote peer.
     * Flow emits each received text message as a String.
     * Use .catch operator for error handling.
     */
    val messages: Flow<String>

    /**
     * Incoming binary messages from remote peer.
     * Flow emits each received binary message as a ByteArray.
     * Use .catch operator for error handling.
     */
    val binaryMessages: Flow<ByteArray>

    /**
     * Send text message over the data channel.
     * Channel must be in "open" state.
     *
     * @param message Text message to send (typically JSON for MA API)
     */
    fun send(message: String)

    /**
     * Send binary data over the data channel.
     * Channel must be in "open" state.
     *
     * @param data Binary data to send (typically audio chunks for Sendspin)
     */
    fun sendBinary(data: ByteArray)

    /**
     * Close the data channel.
     */
    suspend fun close()
}
