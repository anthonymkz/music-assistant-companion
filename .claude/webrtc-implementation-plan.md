# WebRTC Remote Access Implementation Plan

## Overview

This document details the implementation plan for adding WebRTC remote access support to the Music Assistant KMP client. The implementation will enable users to connect to their Music Assistant server from anywhere without port forwarding, using the cloud-based signaling infrastructure at `wss://signaling.music-assistant.io/ws`.

## Current State

**Status**: Implementation in progress - Phase 1 and 2 completed (2026-02-05)

**Current Connection Method**: Direct WebSocket connections to local server (host:port)
- File: `ServiceClient.kt` uses Ktor WebSockets (`ws`/`wss`)
- Model: `ConnectionInfo(host, port, isTls)`

## Implementation Progress

### ✅ Completed Phases

#### Phase 1.1: Foundation - WebRTC Dependencies (COMPLETED 2026-02-05)
**Status**: ✅ Done

**What was implemented:**
- Added `webrtc-kmp` v0.125.11 dependency to `gradle/libs.versions.toml`
- Updated `composeApp/build.gradle.kts` with WebRTC library
- Updated `.claude/dependencies.md` documentation

**Files modified:**
- `gradle/libs.versions.toml` - Added webrtcKmp version and library
- `composeApp/build.gradle.kts` - Added implementation dependency
- `.claude/dependencies.md` - Updated status

**Library details:**
- Package: `com.shepeliev:webrtc-kmp:0.125.11`
- Based on WebRTC M125 revision
- Supports Android, iOS, and JS platforms
- Repository: https://github.com/shepeliev/webrtc-kmp

---

#### Phase 1.2: Data Models (COMPLETED 2026-02-05)
**Status**: ✅ Done

**What was implemented:**
- Created complete WebRTC data model layer with sealed classes and enums
- All models use kotlinx.serialization
- Type-safe state management

**Files created:**
1. `webrtc/model/RemoteId.kt` (75 lines)
   - Remote ID data class with parsing and validation
   - Supports multiple formats: `MA-XXXX-XXXX`, raw alphanumeric
   - `parse()` function for user input validation
   - `formatted` property for UI display

2. `webrtc/model/SignalingMessage.kt` (133 lines)
   - Sealed interface for all signaling protocol messages
   - Message types: Connect, SessionReady, Offer, Answer, IceCandidate, Error, ClientDisconnected, Registered
   - Supporting models: IceServer, SessionDescription, IceCandidateData
   - Full kotlinx.serialization support

3. `webrtc/model/WebRTCState.kt` (140 lines)
   - `WebRTCConnectionState` - Connection state machine (Idle, Connecting, Negotiating, Connected, Error)
   - `WebRTCError` - Categorized errors (SignalingError, RemoteIdNotFound, PeerConnectionError, IceConnectionFailed, DataChannelError, ConnectionError)
   - `RTCPeerConnectionState`, `RTCIceConnectionState`, `RTCDataChannelState` - WebRTC standard state enums
   - `IceCandidateType` - ICE candidate types for diagnostics

**Code example:**
```kotlin
val remoteId = RemoteId.parse("MA-VVPN-3TLP") // Validates and parses
val message = SignalingMessage.Connect(remoteId = remoteId.rawId)
val state: WebRTCConnectionState = WebRTCConnectionState.Connected(sessionId, remoteId)
```

---

#### Phase 1.3: Settings Integration (COMPLETED 2026-02-05)
**Status**: ✅ Done

**What was implemented:**
- Added WebRTC settings to Settings screen
- Remote ID input with real-time validation
- Enable/disable toggle for WebRTC

**Files modified:**
1. `SettingsRepository.kt`
   - Added `webrtcEnabled: StateFlow<Boolean>`
   - Added `webrtcRemoteId: StateFlow<String>`
   - Added `setWebrtcEnabled()` and `setWebrtcRemoteId()` methods
   - Settings persist to local storage

2. `SettingsViewModel.kt`
   - Exposed `webrtcEnabled` and `webrtcRemoteId` flows
   - Added setter delegate methods

3. `SettingsScreen.kt` (Added 95-line WebRTCSection composable)
   - Enable/disable WebRTC toggle switch
   - Remote ID text field with placeholder "MA-XXXX-XXXX"
   - Auto-uppercase input conversion
   - Real-time validation using `RemoteId.isValid()`
   - Error message display for invalid format
   - Supporting text with instructions
   - Info note about cloud signaling and encryption
   - Section visible only when authenticated

**UI Features:**
```kotlin
@Composable
private fun WebRTCSection(viewModel: SettingsViewModel) {
    val webrtcEnabled by viewModel.webrtcEnabled.collectAsStateWithLifecycle()
    val webrtcRemoteId by viewModel.webrtcRemoteId.collectAsStateWithLifecycle()

    // Toggle switch
    // Remote ID input with validation
    // Error messages
    // Info text
}
```

---

#### Phase 2: Signaling Client (COMPLETED 2026-02-05)
**Status**: ✅ Done

**What was implemented:**
- Complete WebSocket signaling client implementation
- Custom JSON serializer for polymorphic message types
- Connection state management with reactive flows

**Files created:**
1. `webrtc/SignalingClient.kt` (227 lines)
   - WebSocket client using Ktor
   - Connects to `wss://signaling.music-assistant.io/ws`
   - Methods:
     - `connect()` - Establish WebSocket connection
     - `sendMessage(SignalingMessage)` - Send signaling messages
     - `disconnect()` - Clean disconnect
     - `close()` - Cleanup resources
   - Flows:
     - `connectionState: StateFlow<SignalingState>` - Connection state (Disconnected, Connecting, Connected, Error)
     - `incomingMessages: SharedFlow<SignalingMessage>` - Stream of received messages
   - Features:
     - Automatic JSON serialization/deserialization
     - Coroutine-based non-blocking I/O
     - Comprehensive error handling
     - Resource cleanup on close
     - Logging with Kermit

2. `webrtc/SignalingMessageSerializer.kt` (30 lines)
   - Custom `JsonContentPolymorphicSerializer` for `SignalingMessage`
   - Uses "type" field as discriminator (WebRTC protocol standard)
   - Handles all 8 message types dynamically
   - Example: `{"type": "connect", "remoteId": "..."}` → `SignalingMessage.Connect`

**Usage example:**
```kotlin
val signalingClient = SignalingClient()

// Observe connection state
launch {
    signalingClient.connectionState.collect { state ->
        when (state) {
            SignalingState.Connected -> { /* Ready */ }
            is SignalingState.Error -> { /* Handle error */ }
        }
    }
}

// Observe incoming messages
launch {
    signalingClient.incomingMessages.collect { message ->
        when (message) {
            is SignalingMessage.SessionReady -> { /* Got ICE servers */ }
            is SignalingMessage.Answer -> { /* Got SDP answer */ }
        }
    }
}

// Connect and send
signalingClient.connect()
signalingClient.sendMessage(SignalingMessage.Connect(remoteId = "MA-XXXX-XXXX"))
```

**Architecture:**
```
SignalingClient
    ↓ (Ktor WebSocket)
wss://signaling.music-assistant.io/ws
    ↓ (Routes messages)
Music Assistant Gateway
```

---

#### Phase 2.5: Architecture Audit & Fixes (COMPLETED 2026-02-05)
**Status**: ✅ Done

After completing Phase 2, a comprehensive architectural audit identified and fixed critical issues before proceeding to Phase 3. All fixes maintain minimal code changes while ensuring production-quality architecture.

**Issues Fixed:**

1. **SignalingClient Lifecycle (CRITICAL)**
   - **Problem**: Implemented `CoroutineScope` with self-owned `SupervisorJob` → memory leak risk
   - **Fix**: Now accepts external `scope: CoroutineScope` via constructor
   - **Impact**: Proper lifecycle management, no resource leaks

2. **Thread Safety (CRITICAL)**
   - **Problem**: Mutable `session` and `receiveJob` vars without synchronization → race conditions
   - **Fix**: Added `Mutex` to protect `connect()` and `disconnect()` operations
   - **Impact**: Thread-safe concurrent access

3. **Resource Management**
   - **Problem**: Created new `HttpClient` per instance → resource waste (Ktor best practice: reuse)
   - **Fix**: Accept `HttpClient` via constructor, created `webrtcModule` DI configuration
   - **Impact**: Shared HttpClient, proper resource management

4. **Cleanup Operations**
   - **Problem**: `close()` was non-suspending, `disconnect()` used `cancel()` without waiting
   - **Fix**: Made `close()` suspend, use `cancelAndJoin()` for proper cleanup
   - **Impact**: Graceful shutdown, no dangling coroutines

5. **Forward Compatibility (CRITICAL)**
   - **Problem**: Unknown signaling message types → `IllegalArgumentException` → client crash
   - **Fix**: Added `SignalingMessage.Unknown` type, log warning instead of crash
   - **Impact**: Client survives server protocol extensions

6. **RemoteId Validation**
   - **Problem**: No constructor validation → potential `IndexOutOfBoundsException` in `formatted` property
   - **Fix**: Added `init` block requiring 8-26 alphanumeric chars
   - **Impact**: Fail-fast validation, no runtime crashes

7. **SignalingMessage Type Field**
   - **Problem**: `type` was constructor parameter → allowed inconsistent instances
   - **Fix**: Made `type` an immutable property, not a constructor parameter
   - **Impact**: Type safety, no invalid messages

8. **Code Cleanup (YAGNI)**
   - **Problem**: ~180 lines of unused enums and dead code (premature optimization)
   - **Fix**: Deleted unused `RTCPeerConnectionState`, `RTCIceConnectionState`, `RTCDataChannelState`, `IceCandidateType` enums and `ServerConnectionSection` composable
   - **Impact**: Reduced codebase size, improved maintainability

9. **Settings Screen UX**
   - **Problem**: Tab selection lost on recomposition
   - **Fix**: Added `preferredConnectionMethod` to `SettingsRepository` for persistence
   - **Impact**: Better UX, remembers user preference

**Files Modified:**
- `SignalingClient.kt` - Lifecycle, thread safety, resource management
- `SignalingMessageSerializer.kt` - Forward compatibility
- `SignalingMessage.kt` - Type immutability, Unknown message type
- `RemoteId.kt` - Constructor validation
- `WebRTCState.kt` - Removed unused enums (-90 lines)
- `SettingsRepository.kt` - Connection method persistence
- `SettingsViewModel.kt` - Exposed preferences
- `SettingsScreen.kt` - Persistent tabs, removed dead code (-90 lines)

**Files Created:**
- `WebRTCModule.kt` - Koin DI module for WebRTC components

**Build Status**: ✅ All changes compile successfully

**Code Quality**: Production-ready architecture with proper lifecycle management, thread safety, and forward compatibility.

---

### 🔄 Next Phases (Not Started)

#### Phase 3: WebRTC Peer Connection & Connection Manager (IN PROGRESS)
**Status**: 🔄 Planning Complete - Ready for Implementation

**Architecture Decisions:**

1. **Connection Mode Exclusivity**
   - WebRTC and Direct connection are **mutually exclusive** (either/or, never both)
   - When WebRTC is connected, Direct connection is unavailable and vice-versa
   - User selects connection method via tabs in Settings screen (already implemented)
   - Settings track `preferredConnectionMethod: "direct" | "webrtc"`

2. **ServiceClient Code Reuse Strategy**
   - WebRTC data channel proxies WebSocket connection (same protocol)
   - **Everything above authentication layer is identical**: message handling, event dispatching, state management
   - **Connection establishment differs**: WebRTC uses peer connection + data channel instead of raw WebSocket
   - **Implementation approach**:
     - Phase 3: Create separate WebRTC implementation (don't touch ServiceClient yet)
     - Phase 4: Identify common code and extract shared logic
     - Phase 5: Refactor ServiceClient to support both modes via abstraction

3. **Library Choice: webrtc-kmp**
   - Using `com.shepeliev:webrtc-kmp:0.125.11` (already added in Phase 1.1)
   - Wraps native WebRTC for Android and iOS
   - Reduces platform-specific code significantly
   - Provides KMP-friendly API with coroutines support

4. **Platform Strategy**
   - **Android**: Full implementation using webrtc-kmp
   - **iOS**: expect/actual stubs with detailed comments for future implementation
   - Don't touch iOS code yet - leave well-documented TODOs

**What needs to be implemented:**

### 3.1: WebRTC Connection Manager (Core Orchestration)

**File**: `webrtc/WebRTCConnectionManager.kt` (~250 lines)

Orchestrates the complete WebRTC connection lifecycle:
- Manages SignalingClient + PeerConnection together
- Handles connection state machine
- Coordinates signaling with peer connection setup
- Creates and manages data channels
- Error handling and recovery

**Responsibilities:**
```kotlin
class WebRTCConnectionManager(
    private val signalingClient: SignalingClient,
    private val scope: CoroutineScope
) {
    val connectionState: StateFlow<WebRTCConnectionState>
    val dataChannel: StateFlow<WebRTCDataChannel?>

    // Connect to MA server via WebRTC
    suspend fun connect(remoteId: RemoteId)

    // Disconnect and cleanup
    suspend fun disconnect()

    // Send data over channel (for ServiceClient integration later)
    suspend fun send(message: String)
}
```

**State Machine:**
```
Idle
  → ConnectingToSignaling (connecting to wss://signaling.music-assistant.io)
  → NegotiatingPeerConnection (exchanging SDP offer/answer)
  → GatheringIceCandidates (finding network path via STUN/TURN)
  → Connected (data channel open, ready for messages)
  → Error (any failure along the way)
```

### 3.2: Peer Connection Abstraction (expect/actual)

**Common Interface**: `webrtc/PeerConnectionWrapper.kt`

```kotlin
expect class PeerConnectionWrapper(
    scope: CoroutineScope,
    onIceCandidate: (IceCandidateData) -> Unit,
    onDataChannel: (DataChannelWrapper) -> Unit,
    onConnectionStateChange: (state: String) -> Unit
) {
    suspend fun initialize(iceServers: List<IceServer>)
    suspend fun createOffer(): SessionDescription
    suspend fun setRemoteAnswer(answer: SessionDescription)
    suspend fun addIceCandidate(candidate: IceCandidateData)
    fun createDataChannel(label: String): DataChannelWrapper
    suspend fun close()
}

expect class DataChannelWrapper {
    val label: String
    val state: StateFlow<String>

    fun send(message: String)
    fun onMessage(callback: (String) -> Unit)
    suspend fun close()
}
```

### 3.3: Android Implementation (webrtc-kmp)

**File**: `androidMain/.../webrtc/PeerConnectionWrapper.android.kt` (~200 lines)

Uses `com.shepeliev.webrtc.kmp` library:
```kotlin
actual class PeerConnectionWrapper actual constructor(
    scope: CoroutineScope,
    onIceCandidate: (IceCandidateData) -> Unit,
    onDataChannel: (DataChannelWrapper) -> Unit,
    onConnectionStateChange: (state: String) -> Unit
) {
    private val peerConnection: RTCPeerConnection

    actual suspend fun initialize(iceServers: List<IceServer>) {
        // Use webrtc-kmp RTCPeerConnection
        // Configure with ICE servers from signaling server
        // Set up callbacks for ICE candidates and state changes
    }

    actual suspend fun createOffer(): SessionDescription {
        // Generate SDP offer using webrtc-kmp
        // Return as our SignalingMessage.SessionDescription format
    }

    // ... rest of implementation using webrtc-kmp APIs
}
```

### 3.4: iOS Stubs (Future Implementation)

**File**: `iosMain/.../webrtc/PeerConnectionWrapper.ios.kt` (~50 lines of TODOs)

```kotlin
actual class PeerConnectionWrapper actual constructor(
    scope: CoroutineScope,
    onIceCandidate: (IceCandidateData) -> Unit,
    onDataChannel: (DataChannelWrapper) -> Unit,
    onConnectionStateChange: (state: String) -> Unit
) {
    actual suspend fun initialize(iceServers: List<IceServer>) {
        // TODO: iOS Implementation
        // Will use webrtc-kmp RTCPeerConnection for iOS
        // Same API as Android, different underlying native library
        // Requires: Cocoapods or SPM integration for WebRTC.xcframework
        throw NotImplementedError("iOS WebRTC not yet implemented")
    }

    // ... all other methods: throw NotImplementedError with detailed TODO comments
}
```

### 3.5: Integration Flow

**WebRTCConnectionManager.connect() implementation:**

```
1. User enters Remote ID and clicks "Connect via WebRTC"
2. WebRTCConnectionManager.connect(remoteId) called
3. State → ConnectingToSignaling
4. SignalingClient.connect() to wss://signaling.music-assistant.io/ws
5. Send SignalingMessage.Connect(remoteId)
6. Receive SignalingMessage.SessionReady(sessionId, iceServers)
7. State → NegotiatingPeerConnection
8. PeerConnectionWrapper.initialize(iceServers)
9. PeerConnectionWrapper.createOffer() → SDP offer
10. Send SignalingMessage.Offer(sessionId, sdp)
11. Receive SignalingMessage.Answer(sessionId, sdp)
12. PeerConnectionWrapper.setRemoteAnswer(sdp)
13. State → GatheringIceCandidates
14. As ICE candidates discovered → Send SignalingMessage.IceCandidate(...)
15. As ICE candidates received → PeerConnectionWrapper.addIceCandidate(...)
16. Receive onDataChannel callback → "ma-api" data channel opened by server
17. State → Connected
18. Ready to send/receive messages over data channel
```

### 3.6: Data Channel Message Routing

Once connected, the data channel carries the same WebSocket protocol that ServiceClient uses:

**Message Format** (same as current WS):
```json
{
  "type": "command",
  "message_id": "uuid",
  "data": { ... }
}
```

**Strategy for Phase 3:**
- Implement basic send/receive over data channel
- Log received messages to verify protocol compatibility
- Don't integrate with ServiceClient yet (Phase 4-5)

**Files to create:**
1. `webrtc/WebRTCConnectionManager.kt` - Core orchestration (~250 lines)
2. `webrtc/PeerConnectionWrapper.kt` - Common interface (~100 lines)
3. `androidMain/.../webrtc/PeerConnectionWrapper.android.kt` - Android impl (~200 lines)
4. `iosMain/.../webrtc/PeerConnectionWrapper.ios.kt` - Stubs (~50 lines)
5. `androidMain/.../webrtc/DataChannelWrapper.android.kt` - Android impl (~100 lines)
6. `iosMain/.../webrtc/DataChannelWrapper.ios.kt` - Stubs (~30 lines)

**Testing approach:**
- Android emulator or device with network access
- Real MA server with WebRTC enabled and Remote ID visible
- Debug logs for each state transition
- Verify data channel messages match WebSocket protocol

**Estimated complexity**: High (native WebRTC integration)
**Estimated LOC**: ~730 lines (550 Android, 80 iOS stubs, 100 common)

---

#### Phase 4: WebRTC Integration Layer
**Status**: ⏳ Pending

**What needs to be implemented:**
- `WebRTCConnectionManager.kt` - Orchestrates signaling + peer connection
- Integrates SignalingClient with WebRTCPeerConnection
- Manages connection lifecycle end-to-end
- Handles ICE candidate exchange
- Creates and manages data channels

---

#### Phase 5: ServiceClient Integration
**Status**: ⏳ Pending

**What needs to be implemented:**
- Extend `ConnectionInfo` model → `ConnectionMode` sealed class
- Add `ConnectionMode.WebRTC(remoteId)` alongside `ConnectionMode.Direct(host, port, tls)`
- Modify `ServiceClient.connect()` to support both modes
- Route API messages over WebRTC data channel when in WebRTC mode

---

#### Phase 6: UI Integration
**Status**: ⏳ Pending

**What needs to be implemented:**
- Add "Connect via WebRTC" button to SettingsScreen
- Show connection status (connecting, negotiating, connected)
- Display ICE connection state
- Error handling and user feedback

---

#### Phase 7: Sendspin Integration
**Status**: ⏳ Pending

**What needs to be implemented:**
- Update `SendspinClient` to support WebRTC data channel mode
- Create `WebRTCDataChannelHandler` (similar to `SendspinWsHandler`)
- Route Sendspin protocol over "sendspin" data channel

---

#### Phase 8: Testing & Refinement
**Status**: ⏳ Pending

**What needs to be tested:**
- End-to-end connection flow
- ICE gathering (STUN/TURN)
- Data channel reliability
- Error recovery
- Network disconnection/reconnection
- Platform compatibility (Android/iOS)

---

### 📊 Summary Statistics

**Lines of code written**: ~600 lines (net after audit cleanup)
- Data models: ~280 lines (after removing unused enums)
- Signaling client: ~200 lines (refactored for lifecycle safety)
- Settings integration: ~120 lines (refactored with tabs)
- DI configuration: ~30 lines

**Files created**: 8
- 3 model files (RemoteId, SignalingMessage, WebRTCState)
- 2 signaling client files (SignalingClient, SignalingMessageSerializer)
- 1 DI module (WebRTCModule)
- 2 settings files (modified: SettingsRepository, SettingsViewModel, SettingsScreen)

**Files deleted/cleaned**:
- ~180 lines of unused/dead code removed (enums, dead composables)

**Completion**: ~35% (Phases 1-2 complete, Phase 3 planned)

**Quality improvements**: All architectural issues from audit fixed
- Thread safety (Mutex)
- Lifecycle management (injected scope)
- Forward compatibility (Unknown message type)
- Input validation (RemoteId constructor)

**Next milestone**: WebRTC Peer Connection implementation (Phase 3)

---

### 🎯 Current Capabilities

What works right now:
- ✅ WebRTC settings UI with Remote ID input
- ✅ Remote ID validation and parsing
- ✅ Signaling client can connect to signaling server
- ✅ Can send/receive signaling messages
- ✅ Settings persist across app restarts

What doesn't work yet:
- ❌ Cannot establish WebRTC peer connections
- ❌ Cannot create data channels
- ❌ Cannot actually connect via WebRTC
- ❌ No integration with ServiceClient
- ❌ No Sendspin over WebRTC

**Bottom line**: Infrastructure is ready, but peer connection layer (the core WebRTC functionality) is not implemented yet.

## Architecture Overview

### Three-Component System

```
┌─────────────────────┐         ┌──────────────────────┐         ┌─────────────────────┐
│  Remote Client      │         │  Signaling Server    │         │  Local MA Server    │
│  (KMP App)          │◄───────►│  (Cloud WebSocket)   │◄───────►│  (WebRTC Gateway)   │
│                     │         │                      │         │                     │
│  RTCPeerConnection  │         │  Message Routing     │         │  aiortc Library     │
└─────────────────────┘         └──────────────────────┘         └─────────────────────┘
         │                                                                   │
         │                    WebRTC Data Channel                            │
         │                    (DTLS Encrypted)                               │
         └───────────────────────────────────────────────────────────────────┘
                                      │
                         ┌────────────┴────────────┐
                         │                         │
                    MA-API Channel          Sendspin Channel
                (JSON WebSocket API)    (Binary Audio Protocol)
```

### Connection Flow

**Phase 1: Gateway Registration** (Server-side, already implemented)
```
Music Assistant Server starts
    ↓
WebRTC Gateway initializes
    ↓
Generates/loads DTLS certificate (ECDSA SECP256R1)
    ↓
Derives Remote ID from certificate fingerprint
    ↓
Connects to wss://signaling.music-assistant.io/ws
    ↓
Sends: {type: "register-server", remoteId: "MA-XXXX-XXXX", iceServers: [...]}
    ↓
Receives: {type: "registered", remoteId: "MA-XXXX-XXXX"}
    ↓
Gateway ready to accept client connections
```

**Phase 2: Client Connection** (To be implemented in KMP client)
```
User enables WebRTC mode in settings
    ↓
User enters Remote ID (MA-XXXX-XXXX)
    ↓
Client connects to wss://signaling.music-assistant.io/ws
    ↓
Client sends: {type: "connect", remoteId: "MA-XXXX-XXXX"}
    ↓
Creates RTCPeerConnection with ICE servers
    ↓
Generates SDP offer
    ↓
Sends: {type: "offer", sessionId: "...", data: {sdp: "...", type: "offer"}}
    ↓
Gateway receives offer, creates peer connection
    ↓
Gateway sends: {type: "answer", sessionId: "...", data: {sdp: "...", type: "answer"}}
    ↓
Client receives answer, sets remote description
    ↓
ICE candidates exchanged: {type: "ice-candidate", sessionId: "...", data: {...}}
    ↓
WebRTC peer connection established (DTLS handshake)
    ↓
Data channels created: "ma-api" (default) and "sendspin"
    ↓
Client sends API messages over ma-api channel (JSON text)
    ↓
Gateway forwards to local WebSocket API
    ↓
Responses forwarded back over data channel
```

## WebRTC Signaling Protocol

### Message Types

#### Client → Signaling Server

**1. Connect Request**
```json
{
  "type": "connect",
  "remoteId": "MA-XXXX-XXXX"
}
```

**2. SDP Offer**
```json
{
  "type": "offer",
  "sessionId": "unique-session-id",
  "data": {
    "sdp": "v=0\no=- 123456789 2 IN IP4 127.0.0.1\n...",
    "type": "offer"
  }
}
```

**3. ICE Candidate**
```json
{
  "type": "ice-candidate",
  "sessionId": "unique-session-id",
  "data": {
    "candidate": "candidate:0 1 UDP 2113937151 192.168.1.100 51472 typ host",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

#### Signaling Server → Client

**1. Connection Accepted**
```json
{
  "type": "session-ready",
  "sessionId": "unique-session-id",
  "iceServers": [
    {"urls": "stun:stun.home-assistant.io:3478"},
    {"urls": "stun:stun.l.google.com:19302"},
    {"urls": "turn:turn.nabucasa.com:3478", "username": "...", "credential": "..."}
  ]
}
```

**2. SDP Answer**
```json
{
  "type": "answer",
  "sessionId": "unique-session-id",
  "data": {
    "sdp": "v=0\no=- 987654321 2 IN IP4 127.0.0.1\n...",
    "type": "answer"
  }
}
```

**3. ICE Candidate from Gateway**
```json
{
  "type": "ice-candidate",
  "sessionId": "unique-session-id",
  "data": {
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

**4. Error**
```json
{
  "type": "error",
  "error": "Remote ID not found / Invalid session / etc."
}
```

**5. Client Disconnected**
```json
{
  "type": "client-disconnected",
  "sessionId": "unique-session-id"
}
```

### Data Channel Structure

Once WebRTC connection is established:

**1. MA-API Channel** (default channel, label: "ma-api" or default)
- **Purpose**: Bridge to Music Assistant WebSocket API
- **Message Format**: JSON text messages (same as existing WebSocket API)
- **Usage**: All API commands (get players, queue actions, library browsing, etc.)
- **Example**:
  ```json
  {
    "command": "music/players/get_all",
    "message_id": "abc123"
  }
  ```

**2. Sendspin Channel** (label: "sendspin")
- **Purpose**: Real-time audio streaming with Sendspin protocol
- **Message Format**: Sendspin protocol messages (binary chunks + JSON metadata)
- **Usage**: Built-in player audio streaming
- **Note**: Gateway forwards to internal Sendspin server (ws://localhost:8927/sendspin)

## ICE Server Configuration

### Basic Mode (No HA Cloud Subscription)

Free STUN servers provided by Open Home Foundation and public infrastructure:

```kotlin
val basicIceServers = listOf(
    RTCIceServer(urls = listOf("stun:stun.home-assistant.io:3478")),
    RTCIceServer(urls = listOf("stun:stun.l.google.com:19302")),
    RTCIceServer(urls = listOf("stun:stun1.l.google.com:19302")),
    RTCIceServer(urls = listOf("stun:stun.cloudflare.com:3478"))
)
```

**Connectivity**: Works for most home networks and simple NAT scenarios

### Optimized Mode (HA Cloud Subscription)

Enhanced connectivity with TURN relay servers:

```kotlin
// Provided by signaling server in session-ready message
val optimizedIceServers = listOf(
    RTCIceServer(urls = listOf("stun:stun.home-assistant.io:3478")),
    RTCIceServer(
        urls = listOf("turn:turn.nabucasa.com:3478"),
        username = "temporary-username",
        credential = "temporary-credential"
    )
)
```

**Connectivity**: Guaranteed connectivity even through:
- Double NAT
- Corporate firewalls
- Mobile carrier NATs
- Symmetric NATs

**Note**: TURN credentials are time-limited and fetched fresh for each session

## Remote ID Format

The Remote ID uniquely identifies a Music Assistant server instance.

**Generation** (Server-side):
```
1. DTLS Certificate SHA-256 fingerprint (32 bytes)
2. Truncate to first 128 bits (16 bytes)
3. Base32 encode (with '9' instead of '2')
4. Result: 26-character uppercase alphanumeric string
```

**Example Raw ID**: `VVPN3TLP34YMGIZDINCEKQKSIR`

**User-Facing Format**: `MA-XXXX-XXXX` (with hyphen formatting for readability)

**Client Implementation**:
```kotlin
data class RemoteId(val rawId: String) {
    val formatted: String
        get() = "MA-${rawId.substring(0, 4)}-${rawId.substring(4, 8)}"

    companion object {
        fun parse(input: String): RemoteId? {
            val cleaned = input.replace("MA-", "").replace("-", "").uppercase()
            return if (cleaned.matches(Regex("[A-Z0-9]{8,26}"))) {
                RemoteId(cleaned)
            } else null
        }
    }
}
```

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**1.1 Add WebRTC Dependencies**

Update `gradle/libs.versions.toml`:
```toml
[versions]
webrtc = "1.0.0" # Check latest KMP WebRTC wrapper

[libraries]
webrtc-common = { module = "io.github.webrtc-sdk:webrtc-kmp", version.ref = "webrtc" }
webrtc-android = { module = "org.webrtc:google-webrtc", version = "1.0.42" }
webrtc-ios = { module = "WebRTC-SDK", version = "125.0" }
```

Update `composeApp/build.gradle.kts`:
```kotlin
commonMain.dependencies {
    implementation(libs.webrtc.common)
}
androidMain.dependencies {
    implementation(libs.webrtc.android)
}
iosMain.dependencies {
    implementation(libs.webrtc.ios)
}
```

**1.2 Create WebRTC Models**

File: `composeApp/src/commonMain/kotlin/io/music_assistant/client/webrtc/model/WebRTCModels.kt`
```kotlin
// Remote ID
data class RemoteId(val rawId: String) { /* ... */ }

// ICE Server
data class IceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

// Signaling messages
@Serializable
sealed interface SignalingMessage {
    @Serializable
    data class Connect(val remoteId: String) : SignalingMessage

    @Serializable
    data class SessionReady(
        val sessionId: String,
        val iceServers: List<IceServer>
    ) : SignalingMessage

    @Serializable
    data class Offer(
        val sessionId: String,
        val data: SessionDescription
    ) : SignalingMessage

    @Serializable
    data class Answer(
        val sessionId: String,
        val data: SessionDescription
    ) : SignalingMessage

    @Serializable
    data class IceCandidate(
        val sessionId: String,
        val data: IceCandidateData
    ) : SignalingMessage

    @Serializable
    data class Error(val error: String) : SignalingMessage

    @Serializable
    data class ClientDisconnected(val sessionId: String) : SignalingMessage
}

@Serializable
data class SessionDescription(
    val sdp: String,
    val type: String // "offer" or "answer"
)

@Serializable
data class IceCandidateData(
    val candidate: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int?
)
```

**1.3 Create WebRTC Connection State**

File: `composeApp/src/commonMain/kotlin/io/music_assistant/client/webrtc/WebRTCState.kt`
```kotlin
sealed class WebRTCConnectionState {
    object Idle : WebRTCConnectionState()
    object ConnectingToSignaling : WebRTCConnectionState()
    data class NegotiatingPeerConnection(val sessionId: String) : WebRTCConnectionState()
    data class Connected(
        val sessionId: String,
        val remoteId: RemoteId
    ) : WebRTCConnectionState()
    data class Error(val error: WebRTCError) : WebRTCConnectionState()
}

sealed class WebRTCError {
    data class SignalingError(val message: String) : WebRTCError()
    data class PeerConnectionError(val message: String) : WebRTCError()
    data class RemoteIdNotFound(val remoteId: RemoteId) : WebRTCError()
    data class IceConnectionFailed(val reason: String) : WebRTCError()
}
```

### Phase 2: Signaling Client (Week 3)

**2.1 Implement Signaling WebSocket Client**

File: `composeApp/src/commonMain/kotlin/io/music_assistant/client/webrtc/SignalingClient.kt`
```kotlin
class SignalingClient(
    private val signalingUrl: String = "wss://signaling.music-assistant.io/ws"
) : CoroutineScope {
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + supervisorJob

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 10)
    val incomingMessages: Flow<SignalingMessage> = _incomingMessages.asSharedFlow()

    private var session: WebSocketSession? = null
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(myJson)
        }
    }

    suspend fun connect() {
        _connectionState.value = WebSocketState.Connecting
        try {
            session = client.webSocketSession(signalingUrl)
            _connectionState.value = WebSocketState.Connected

            // Start listening for messages
            launch { listenForMessages() }
        } catch (e: Exception) {
            _connectionState.value = WebSocketState.Error(e)
        }
    }

    private suspend fun listenForMessages() {
        val session = session ?: return
        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val message = myJson.decodeFromString<SignalingMessage>(text)
                        _incomingMessages.emit(message)
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Signaling message receive error" }
            _connectionState.value = WebSocketState.Error(e)
        }
    }

    suspend fun sendMessage(message: SignalingMessage) {
        val session = session ?: throw IllegalStateException("Not connected")
        val json = myJson.encodeToString(message)
        session.send(json)
    }

    suspend fun disconnect() {
        session?.close()
        session = null
        _connectionState.value = WebSocketState.Disconnected
    }

    fun close() {
        supervisorJob.cancel()
        client.close()
    }
}
```

**2.2 Add Settings for WebRTC**

Update `SettingsRepository.kt`:
```kotlin
// WebRTC remote access settings
val webrtcEnabled = booleanFlow("webrtc_enabled", false)
val webrtcRemoteId = stringFlow("webrtc_remote_id", "")

fun updateWebRTCEnabled(enabled: Boolean) {
    settings.putBoolean("webrtc_enabled", enabled)
}

fun updateWebRTCRemoteId(remoteId: String) {
    settings.putString("webrtc_remote_id", remoteId)
}
```

### Phase 3: WebRTC Peer Connection (Week 4-5)

**3.1 Create WebRTC Abstraction Interface**

File: `composeApp/src/commonMain/kotlin/io/music_assistant/client/webrtc/WebRTCPeerConnection.kt`
```kotlin
interface WebRTCPeerConnection {
    val connectionState: StateFlow<RTCPeerConnectionState>
    val iceConnectionState: StateFlow<RTCIceConnectionState>

    suspend fun initialize(iceServers: List<IceServer>)

    suspend fun createOffer(): SessionDescription
    suspend fun setRemoteDescription(description: SessionDescription)
    suspend fun addIceCandidate(candidate: IceCandidateData)

    fun createDataChannel(label: String): WebRTCDataChannel
    fun onDataChannel(callback: (WebRTCDataChannel) -> Unit)

    fun onIceCandidate(callback: (IceCandidateData) -> Unit)

    suspend fun close()
}

interface WebRTCDataChannel {
    val label: String
    val readyState: StateFlow<RTCDataChannelState>

    fun send(message: String)
    fun send(data: ByteArray)

    fun onMessage(callback: (ByteArray) -> Unit)
    fun onOpen(callback: () -> Unit)
    fun onClose(callback: () -> Unit)

    fun close()
}

enum class RTCPeerConnectionState {
    NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED
}

enum class RTCIceConnectionState {
    NEW, CHECKING, CONNECTED, COMPLETED, FAILED, DISCONNECTED, CLOSED
}

enum class RTCDataChannelState {
    CONNECTING, OPEN, CLOSING, CLOSED
}
```

**3.2 Implement Platform-Specific WebRTC**

File: `composeApp/src/androidMain/kotlin/io/music_assistant/client/webrtc/WebRTCPeerConnection.android.kt`
```kotlin
actual class WebRTCPeerConnectionImpl : WebRTCPeerConnection {
    private lateinit var peerConnection: org.webrtc.PeerConnection
    private lateinit var peerConnectionFactory: PeerConnectionFactory

    private val _connectionState = MutableStateFlow(RTCPeerConnectionState.NEW)
    override val connectionState: StateFlow<RTCPeerConnectionState> = _connectionState.asStateFlow()

    private val _iceConnectionState = MutableStateFlow(RTCIceConnectionState.NEW)
    override val iceConnectionState: StateFlow<RTCIceConnectionState> = _iceConnectionState.asStateFlow()

    private var iceCandidateCallback: ((IceCandidateData) -> Unit)? = null
    private var dataChannelCallback: ((WebRTCDataChannel) -> Unit)? = null

    override suspend fun initialize(iceServers: List<IceServer>) {
        // Initialize PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()

        // Convert ICE servers
        val rtcIceServers = iceServers.map { server ->
            org.webrtc.PeerConnection.IceServer.builder(server.urls)
                .apply {
                    server.username?.let { setUsername(it) }
                    server.credential?.let { setPassword(it) }
                }
                .createIceServer()
        }

        val rtcConfig = org.webrtc.PeerConnection.RTCConfiguration(rtcIceServers)

        // Create peer connection with observer
        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : org.webrtc.PeerConnection.Observer {
                override fun onIceCandidate(candidate: org.webrtc.IceCandidate) {
                    iceCandidateCallback?.invoke(
                        IceCandidateData(
                            candidate = candidate.sdp,
                            sdpMid = candidate.sdpMid,
                            sdpMLineIndex = candidate.sdpMLineIndex
                        )
                    )
                }

                override fun onDataChannel(dataChannel: org.webrtc.DataChannel) {
                    dataChannelCallback?.invoke(WebRTCDataChannelImpl(dataChannel))
                }

                override fun onIceConnectionChange(state: org.webrtc.PeerConnection.IceConnectionState) {
                    _iceConnectionState.value = when (state) {
                        org.webrtc.PeerConnection.IceConnectionState.NEW -> RTCIceConnectionState.NEW
                        org.webrtc.PeerConnection.IceConnectionState.CHECKING -> RTCIceConnectionState.CHECKING
                        org.webrtc.PeerConnection.IceConnectionState.CONNECTED -> RTCIceConnectionState.CONNECTED
                        org.webrtc.PeerConnection.IceConnectionState.COMPLETED -> RTCIceConnectionState.COMPLETED
                        org.webrtc.PeerConnection.IceConnectionState.FAILED -> RTCIceConnectionState.FAILED
                        org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED -> RTCIceConnectionState.DISCONNECTED
                        org.webrtc.PeerConnection.IceConnectionState.CLOSED -> RTCIceConnectionState.CLOSED
                    }
                }

                override fun onConnectionChange(state: org.webrtc.PeerConnection.PeerConnectionState) {
                    _connectionState.value = when (state) {
                        org.webrtc.PeerConnection.PeerConnectionState.NEW -> RTCPeerConnectionState.NEW
                        org.webrtc.PeerConnection.PeerConnectionState.CONNECTING -> RTCPeerConnectionState.CONNECTING
                        org.webrtc.PeerConnection.PeerConnectionState.CONNECTED -> RTCPeerConnectionState.CONNECTED
                        org.webrtc.PeerConnection.PeerConnectionState.DISCONNECTED -> RTCPeerConnectionState.DISCONNECTED
                        org.webrtc.PeerConnection.PeerConnectionState.FAILED -> RTCPeerConnectionState.FAILED
                        org.webrtc.PeerConnection.PeerConnectionState.CLOSED -> RTCPeerConnectionState.CLOSED
                    }
                }

                // ... other observer methods
            }
        ) ?: throw RuntimeException("Failed to create peer connection")
    }

    override suspend fun createOffer(): SessionDescription = suspendCoroutine { continuation ->
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: org.webrtc.SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        continuation.resume(SessionDescription(sdp.description, sdp.type.canonicalForm()))
                    }
                    override fun onSetFailure(error: String) {
                        continuation.resumeWithException(Exception("Set local description failed: $error"))
                    }
                    override fun onCreateSuccess(p0: org.webrtc.SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }

            override fun onCreateFailure(error: String) {
                continuation.resumeWithException(Exception("Create offer failed: $error"))
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    // ... implement other methods
}
```

Similar implementation for iOS in `WebRTCPeerConnection.ios.kt` using native WebRTC framework.

### Phase 4: WebRTC Integration Layer (Week 6)

**4.1 Create WebRTC Connection Manager**

File: `composeApp/src/commonMain/kotlin/io/music_assistant/client/webrtc/WebRTCConnectionManager.kt`
```kotlin
class WebRTCConnectionManager(
    private val settings: SettingsRepository
) : CoroutineScope {
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + supervisorJob

    private val logger = Logger.withTag("WebRTCConnectionManager")

    private val signalingClient = SignalingClient()
    private var peerConnection: WebRTCPeerConnection? = null

    private var apiDataChannel: WebRTCDataChannel? = null
    private var sendspinDataChannel: WebRTCDataChannel? = null

    private val _connectionState = MutableStateFlow<WebRTCConnectionState>(WebRTCConnectionState.Idle)
    val connectionState: StateFlow<WebRTCConnectionState> = _connectionState.asStateFlow()

    private val _apiMessages = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val apiMessages: Flow<String> = _apiMessages.asSharedFlow()

    private var currentSessionId: String? = null

    suspend fun connect(remoteId: RemoteId) {
        logger.i { "Connecting to remote server: ${remoteId.formatted}" }

        try {
            // Step 1: Connect to signaling server
            _connectionState.value = WebRTCConnectionState.ConnectingToSignaling
            signalingClient.connect()

            // Step 2: Request connection to remote ID
            signalingClient.sendMessage(SignalingMessage.Connect(remoteId.rawId))

            // Step 3: Listen for signaling messages
            launch { handleSignalingMessages() }

        } catch (e: Exception) {
            logger.e(e) { "Failed to connect via WebRTC" }
            _connectionState.value = WebRTCConnectionState.Error(
                WebRTCError.SignalingError(e.message ?: "Connection failed")
            )
        }
    }

    private suspend fun handleSignalingMessages() {
        signalingClient.incomingMessages.collect { message ->
            when (message) {
                is SignalingMessage.SessionReady -> handleSessionReady(message)
                is SignalingMessage.Answer -> handleAnswer(message)
                is SignalingMessage.IceCandidate -> handleIceCandidate(message)
                is SignalingMessage.Error -> handleError(message)
                is SignalingMessage.ClientDisconnected -> handleDisconnected(message)
                else -> {}
            }
        }
    }

    private suspend fun handleSessionReady(message: SignalingMessage.SessionReady) {
        logger.i { "Session ready: ${message.sessionId}" }
        currentSessionId = message.sessionId

        // Initialize peer connection with ICE servers
        val peer = createPeerConnection()
        peer.initialize(message.iceServers)
        peerConnection = peer

        // Set up ICE candidate callback
        peer.onIceCandidate { candidate ->
            launch {
                signalingClient.sendMessage(
                    SignalingMessage.IceCandidate(
                        sessionId = currentSessionId!!,
                        data = candidate
                    )
                )
            }
        }

        // Set up data channel callback (gateway will create channels)
        peer.onDataChannel { dataChannel ->
            when (dataChannel.label) {
                "ma-api", "" -> {
                    logger.i { "MA-API data channel received" }
                    setupApiDataChannel(dataChannel)
                }
                "sendspin" -> {
                    logger.i { "Sendspin data channel received" }
                    sendspinDataChannel = dataChannel
                    setupSendspinDataChannel(dataChannel)
                }
            }
        }

        // Create and send offer
        _connectionState.value = WebRTCConnectionState.NegotiatingPeerConnection(message.sessionId)
        val offer = peer.createOffer()

        signalingClient.sendMessage(
            SignalingMessage.Offer(
                sessionId = message.sessionId,
                data = offer
            )
        )
    }

    private suspend fun handleAnswer(message: SignalingMessage.Answer) {
        logger.i { "Received SDP answer" }
        peerConnection?.setRemoteDescription(message.data)
    }

    private suspend fun handleIceCandidate(message: SignalingMessage.IceCandidate) {
        logger.d { "Received ICE candidate" }
        peerConnection?.addIceCandidate(message.data)
    }

    private fun setupApiDataChannel(channel: WebRTCDataChannel) {
        apiDataChannel = channel

        channel.onOpen {
            logger.i { "MA-API data channel open" }
            val sessionId = currentSessionId ?: return@onOpen
            val remoteId = RemoteId(settings.webrtcRemoteId.value)
            _connectionState.value = WebRTCConnectionState.Connected(sessionId, remoteId)
        }

        channel.onMessage { data ->
            val message = data.decodeToString()
            logger.d { "API message received: $message" }
            launch { _apiMessages.emit(message) }
        }

        channel.onClose {
            logger.i { "MA-API data channel closed" }
            _connectionState.value = WebRTCConnectionState.Idle
        }
    }

    private fun setupSendspinDataChannel(channel: WebRTCDataChannel) {
        channel.onMessage { data ->
            // Forward to Sendspin handler
            // TODO: Integration with SendspinClient
        }
    }

    suspend fun sendApiMessage(message: String) {
        apiDataChannel?.send(message) ?: throw IllegalStateException("Not connected")
    }

    suspend fun sendSendspinMessage(data: ByteArray) {
        sendspinDataChannel?.send(data) ?: throw IllegalStateException("Sendspin channel not ready")
    }

    suspend fun disconnect() {
        logger.i { "Disconnecting WebRTC" }

        apiDataChannel?.close()
        sendspinDataChannel?.close()
        peerConnection?.close()
        signalingClient.disconnect()

        _connectionState.value = WebRTCConnectionState.Idle
    }

    fun close() {
        supervisorJob.cancel()
        signalingClient.close()
    }

    private fun createPeerConnection(): WebRTCPeerConnection {
        // Platform-specific implementation via expect/actual
        return WebRTCPeerConnectionImpl()
    }
}
```

### Phase 5: ServiceClient Integration (Week 7)

**5.1 Extend ConnectionInfo Model**

Update `ConnectionInfo.kt`:
```kotlin
sealed class ConnectionMode {
    data class Direct(
        val host: String,
        val port: Int,
        val isTls: Boolean
    ) : ConnectionMode()

    data class WebRTC(
        val remoteId: RemoteId
    ) : ConnectionMode()
}

// Update existing usage to use ConnectionMode.Direct
```

**5.2 Modify ServiceClient**

Update `ServiceClient.kt`:
```kotlin
class ServiceClient(
    private val settings: SettingsRepository,
    private val webrtcManager: WebRTCConnectionManager
) : CoroutineScope {

    // ... existing code

    fun connect(connectionMode: ConnectionMode) {
        when (connectionMode) {
            is ConnectionMode.Direct -> connectDirect(connectionMode)
            is ConnectionMode.WebRTC -> connectWebRTC(connectionMode)
        }
    }

    private fun connectDirect(mode: ConnectionMode.Direct) {
        // Existing WebSocket connection logic
        // ...
    }

    private fun connectWebRTC(mode: ConnectionMode.WebRTC) {
        launch {
            try {
                _sessionState.value = SessionState.Connecting

                // Connect via WebRTC
                webrtcManager.connect(mode.remoteId)

                // Monitor WebRTC connection state
                launch {
                    webrtcManager.connectionState.collect { state ->
                        when (state) {
                            is WebRTCConnectionState.Connected -> {
                                // WebRTC connected - now handle API messages
                                _sessionState.value = SessionState.Connected(
                                    connectionInfo = null, // WebRTC mode
                                    serverInfo = null,
                                    user = null,
                                    dataConnectionState = DataConnectionState.AwaitingServerInfo
                                )

                                // Start listening for API messages
                                launch { listenForWebRTCMessages() }

                                // Request server info
                                sendRequest(Request.Server.info())
                            }

                            is WebRTCConnectionState.Error -> {
                                _sessionState.value = SessionState.Disconnected.Error(
                                    "WebRTC connection failed: ${state.error}"
                                )
                            }

                            else -> {}
                        }
                    }
                }

            } catch (e: Exception) {
                logger.e(e) { "WebRTC connection failed" }
                _sessionState.value = SessionState.Disconnected.Error(e.message ?: "Connection failed")
            }
        }
    }

    private suspend fun listenForWebRTCMessages() {
        webrtcManager.apiMessages.collect { message ->
            // Same message handling as WebSocket
            val jsonMessage = myJson.decodeFromString<JsonObject>(message)
            handleIncomingMessage(jsonMessage)
        }
    }

    // Modify sendRequest to support both WebSocket and WebRTC
    suspend fun sendRequest(request: Request): ApiResult {
        return when (val state = _sessionState.value) {
            is SessionState.Connected -> {
                val message = buildRequestMessage(request)

                // Send via WebSocket or WebRTC based on connection mode
                if (webrtcManager.connectionState.value is WebRTCConnectionState.Connected) {
                    webrtcManager.sendApiMessage(message)
                } else {
                    // Existing WebSocket send
                    session?.send(message)
                }

                // Wait for response (same logic)
                suspendCoroutine { continuation ->
                    pendingResponses[request.messageId] = { answer ->
                        continuation.resume(answer)
                    }
                }
            }
            else -> ApiResult.Error("Not connected")
        }
    }
}
```

### Phase 6: UI Integration (Week 8)

**6.1 Update Settings Screen**

Add WebRTC settings section to `SettingsScreen.kt`:

```kotlin
@Composable
fun WebRTCSection(
    enabled: Boolean,
    remoteId: String,
    connectionState: WebRTCConnectionState,
    onEnabledChanged: (Boolean) -> Unit,
    onRemoteIdChanged: (String) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier) {
        SectionTitle("WebRTC Remote Access")

        Text(
            text = "Connect from anywhere without port forwarding",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable WebRTC")
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )
        }

        if (enabled) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = remoteId,
                onValueChange = onRemoteIdChanged,
                label = { Text("Remote ID") },
                placeholder = { Text("MA-XXXX-XXXX") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Connection status
            when (connectionState) {
                is WebRTCConnectionState.Connected -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Connected via WebRTC")
                    }
                }
                is WebRTCConnectionState.Error -> {
                    Text(
                        text = "Error: ${connectionState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                enabled = remoteId.isNotBlank()
            ) {
                Text("Connect via WebRTC")
            }
        }
    }
}
```

**6.2 Update SettingsViewModel**

```kotlin
class SettingsViewModel(
    private val serviceClient: ServiceClient,
    private val settings: SettingsRepository,
    private val webrtcManager: WebRTCConnectionManager
) : ViewModel() {

    val webrtcEnabled = settings.webrtcEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val webrtcRemoteId = settings.webrtcRemoteId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val webrtcConnectionState = webrtcManager.connectionState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WebRTCConnectionState.Idle
    )

    fun updateWebRTCEnabled(enabled: Boolean) {
        settings.updateWebRTCEnabled(enabled)
    }

    fun updateWebRTCRemoteId(remoteId: String) {
        settings.updateWebRTCRemoteId(remoteId)
    }

    fun connectViaWebRTC() {
        viewModelScope.launch {
            val remoteId = RemoteId.parse(webrtcRemoteId.value)
            if (remoteId != null) {
                serviceClient.connect(ConnectionMode.WebRTC(remoteId))
            }
        }
    }
}
```

### Phase 7: Sendspin Integration (Week 9)

**7.1 Update SendspinClient for WebRTC**

The Sendspin channel needs to be integrated with the existing `SendspinClient`:

```kotlin
class SendspinClient(
    private val config: SendspinConfig,
    private val mediaPlayerController: MediaPlayerController,
    private val webrtcDataChannel: WebRTCDataChannel? = null // For WebRTC mode
) : CoroutineScope {

    // Modify connectToServer to support WebRTC mode
    private suspend fun connectToServer(serverUrl: String) {
        if (webrtcDataChannel != null) {
            // WebRTC mode - use data channel instead of WebSocket
            setupWebRTCDataChannel()
        } else {
            // Direct mode - use WebSocket (existing code)
            // ...
        }
    }

    private suspend fun setupWebRTCDataChannel() {
        logger.i { "Setting up Sendspin via WebRTC data channel" }

        // Create message dispatcher for WebRTC mode
        // Instead of SendspinWsHandler, use WebRTCDataChannelHandler
        val channelHandler = WebRTCDataChannelHandler(webrtcDataChannel!!)

        // ... rest of setup similar to WebSocket mode
    }
}

// New handler for WebRTC data channel
class WebRTCDataChannelHandler(
    private val dataChannel: WebRTCDataChannel
) {
    private val _binaryMessages = MutableSharedFlow<ByteArray>(extraBufferCapacity = 100)
    val binaryMessages: Flow<ByteArray> = _binaryMessages.asSharedFlow()

    init {
        dataChannel.onMessage { data ->
            launch { _binaryMessages.emit(data) }
        }
    }

    suspend fun send(data: ByteArray) {
        dataChannel.send(data)
    }
}
```

### Phase 8: Testing & Refinement (Week 10)

**8.1 Testing Checklist**

- [ ] WebRTC connection establishment with valid Remote ID
- [ ] Invalid Remote ID error handling
- [ ] Network disconnection and reconnection
- [ ] ICE gathering and candidate exchange
- [ ] Data channel creation and message forwarding
- [ ] API commands over WebRTC (get players, queue actions, etc.)
- [ ] Sendspin audio streaming over WebRTC data channel
- [ ] STUN-only connectivity (home networks)
- [ ] TURN relay connectivity (corporate networks, mobile)
- [ ] Switching between Direct and WebRTC modes
- [ ] Connection state transitions and UI updates
- [ ] Error messages and user feedback
- [ ] Performance and latency measurements
- [ ] Memory usage and resource cleanup

**8.2 Known Limitations**

- **Platform Support**: WebRTC libraries may have different maturity levels on Android vs iOS
- **Audio Latency**: WebRTC data channel may have higher latency than direct WebSocket
- **Battery Usage**: WebRTC connections may consume more battery than direct connections
- **Network Requirements**: STUN servers may not work in all network configurations

## Dependencies

### Kotlin Multiplatform WebRTC

**Option 1: kotlinx-webrtc** (if available)
- Pros: Unified API, KMP-first design
- Cons: May not exist yet (need to verify)

**Option 2: Platform-specific wrappers**
- Android: `org.webrtc:google-webrtc:1.0.42`
- iOS: Native `WebRTC.framework` via CocoaPods
- Use expect/actual pattern for abstraction

### Recommended: webrtc-kmp library

Check for community KMP WebRTC libraries or create thin wrappers around native libraries.

## Security Considerations

### DTLS Encryption

All WebRTC data channels use DTLS-SRTP encryption:
- **Protocol**: DTLS 1.2+ with ECDHE key exchange
- **Ciphers**: AES-GCM preferred
- **Certificate**: Server uses persistent ECDSA certificate
- **Verification**: Client can pin server certificate fingerprint

### Authentication

Same token-based authentication as WebSocket:
```json
{
  "command": "auth/authorize",
  "token": "eyJhbGc..."
}
```

### Signaling Server Trust

The signaling server:
- **Cannot decrypt**: Data is end-to-end encrypted (DTLS)
- **Cannot inspect**: Only routes signaling messages
- **Can observe**: Connection metadata (Remote ID, timing, IP addresses)

### Certificate Pinning

For enhanced security:
```kotlin
val expectedFingerprint = "SHA-256 AB:CD:EF:12:34:56:..."
val actualFingerprint = peerConnection.getRemoteCertificateFingerprint()
if (actualFingerprint != expectedFingerprint) {
    throw SecurityException("Certificate fingerprint mismatch")
}
```

## Performance Optimization

### Connection Establishment

**Fast Path** (optimal):
```
User enters Remote ID → Connect → Session Ready → Offer → Answer → ICE → Connected
Total: ~2-5 seconds
```

**Slow Path** (TURN relay required):
```
User enters Remote ID → Connect → Session Ready → Offer → Answer →
ICE STUN fails → ICE TURN → Connected
Total: ~5-15 seconds
```

### Message Throughput

**WebRTC Data Channel**:
- Reliable, ordered delivery (SCTP)
- Max message size: 256 KB (fragmentation automatic)
- Recommended: Keep messages < 16 KB for best performance

**Sendspin Audio Streaming**:
- Binary messages (Opus/FLAC encoded chunks)
- Typical chunk size: 4-16 KB
- Frequency: Every 10-20ms during playback
- Buffer management: Same as direct WebSocket

### Battery Impact

WebRTC connections maintain persistent ICE connectivity checks:
- **Frequency**: ~1 second intervals
- **Impact**: Minimal (< 1% battery/hour)
- **Optimization**: Use ConnectionState monitoring to pause checks when backgrounded

## Monitoring & Debugging

### Connection State Logging

```kotlin
launch {
    webrtcManager.connectionState.collect { state ->
        Logger.i { "WebRTC State: $state" }

        // Report to analytics
        analytics.logEvent("webrtc_state_change", mapOf(
            "state" to state::class.simpleName,
            "timestamp" to Clock.System.now()
        ))
    }
}
```

### ICE Candidate Diagnostics

```kotlin
peerConnection.onIceCandidate { candidate ->
    Logger.d { "ICE Candidate: ${candidate.candidate}" }
    // Analyze candidate type: host, srflx (STUN), relay (TURN)
}
```

### Data Channel Metrics

```kotlin
dataChannel.onMessage { data ->
    val latency = Clock.System.now() - messageTimestamp
    Logger.d { "Message latency: ${latency.inWholeMilliseconds}ms" }
}
```

## Rollout Strategy

### Phase 1: Beta Testing
- Release to beta testers with WebRTC toggle in settings
- Monitor connection success rates
- Collect feedback on latency and reliability

### Phase 2: Gradual Rollout
- Enable for users who explicitly opt-in
- A/B test Direct vs WebRTC default
- Monitor crash rates and error reports

### Phase 3: General Availability
- Make WebRTC the default for remote connections
- Keep Direct mode as fallback/option
- Document setup instructions

## Future Enhancements

### Automatic Mode Selection

Detect whether user is on local network or remote:
```kotlin
suspend fun detectConnectionMode(): ConnectionMode {
    // Try direct connection first
    val directResult = tryDirectConnection(host, port)
    if (directResult.isSuccess) {
        return ConnectionMode.Direct(host, port, tls)
    }

    // Fallback to WebRTC
    return ConnectionMode.WebRTC(remoteId)
}
```

### Connection Quality Monitoring

```kotlin
data class ConnectionQuality(
    val latency: Duration,
    val packetLoss: Double,
    val jitter: Duration,
    val bandwidth: ByteRate
)
```

### QR Code for Remote ID

Generate QR code containing Remote ID for easy sharing:
```kotlin
fun generateRemoteIdQRCode(remoteId: RemoteId): Bitmap {
    // Use QR code library
    return QRCode.generate(remoteId.formatted)
}
```

### Smart Reconnection

Prefer last successful connection mode:
```kotlin
val lastSuccessfulMode = settings.lastConnectionMode.value
connect(lastSuccessfulMode ?: detectConnectionMode())
```

## References

### Music Assistant Server Implementation

- **Gateway**: `/music_assistant/controllers/webserver/remote_access/gateway.py` (843 lines)
- **Certificate**: `/music_assistant/helpers/webrtc_certificate.py` (211 lines)
- **Manager**: `/music_assistant/controllers/webserver/remote_access/__init__.py` (280 lines)

### WebRTC Resources

- [WebRTC API - MDN](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API)
- [WebRTC for the Curious](https://webrtcforthecurious.com/)
- [STUN/TURN Server Configuration](https://www.nabto.com/understanding-stun-servers-in-webrtc-and-iot/)
- [aiortc Documentation](https://aiortc.readthedocs.io/)

### Related Documentation

- `.claude/architecture.md` - Current architecture patterns
- `.claude/sendspin-status.md` - Sendspin protocol details
- `.claude/settings-screen.md` - Settings UI patterns

---

**Document Version**: 1.0
**Last Updated**: 2026-02-05
**Status**: Planning Phase - Not Implemented
