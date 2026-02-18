package io.music_assistant.client.ui.compose.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.music_assistant.client.utils.LocalPlatformType
import io.music_assistant.client.utils.PlatformType

@Composable
fun ActionButton(
    icon: ImageVector,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val isTV = LocalPlatformType.current == PlatformType.TV
    val primary = MaterialTheme.colorScheme.primary
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    IconButton(
        modifier = Modifier
            .then(
                if (isTV) Modifier.onFocusChanged { isFocused = it.isFocused }
                else Modifier
            )
            .then(
                if (isTV && isFocused) Modifier.border(2.5.dp, primary, shape)
                else Modifier
            )
            .alpha(if (enabled) 1F else 0.5f)
            .size(size),
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            modifier = Modifier.size(size),
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
    }
}