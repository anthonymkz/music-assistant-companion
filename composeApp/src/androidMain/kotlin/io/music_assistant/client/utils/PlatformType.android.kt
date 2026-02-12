package io.music_assistant.client.utils

import android.app.UiModeManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getPlatformType(): PlatformType {
    val context = LocalContext.current
    val uiModeManager = context.getSystemService(UiModeManager::class.java)
    return if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
        PlatformType.TV
    } else {
        PlatformType.PHONE
    }
}
