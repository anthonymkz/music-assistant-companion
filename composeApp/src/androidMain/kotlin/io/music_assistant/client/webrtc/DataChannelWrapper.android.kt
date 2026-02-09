package io.music_assistant.client.webrtc

import co.touchlab.kermit.Logger
import com.shepeliev.webrtckmp.DataChannel
import com.shepeliev.webrtckmp.DataChannelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation of DataChannelWrapper using webrtc-kmp library.
 */
actual class DataChannelWrapper(
    private val dataChannel: DataChannel
) {
    private val logger = Logger.withTag("DataChannelWrapper[Android]")
    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val closed = AtomicBoolean(false)

    actual val label: String
        get() = dataChannel.label

    private val _state = MutableStateFlow(dataChannel.readyState)
    actual val state: StateFlow<DataChannelState> = _state.asStateFlow()

    actual val messages: Flow<String> = dataChannel.onMessage.map { buffer -> buffer.decodeToString() }

    init {
        // Monitor state changes via flow events
        eventScope.launch {
            try {
                dataChannel.onOpen.collect { _state.update { DataChannelState.Open } }
            } catch (e: Exception) {
                logger.e(e) { "Error in onOpen flow" }
            }
        }
        eventScope.launch {
            try {
                dataChannel.onClosing.collect { _state.update { DataChannelState.Closing } }
            } catch (e: Exception) {
                logger.e(e) { "Error in onClosing flow" }
            }
        }
        eventScope.launch {
            try {
                dataChannel.onClose.collect { _state.update { DataChannelState.Closed } }
            } catch (e: Exception) {
                logger.e(e) { "Error in onClose flow" }
            }
        }
    }

    actual fun send(message: String) {
        val data = message.encodeToByteArray()

        // CRITICAL FIX: webrtc-kmp sends BINARY messages, but Music Assistant server expects TEXT
        // We bypass webrtc-kmp and use native Android WebRTC API to send as TEXT

        val buffer = org.webrtc.DataChannel.Buffer(
            ByteBuffer.wrap(data),
            false
        )
        dataChannel.android.send(buffer)
    }

    actual suspend fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        logger.i { "Closing data channel" }
        eventScope.cancel()
        dataChannel.close()
        _state.update { DataChannelState.Closed }
    }
}
