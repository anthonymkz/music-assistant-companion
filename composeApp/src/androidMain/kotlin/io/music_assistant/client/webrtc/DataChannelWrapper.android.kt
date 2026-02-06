package io.music_assistant.client.webrtc

import co.touchlab.kermit.Logger
import com.shepeliev.webrtckmp.DataChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _state = MutableStateFlow(dataChannel.readyState.toStateString())
    actual val state: StateFlow<String> = _state.asStateFlow()

    init {
        try {
            // Monitor state changes via flow events
            eventScope.launch {
                try {
                    dataChannel.onOpen.collect {
                        _state.value = "open"
                        logger.d { "Data channel opened" }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Data channel onOpen flow failed" }
                }
            }
            eventScope.launch {
                try {
                    dataChannel.onClosing.collect {
                        _state.value = "closing"
                        logger.d { "Data channel closing" }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Data channel onClosing flow failed" }
                }
            }
            eventScope.launch {
                try {
                    dataChannel.onClose.collect {
                        _state.value = "closed"
                        logger.d { "Data channel closed" }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Data channel onClose flow failed" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to initialize data channel wrapper" }
            eventScope.cancel()
            throw e
        }
    }

    actual fun send(message: String) {
        try {
            val data = message.encodeToByteArray()
            val success = dataChannel.send(data)
            if (success) {
                logger.d { "Sent message (${message.length} chars)" }
            } else {
                logger.w { "Send returned false" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to send message" }
        }
    }

    actual fun onMessage(callback: (String) -> Unit) {
        eventScope.launch {
            try {
                dataChannel.onMessage.collect { buffer ->
                    val message = buffer.decodeToString()
                    callback(message)
                }
            } catch (e: Exception) {
                logger.e(e) { "Data channel onMessage flow failed" }
            }
        }
    }

    actual suspend fun close() {
        if (!closed.compareAndSet(false, true)) {
            logger.d { "Data channel already closed" }
            return
        }
        logger.i { "Closing data channel" }
        eventScope.cancel()
        dataChannel.close()
        _state.value = "closed"
    }

    private fun com.shepeliev.webrtckmp.DataChannelState.toStateString(): String {
        return when (this) {
            com.shepeliev.webrtckmp.DataChannelState.Connecting -> "connecting"
            com.shepeliev.webrtckmp.DataChannelState.Open -> "open"
            com.shepeliev.webrtckmp.DataChannelState.Closing -> "closing"
            com.shepeliev.webrtckmp.DataChannelState.Closed -> "closed"
        }
    }
}
