package io.music_assistant.client.webrtc

import com.shepeliev.webrtckmp.DataChannelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS implementation of DataChannelWrapper.
 *
 * TODO: Implement using webrtc-kmp iOS support
 */
actual class DataChannelWrapper {
    private val _state = MutableStateFlow(DataChannelState.Closed)

    actual val label: String
        get() = throw NotImplementedError("iOS WebRTC support not yet implemented")

    actual val state: StateFlow<DataChannelState> = _state.asStateFlow()

    actual val messages: Flow<String> = emptyFlow()

    actual fun send(message: String) {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }

    actual suspend fun close() {
        // No-op for now
    }

    actual val binaryMessages: Flow<ByteArray>
        get() = throw NotImplementedError("iOS WebRTC support not yet implemented")

    actual fun sendBinary(data: ByteArray) {
        throw NotImplementedError("iOS WebRTC support not yet implemented")
    }
}
