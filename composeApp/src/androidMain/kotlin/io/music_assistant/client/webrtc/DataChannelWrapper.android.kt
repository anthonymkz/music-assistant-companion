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
        val initialState = dataChannel.readyState.toStateString()
        logger.e { "🔵 DataChannelWrapper INIT for '$label', initial state: $initialState, thread: ${Thread.currentThread().name}" }

        try {
            // Monitor state changes via flow events
            eventScope.launch {
                try {
                    logger.e { "🔵 Starting onOpen flow collector for '$label'" }
                    dataChannel.onOpen.collect {
                        _state.value = "open"
                        logger.e { "🟢 Data channel OPENED: $label, thread: ${Thread.currentThread().name}" }
                    }
                    logger.e { "🟡 onOpen flow COMPLETED for '$label'" }
                } catch (e: Exception) {
                    logger.e(e) { "🔴 Data channel onOpen flow FAILED for '$label'" }
                }
            }
            eventScope.launch {
                try {
                    logger.e { "🔵 Starting onClosing flow collector for '$label'" }
                    dataChannel.onClosing.collect {
                        _state.value = "closing"
                        logger.e { "🟡 Data channel CLOSING: $label, thread: ${Thread.currentThread().name}" }
                    }
                    logger.e { "🟡 onClosing flow COMPLETED for '$label'" }
                } catch (e: Exception) {
                    logger.e(e) { "🔴 Data channel onClosing flow FAILED for '$label'" }
                }
            }
            eventScope.launch {
                try {
                    logger.e { "🔵 Starting onClose flow collector for '$label'" }
                    dataChannel.onClose.collect {
                        _state.value = "closed"
                        logger.e { "🔴 Data channel CLOSED: $label, thread: ${Thread.currentThread().name}" }
                    }
                    logger.e { "🟡 onClose flow COMPLETED for '$label'" }
                } catch (e: Exception) {
                    logger.e(e) { "🔴 Data channel onClose flow FAILED for '$label'" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "🔴 Failed to initialize data channel wrapper for '$label'" }
            eventScope.cancel()
            throw e
        }
    }

    actual fun send(message: String) {
        try {
            val data = message.encodeToByteArray()
            logger.e { "🔵 Native send() START - ${message.length} bytes, thread: ${Thread.currentThread().name}" }
            val beforeSend = System.currentTimeMillis()

            // CRITICAL FIX: webrtc-kmp sends BINARY messages, but Music Assistant server expects TEXT
            // We bypass webrtc-kmp and use native Android WebRTC API to send as TEXT
            val success = try {
                val nativeChannel = getNativeDataChannel()

                // Create TEXT message buffer (binary=false means TEXT)
                val buffer = org.webrtc.DataChannel.Buffer(
                    java.nio.ByteBuffer.wrap(data),
                    false  // binary=false → TEXT message (not binary)
                )

                logger.e { "🔍 Sending via native DataChannel as TEXT message (bypassing webrtc-kmp)" }
                nativeChannel.send(buffer)
            } catch (e: Exception) {
                logger.e(e) { "⚠️ Failed to access native channel, falling back to webrtc-kmp binary send" }
                dataChannel.send(data)  // Fallback to original binary send
            }

            val afterSend = System.currentTimeMillis()

            if (success) {
                logger.e { "🟢 Native send() SUCCESS in ${afterSend - beforeSend}ms, thread: ${Thread.currentThread().name}" }
            } else {
                logger.e { "🔴 Native send() returned FALSE!" }
            }
        } catch (e: Exception) {
            logger.e(e) { "🔴 Native send() EXCEPTION: ${e.message}" }
        }
    }

    /**
     * Access the underlying native org.webrtc.DataChannel from webrtc-kmp wrapper.
     * Uses reflection since webrtc-kmp doesn't expose it.
     */
    private fun getNativeDataChannel(): org.webrtc.DataChannel {
        val clazz = dataChannel::class.java

        // Try common field names first
        val possibleNames = listOf(
            "nativeDataChannel",
            "nativeChannel",
            "rtcDataChannel",
            "channel",
            "peer"
        )

        for (name in possibleNames) {
            try {
                val field = clazz.getDeclaredField(name)
                field.isAccessible = true
                val value = field.get(dataChannel)
                if (value is org.webrtc.DataChannel) {
                    logger.e { "🔍 Found native DataChannel via reflection: field='$name'" }
                    return value
                }
            } catch (_: NoSuchFieldException) {
                continue
            } catch (_: ClassCastException) {
                continue
            }
        }

        // If not found, brute force all fields
        for (field in clazz.declaredFields) {
            try {
                field.isAccessible = true
                val value = field.get(dataChannel)
                if (value is org.webrtc.DataChannel) {
                    logger.e { "🔍 Found native DataChannel via brute force: field='${field.name}'" }
                    return value
                }
            } catch (_: Exception) {
                continue
            }
        }

        throw IllegalStateException("Could not find native org.webrtc.DataChannel in webrtc-kmp wrapper")
    }

    actual fun onMessage(callback: (String) -> Unit) {
        val threadName = Thread.currentThread().name
        logger.e { "🔵 Registering onMessage callback for channel: $label, thread: $threadName" }

        eventScope.launch {
            val collectorThread = Thread.currentThread().name
            logger.e { "🔵 Flow collector STARTED for channel: $label, thread: $collectorThread, scope: eventScope" }

            try {
                var messageCount = 0
                var lastMessageTime = System.currentTimeMillis()

                logger.e { "🔵 About to call dataChannel.onMessage.collect() - this will block until messages arrive" }

                // Launch heartbeat to prove flow is alive
                val heartbeatJob = eventScope.launch {
                    var heartbeatCount = 0
                    while (true) {
                        kotlinx.coroutines.delay(5000) // Every 5 seconds
                        heartbeatCount++
                        logger.e { "💓 HEARTBEAT #$heartbeatCount - Flow is ALIVE and waiting for messages, channel: $label, messages received so far: $messageCount" }
                    }
                }

                try {
                    dataChannel.onMessage.collect { buffer ->
                        val receiveThread = Thread.currentThread().name
                        val now = System.currentTimeMillis()
                        val timeSinceLastMessage = now - lastMessageTime
                        lastMessageTime = now

                        messageCount++
                        logger.e { "🟢 RAW BUFFER #$messageCount RECEIVED! Size: ${buffer.size} bytes, channel: $label, thread: $receiveThread, time since last: ${timeSinceLastMessage}ms" }

                        val message = try {
                            buffer.decodeToString()
                        } catch (e: Exception) {
                            logger.e(e) { "🔴 Failed to decode buffer #$messageCount" }
                            throw e
                        }

                        logger.e { "🟢 Decoded message #$messageCount: ${message.take(200)}..." }

                        try {
                            val callbackThread = Thread.currentThread().name
                            logger.e { "🔵 Invoking callback for message #$messageCount on thread: $callbackThread" }
                            callback(message)
                            logger.e { "🟢 Callback invoked successfully for message #$messageCount" }
                        } catch (e: Exception) {
                            logger.e(e) { "🔴 Callback failed for message #$messageCount" }
                            throw e
                        }
                    }

                    logger.e { "🟡 onMessage flow COMPLETED NORMALLY - no more messages will arrive! Total received: $messageCount, thread: ${Thread.currentThread().name}" }
                } finally {
                    heartbeatJob.cancel()
                    logger.e { "💓 HEARTBEAT stopped for channel: $label" }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                logger.e { "🟡 onMessage flow CANCELLED on thread: ${Thread.currentThread().name}" }
                throw e
            } catch (e: Exception) {
                logger.e(e) { "🔴 onMessage flow FAILED with exception: ${e::class.simpleName} - ${e.message}, thread: ${Thread.currentThread().name}" }
                logger.e { "🔴 Stack trace: ${e.stackTraceToString()}" }
                throw e
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
