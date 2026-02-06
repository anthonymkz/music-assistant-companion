package io.music_assistant.client.webrtc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS implementation of DataChannelWrapper.
 *
 * TODO: Implement using webrtc-kmp iOS support
 */
actual class DataChannelWrapper {
    private val _state = MutableStateFlow("closed")

    actual val label: String
        get() = throw NotImplementedError("iOS WebRTC support not yet implemented")

    actual val state: StateFlow<String> = _state.asStateFlow()

    actual fun send(message: String) {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual fun onMessage(callback: (String) -> Unit) {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual suspend fun close() {
        // No-op for now
    }
}
