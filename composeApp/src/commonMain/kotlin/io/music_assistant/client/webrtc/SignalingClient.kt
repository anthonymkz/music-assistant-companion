package io.music_assistant.client.webrtc

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.music_assistant.client.utils.myJson
import io.music_assistant.client.webrtc.model.SignalingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.coroutines.CoroutineContext

/**
 * WebSocket state for signaling server connection.
 */
sealed class SignalingState {
    /** Not connected */
    data object Disconnected : SignalingState()

    /** Connecting to signaling server */
    data object Connecting : SignalingState()

    /** Connected to signaling server */
    data object Connected : SignalingState()

    /** Connection error */
    data class Error(val error: Throwable) : SignalingState()
}

/**
 * WebSocket client for WebRTC signaling server.
 *
 * Connects to wss://signaling.music-assistant.io/ws and routes messages between
 * the client and Music Assistant gateway for WebRTC peer connection establishment.
 *
 * Flow:
 * 1. Client connects to signaling server
 * 2. Client sends Connect message with Remote ID
 * 3. Server responds with SessionReady (includes session ID and ICE servers)
 * 4. Client and gateway exchange SDP offers/answers and ICE candidates via server
 * 5. WebRTC peer connection established directly between client and gateway
 *
 * @param signalingUrl WebSocket URL of signaling server (default: production server)
 */
class SignalingClient(
    private val signalingUrl: String = DEFAULT_SIGNALING_URL
) : CoroutineScope {

    private val logger = Logger.withTag("SignalingClient")
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + supervisorJob

    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(myJson)
        }
    }

    private var session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null

    private val _connectionState = MutableStateFlow<SignalingState>(SignalingState.Disconnected)
    val connectionState: StateFlow<SignalingState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 50)
    val incomingMessages: SharedFlow<SignalingMessage> = _incomingMessages.asSharedFlow()

    /**
     * Connect to the signaling server.
     * Opens WebSocket connection and starts listening for messages.
     */
    suspend fun connect() {
        if (_connectionState.value is SignalingState.Connected ||
            _connectionState.value is SignalingState.Connecting
        ) {
            logger.w { "Already connected or connecting to signaling server" }
            return
        }

        logger.i { "Connecting to signaling server: $signalingUrl" }
        _connectionState.value = SignalingState.Connecting

        try {
            val wsSession = client.webSocketSession(signalingUrl)
            session = wsSession
            _connectionState.value = SignalingState.Connected
            logger.i { "Connected to signaling server" }

            // Start listening for incoming messages
            startReceiving()

        } catch (e: Exception) {
            logger.e(e) { "Failed to connect to signaling server" }
            _connectionState.value = SignalingState.Error(e)
            session = null
        }
    }

    /**
     * Send a signaling message to the server.
     *
     * @param message The message to send (will be serialized to JSON)
     */
    suspend fun sendMessage(message: SignalingMessage) {
        val currentSession = session
        if (currentSession == null) {
            logger.e { "Cannot send message: not connected to signaling server" }
            throw IllegalStateException("Not connected to signaling server")
        }

        try {
            val json = myJson.encodeToString(SignalingMessageSerializer, message)
            logger.d { "Sending signaling message: ${message.type}" }
            currentSession.send(Frame.Text(json))
        } catch (e: Exception) {
            logger.e(e) { "Failed to send signaling message" }
            throw e
        }
    }

    /**
     * Disconnect from the signaling server.
     */
    suspend fun disconnect() {
        logger.i { "Disconnecting from signaling server" }

        try {
            receiveJob?.cancel()
            receiveJob = null

            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
            session = null

            _connectionState.value = SignalingState.Disconnected
            logger.i { "Disconnected from signaling server" }
        } catch (e: Exception) {
            logger.e(e) { "Error during disconnect" }
        }
    }

    /**
     * Close the signaling client and cleanup resources.
     */
    fun close() {
        logger.i { "Closing signaling client" }
        receiveJob?.cancel()
        supervisorJob.cancel()
        client.close()
    }

    /**
     * Start listening for incoming messages from the signaling server.
     */
    private fun startReceiving() {
        receiveJob = launch {
            val currentSession = session ?: return@launch

            try {
                for (frame in currentSession.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            handleTextFrame(frame)
                        }
                        is Frame.Close -> {
                            logger.i { "Signaling server closed connection" }
                            _connectionState.value = SignalingState.Disconnected
                            break
                        }
                        else -> {
                            // Ignore binary frames, ping/pong
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    logger.e(e) { "Error receiving signaling messages" }
                    _connectionState.value = SignalingState.Error(e)
                }
            } finally {
                session = null
                if (_connectionState.value is SignalingState.Connected) {
                    _connectionState.value = SignalingState.Disconnected
                }
            }
        }
    }

    /**
     * Handle incoming text frame (JSON signaling message).
     */
    private suspend fun handleTextFrame(frame: Frame.Text) {
        try {
            val text = frame.readText()
            logger.d { "Received signaling message: ${text.take(100)}..." }

            val message = myJson.decodeFromString(SignalingMessageSerializer, text)
            logger.d { "Parsed signaling message type: ${message.type}" }

            _incomingMessages.emit(message)
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse signaling message" }
        }
    }

    companion object {
        /**
         * Default signaling server URL (Music Assistant production server).
         */
        const val DEFAULT_SIGNALING_URL = "wss://signaling.music-assistant.io/ws"
    }
}
