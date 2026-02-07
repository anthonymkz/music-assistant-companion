package io.music_assistant.client.api

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * Abstraction for connection transport (WebSocket or WebRTC).
 *
 * Provides a uniform interface for sending/receiving JSON messages,
 * hiding the underlying transport details from ServiceClient.
 *
 * Lifecycle:
 * - WebSocket: Created inside ws {} block, closed when block exits
 * - WebRTC: Created by explicit connect(), closed by close()
 */
interface ConnectionSession {
    /**
     * Flow of incoming JSON messages from the server.
     * Collect from this flow to receive messages.
     */
    val messages: Flow<JsonObject>

    /**
     * Send a JSON message to the server.
     *
     * @param message JSON object to send
     */
    suspend fun send(message: JsonObject)

    /**
     * Close the connection and cleanup resources.
     * After calling close(), no more messages can be sent/received.
     */
    suspend fun close()
}
