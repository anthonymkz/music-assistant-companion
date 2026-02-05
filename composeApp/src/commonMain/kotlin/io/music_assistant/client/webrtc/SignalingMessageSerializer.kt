package io.music_assistant.client.webrtc

import io.music_assistant.client.webrtc.model.SignalingMessage
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for SignalingMessage that uses the "type" field
 * to determine which subclass to deserialize.
 *
 * This is required because the signaling protocol uses a "type" discriminator field
 * rather than kotlinx.serialization's default class discriminator.
 */
object SignalingMessageSerializer : JsonContentPolymorphicSerializer<SignalingMessage>(SignalingMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SignalingMessage> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'type' field in signaling message")

        return when (type) {
            "connect" -> SignalingMessage.Connect.serializer()
            "session-ready" -> SignalingMessage.SessionReady.serializer()
            "offer" -> SignalingMessage.Offer.serializer()
            "answer" -> SignalingMessage.Answer.serializer()
            "ice-candidate" -> SignalingMessage.IceCandidate.serializer()
            "error" -> SignalingMessage.Error.serializer()
            "client-disconnected" -> SignalingMessage.ClientDisconnected.serializer()
            "registered" -> SignalingMessage.Registered.serializer()
            else -> throw IllegalArgumentException("Unknown signaling message type: $type")
        }
    }
}
