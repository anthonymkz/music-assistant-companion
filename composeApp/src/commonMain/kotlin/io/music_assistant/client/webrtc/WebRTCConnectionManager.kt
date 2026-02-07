package io.music_assistant.client.webrtc

import co.touchlab.kermit.Logger
import io.music_assistant.client.webrtc.model.RemoteId
import io.music_assistant.client.webrtc.model.SignalingMessage
import io.music_assistant.client.webrtc.model.WebRTCConnectionState
import io.music_assistant.client.webrtc.model.WebRTCError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages WebRTC connection lifecycle and orchestrates signaling + peer connection.
 *
 * This is the main entry point for WebRTC connections. It coordinates:
 * - SignalingClient: WebSocket connection to signaling server
 * - PeerConnectionWrapper: Native WebRTC peer connection
 * - Data channel management for MA API communication
 *
 * Connection Flow:
 * 1. connect(remoteId) called
 * 2. Connect to signaling server
 * 3. Send Connect message with Remote ID
 * 4. Receive SessionReady (ICE servers, session ID)
 * 5. Initialize peer connection with ICE servers
 * 6. Create SDP offer
 * 7. Send Offer to signaling server
 * 8. Receive Answer from server
 * 9. Set remote answer in peer connection
 * 10. Exchange ICE candidates
 * 11. Data channel "ma-api" opened by server
 * 12. Connected!
 *
 * Usage:
 * ```kotlin
 * val manager = WebRTCConnectionManager(signalingClient, scope)
 *
 * // Observe state
 * manager.connectionState.collect { state ->
 *     when (state) {
 *         is WebRTCConnectionState.Connected -> println("Connected!")
 *         is WebRTCConnectionState.Error -> println("Error: ${state.error}")
 *     }
 * }
 *
 * // Connect
 * manager.connect(RemoteId.parse("PGSVXKGZ-JCFA6-MOH4U-PBH5Q9HY")!!)
 *
 * // Send message over data channel
 * manager.send("""{"type":"command","data":{...}}""")
 *
 * // Disconnect
 * manager.disconnect()
 * ```
 */
class WebRTCConnectionManager(
    private val signalingClient: SignalingClient,
    private val scope: CoroutineScope
) {
    private val logger = Logger.withTag("WebRTCConnectionManager")
    private val mutex = Mutex()

    private var peerConnection: PeerConnectionWrapper? = null
    private var dataChannel: DataChannelWrapper? = null
    private var messageListenerJob: Job? = null
    private var dataChannelStateJob: Job? = null
    private var currentSessionId: String? = null
    private var currentRemoteId: RemoteId? = null

    private val _connectionState = MutableStateFlow<WebRTCConnectionState>(WebRTCConnectionState.Idle)
    val connectionState: StateFlow<WebRTCConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    /**
     * Connect to Music Assistant server via WebRTC.
     *
     * @param remoteId Remote ID of the Music Assistant server
     */
    suspend fun connect(remoteId: RemoteId) = mutex.withLock {
        if (_connectionState.value !is WebRTCConnectionState.Idle &&
            _connectionState.value !is WebRTCConnectionState.Error
        ) {
            logger.w { "Already connecting or connected" }
            return@withLock
        }

        logger.i { "Starting WebRTC connection to Remote ID: $remoteId" }
        currentRemoteId = remoteId
        _connectionState.value = WebRTCConnectionState.ConnectingToSignaling

        try {
            // Step 1: Connect to signaling server
            signalingClient.connect()

            // Step 2: Listen for signaling messages
            startListeningToSignaling()

            // Step 3: Send ConnectRequest message
            logger.d { "Sending ConnectRequest message" }
            signalingClient.sendMessage(SignalingMessage.ConnectRequest(remoteId = remoteId.rawId))

            // Subsequent steps handled in signaling message handlers

        } catch (e: Exception) {
            logger.e(e) { "Failed to connect to signaling server" }
            _connectionState.value = WebRTCConnectionState.Error(
                WebRTCError.SignalingError("Failed to connect to signaling server", e)
            )
            cleanup()
        }
    }

    /**
     * Disconnect from WebRTC connection and cleanup resources.
     */
    suspend fun disconnect() = mutex.withLock {
        logger.i { "Disconnecting WebRTC connection" }
        _connectionState.value = WebRTCConnectionState.Disconnecting
        cleanup()
        _connectionState.value = WebRTCConnectionState.Idle
    }

    /**
     * Send message over WebRTC data channel.
     * Channel must be open (state is Connected).
     *
     * @param message JSON string to send
     */
    fun send(message: String) {
        val channel = dataChannel
        if (channel == null) {
            logger.e { "Cannot send: data channel not available" }
            return
        }

        channel.send(message)
    }

    /**
     * Listen for incoming signaling messages and handle WebRTC setup.
     */
    private fun startListeningToSignaling() {
        messageListenerJob = scope.launch {
            signalingClient.incomingMessages.collect { message ->
                handleSignalingMessage(message)
            }
        }
    }

    /**
     * Handle incoming signaling messages.
     */
    private suspend fun handleSignalingMessage(message: SignalingMessage) {
        logger.d { "Received signaling message: ${message.type}" }

        when (message) {
            is SignalingMessage.Connected -> handleConnected(message)
            is SignalingMessage.Answer -> handleAnswer(message)
            is SignalingMessage.IceCandidate -> handleIceCandidate(message)
            is SignalingMessage.Error -> handleSignalingError(message)
            is SignalingMessage.PeerDisconnected -> handlePeerDisconnected(message)
            is SignalingMessage.Unknown -> logger.w { "Received unknown message type: ${message.type}" }
            else -> logger.d { "Ignoring message type: ${message.type}" }
        }
    }

    /**
     * Handle Connected: Initialize peer connection and create offer.
     */
    private suspend fun handleConnected(message: SignalingMessage.Connected) {
        logger.i { "Connected. Session ID: ${message.sessionId}, ICE servers: ${message.iceServers.size}" }
        currentSessionId = message.sessionId
        _connectionState.value = WebRTCConnectionState.NegotiatingPeerConnection(message.sessionId ?: "")

        try {
            // Create peer connection with callbacks
            val pc = PeerConnectionWrapper(
                onIceCandidate = { candidate ->
                    // Send ICE candidate to signaling server
                    scope.launch {
                        signalingClient.sendMessage(
                            SignalingMessage.IceCandidate(
                                remoteId = currentRemoteId!!.rawId,
                                sessionId = message.sessionId ?: "",
                                data = candidate
                            )
                        )
                    }
                },
                onDataChannel = { channel ->
                    // Server-created data channel (e.g., "sendspin" in the future)
                    logger.i { "Remote data channel received: ${channel.label}" }
                },
                onConnectionStateChange = { state ->
                    logger.d { "Peer connection state: $state" }
                }
            )

            peerConnection = pc

            // Initialize with ICE servers
            pc.initialize(message.iceServers)

            // Create data channel BEFORE offer (required: adds m=application to SDP)
            logger.d { "Creating ma-api data channel" }
            val channel = pc.createDataChannel("ma-api")
            setupDataChannel(channel, message.sessionId ?: "")

            // Create SDP offer (now includes m=application section)
            logger.d { "Creating SDP offer" }
            val offer = pc.createOffer()

            // Send offer to signaling server
            logger.d { "Sending SDP offer" }
            signalingClient.sendMessage(
                SignalingMessage.Offer(
                    remoteId = currentRemoteId!!.rawId,
                    sessionId = message.sessionId ?: "",
                    data = offer
                )
            )

            _connectionState.value = WebRTCConnectionState.GatheringIceCandidates(message.sessionId ?: "")

        } catch (e: Exception) {
            logger.e(e) { "Failed to initialize peer connection" }
            _connectionState.value = WebRTCConnectionState.Error(
                WebRTCError.PeerConnectionError("Failed to initialize peer connection", e)
            )
            cleanup()
        }
    }

    /**
     * Handle Answer: Set remote description.
     */
    private suspend fun handleAnswer(message: SignalingMessage.Answer) {
        logger.i { "Received SDP answer" }
        val pc = peerConnection

        if (pc == null) {
            logger.e { "Received answer but no peer connection exists" }
            return
        }

        try {
            pc.setRemoteAnswer(message.data)
            logger.d { "Remote answer set successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to set remote answer" }
            _connectionState.value = WebRTCConnectionState.Error(
                WebRTCError.PeerConnectionError("Failed to set remote answer", e)
            )
            cleanup()
        }
    }

    /**
     * Handle ICE candidate from remote peer.
     */
    private suspend fun handleIceCandidate(message: SignalingMessage.IceCandidate) {
        logger.d { "Received ICE candidate" }
        val pc = peerConnection

        if (pc == null) {
            logger.e { "Received ICE candidate but no peer connection exists" }
            return
        }

        try {
            pc.addIceCandidate(message.data)
        } catch (e: Exception) {
            logger.e(e) { "Failed to add ICE candidate" }
        }
    }

    /**
     * Handle signaling error.
     */
    private fun handleSignalingError(message: SignalingMessage.Error) {
        logger.e { "Signaling error: ${message.error}" }
        _connectionState.value = WebRTCConnectionState.Error(
            WebRTCError.SignalingError(message.error)
        )
        scope.launch { cleanup() }
    }

    /**
     * Handle peer disconnected notification.
     */
    private fun handlePeerDisconnected(message: SignalingMessage.PeerDisconnected) {
        logger.i { "Remote peer disconnected: ${message.sessionId}" }
        _connectionState.value = WebRTCConnectionState.Error(
            WebRTCError.ConnectionError("Remote peer disconnected")
        )
        scope.launch { cleanup() }
    }

    /**
     * Set up the ma-api data channel: message listener and state monitoring.
     */
    private fun setupDataChannel(channel: DataChannelWrapper, sessionId: String) {
        // Cleanup previous channel if exists (reconnection edge case)
        dataChannelStateJob?.cancel()
        val oldChannel = dataChannel
        if (oldChannel != null) {
            scope.launch { oldChannel.close() }
        }

        dataChannel = channel
        channel.onMessage { msg ->
            logger.d { "Received message on data channel: ${msg.take(100)}" }
            scope.launch {
                _incomingMessages.emit(msg)
            }
        }

        // Check initial state (may already be open)
        if (channel.state.value == "open") {
            logger.d { "Data channel already open" }
            _connectionState.value = WebRTCConnectionState.Connected(
                sessionId = sessionId,
                remoteId = currentRemoteId!!
            )
        }

        // Monitor future state changes
        dataChannelStateJob = scope.launch {
            channel.state.collect { state ->
                if (state == "open") {
                    _connectionState.value = WebRTCConnectionState.Connected(
                        sessionId = sessionId,
                        remoteId = currentRemoteId!!
                    )
                }
            }
        }
    }

    /**
     * Cleanup resources.
     */
    private suspend fun cleanup() {
        messageListenerJob?.cancel()
        messageListenerJob = null

        dataChannelStateJob?.cancel()
        dataChannelStateJob = null

        dataChannel?.close()
        dataChannel = null

        peerConnection?.close()
        peerConnection = null

        signalingClient.disconnect()

        currentSessionId = null
    }
}
