package io.music_assistant.client.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.music_assistant.client.utils.myJson
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for WebRTC components.
 *
 * Currently unused - will be activated in Phase 3 when WebRTCConnectionManager is implemented.
 *
 * To activate: Add `webrtcModule` to the modules list in initKoin.kt
 */
val webrtcModule = module {
    // Shared HttpClient for WebRTC signaling
    // Uses default engine (platform-specific: CIO on JVM/Android, Darwin on iOS)
    // Configured with WebSockets support for signaling server connection
    single(named("webrtcHttpClient")) {
        HttpClient {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(myJson)
            }
        }
    }

    // SignalingClient factory
    // Not registered yet - will be created by WebRTCConnectionManager in Phase 3
    // Example usage when WebRTCConnectionManager is implemented:
    // factory { SignalingClient(get(named("webrtcHttpClient")), scope, signalingUrl) }
}
