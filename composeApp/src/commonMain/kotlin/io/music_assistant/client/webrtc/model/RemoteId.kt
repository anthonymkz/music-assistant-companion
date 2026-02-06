package io.music_assistant.client.webrtc.model

/**
 * Unique identifier for a Music Assistant server instance.
 *
 * Generated from the server's DTLS certificate SHA-256 fingerprint:
 * 1. Truncate to first 128 bits (16 bytes)
 * 2. Base32 encode (with '9' instead of '2')
 * 3. Result: 26-character uppercase alphanumeric string
 *
 * Example: "VVPN3TLP34YMGIZDINCEKQKSIR" (raw)
 *          "MA-VVPN-3TLP" (formatted for display)
 */
data class RemoteId(val rawId: String) {
    init {
        require(rawId.matches(Regex("[A-Z0-9]{8,26}"))) {
            "Invalid Remote ID: must be 8-26 uppercase alphanumeric characters, got: $rawId"
        }
    }
    /**
     * User-facing formatted Remote ID with "MA-" prefix and hyphens.
     * Format: MA-XXXX-XXXX (first 8 characters for display)
     */
    val formatted: String
        get() {
            val displayPart = rawId.take(8)
            return "MA-${displayPart.substring(0, 4)}-${displayPart.substring(4, 8)}"
        }

    /**
     * Full formatted Remote ID showing all characters.
     * Format: MA-XXXX-XXXX-XXXX-XXXX-XXXX-XX (full 26 characters)
     */
    val fullFormatted: String
        get() {
            return buildString {
                append("MA-")
                rawId.chunked(4).forEachIndexed { index, chunk ->
                    if (index > 0) append("-")
                    append(chunk)
                }
            }
        }

    companion object {
        /**
         * Parse Remote ID from user input.
         * Accepts formats:
         * - "MA-XXXX-XXXX" (short display format)
         * - "MA-XXXX-XXXX-..." (full format with hyphens)
         * - Raw alphanumeric string (8-26 characters)
         *
         * @return RemoteId if valid, null otherwise
         */
        fun parse(input: String): RemoteId? {
            // Remove "MA-" prefix and all hyphens, convert to uppercase
            val cleaned = input
                .replace(Regex("^MA-", RegexOption.IGNORE_CASE), "")
                .replace("-", "")
                .uppercase()

            // Validate: must be 8-26 alphanumeric characters
            return if (cleaned.matches(Regex("[A-Z0-9]{8,26}"))) {
                RemoteId(cleaned)
            } else {
                null
            }
        }

        /**
         * Validate if a string could be a valid Remote ID format.
         */
        fun isValid(input: String): Boolean {
            return parse(input) != null
        }
    }

    override fun toString(): String = formatted
}
