package io.music_assistant.client.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

enum class PlatformType {
    PHONE,
    TV
}

/**
 * Returns the current platform type.
 * On Android, detects TV via UiModeManager.
 * On iOS, always returns PHONE.
 */
@Composable
expect fun getPlatformType(): PlatformType

val LocalPlatformType = compositionLocalOf { PlatformType.PHONE }
