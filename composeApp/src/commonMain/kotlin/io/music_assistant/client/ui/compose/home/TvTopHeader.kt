package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.PlayableItem
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import io.music_assistant.client.ui.theme.HeaderFontFamily
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.mass
import org.jetbrains.compose.resources.painterResource

/**
 * Universal TV top header. Shows context art/text for the currently focused/selected item
 * (from Library grid, ItemDetails, etc). The now-playing info is only in the small widget top-right.
 */
@Composable
fun TvTopHeader(
    selectedDestination: TvNavDestination,
    onDestinationSelected: (TvNavDestination) -> Unit,
    headerFocusRequester: FocusRequester,
    contextArtUrl: String?,
    contextTitle: String?,
    contextSubtitle: String?,
    playerData: PlayerData?,
    serverUrl: String?,
    onNowPlayingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val track = playerData?.queueInfo?.currentItem?.track
    val player = playerData?.player

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background artwork — only right 2/3 of header, uses CONTEXT art (not now-playing)
        Crossfade(
            targetState = contextArtUrl,
            animationSpec = tween(500),
            label = "header_art",
        ) { url ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (url != null) {
                    // Art positioned in right 2/3 only
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.667f)
                            .align(Alignment.CenterEnd)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.55f)
                        )
                        // Left fade — art fades into bg
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0f to backgroundColor,
                                        0.3f to Color.Transparent,
                                    )
                                )
                        )
                        // Top fade
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to backgroundColor.copy(alpha = 0.7f),
                                        0.35f to Color.Transparent,
                                    )
                                )
                        )
                        // Bottom fade
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.55f to Color.Transparent,
                                        1f to backgroundColor,
                                    )
                                )
                        )
                        // NO right fade — art touches screen edge
                    }
                }
            }
        }

        // Top row: Brand left, Nav tabs center, Now Playing right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.mass),
                    contentDescription = "Music Assistant Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                            append("MASS")
                        }
                        append(" ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Light, fontSize = 17.sp)) {
                            append("Companion")
                        }
                    },
                    fontFamily = HeaderFontFamily,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.weight(1f))

            // Nav tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvNavDestination.entries.forEachIndexed { index, destination ->
                    TvNavTab(
                        destination = destination,
                        isSelected = destination == selectedDestination,
                        onClick = { onDestinationSelected(destination) },
                        focusRequester = if (index == 0) headerFocusRequester else null,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Now Playing widget (top-right) — always shows now-playing track
            if (player != null) {
                TvHeaderNowPlaying(
                    track = track,
                    player = player,
                    serverUrl = serverUrl,
                    onClick = onNowPlayingClick,
                )
            }
        }

        // Context info at bottom-left of header (shows focused/selected item, NOT now-playing)
        if (contextTitle != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 20.dp),
            ) {
                Text(
                    text = contextTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = HeaderFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                contextSubtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvNavTab(
    destination: TvNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .then(
                if (isFocused || isSelected) Modifier.border(2.5.dp, primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(20.dp),
                tint = defaultColor,
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = defaultColor,
            )
        }
    }
}

@Composable
private fun TvHeaderNowPlaying(
    track: PlayableItem?,
    player: Player,
    serverUrl: String?,
    onClick: () -> Unit,
) {
    val artUrl = track?.imageInfo?.url(serverUrl)
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .then(
                if (isFocused) Modifier.border(2.5.dp, primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small album art
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            if (artUrl != null) {
                val placeholder = rememberPlaceholderPainter(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Default.MusicNote,
                )
                AsyncImage(
                    model = artUrl,
                    contentDescription = track.name,
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder,
                    fallback = placeholder,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                text = track?.name ?: "No track",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).alpha(0.6f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = player.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
