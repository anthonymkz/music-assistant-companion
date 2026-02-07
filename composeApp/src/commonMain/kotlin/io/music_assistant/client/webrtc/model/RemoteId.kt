package io.music_assistant.client.webrtc.model

/**
 * Unique identifier for a Music Assistant server instance.
 *
 * Generated from the server's DTLS certificate SHA-256 fingerprint:
 * 1. Truncate to first 128 bits (16 bytes)
 * 2. Base32 encode
 * 3. Result: 26-character uppercase alphanumeric string
 *
 * Example: "PGSVXKGZJCFA6MOH4UPBH5Q9HY" (raw)
 *          May be displayed with hyphens in various patterns by the server.
 */
data class RemoteId(val rawId: String) {
    init {
        require(rawId.matches(Regex("[A-Z0-9]{26}"))) {
            "Invalid Remote ID: must be exactly 26 uppercase alphanumeric characters, got: $rawId"
        }
    }

    companion object {
        /**
         * Parse Remote ID from user input.
         * Strips all hyphens and whitespace, converts to uppercase.
         *
         * @return RemoteId if valid, null otherwise
         */
        fun parse(input: String): RemoteId? {
            val cleaned = input
                .replace("-", "")
                .replace(" ", "")
                .uppercase()

            return if (cleaned.matches(Regex("[A-Z0-9]{26}"))) {
                RemoteId(cleaned)
            } else {
                null
            }
        }

        fun isValid(input: String): Boolean = parse(input) != null
    }

    override fun toString(): String = rawId
}
