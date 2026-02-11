package io.music_assistant.client.ui.compose.common.items

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.PlayableItem
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import io.music_assistant.client.ui.compose.common.painters.rememberVinylRecordPainter
import io.music_assistant.client.ui.compose.common.painters.rememberWaveformPainter

/**
 * Track media item with waveform overlay.
 *
 * @param item The track item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemTrack(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    serverUrl: String?,
    onClick: (PlayableItem) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            TrackImage(itemSize, item, serverUrl)
            (item as? AppMediaItem)?.let {
                Badges(
                    item = it,
                    providerIconFetcher = providerIconFetcher
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(itemSize),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TrackImage(
    itemSize: Dp,
    item: PlayableItem,
    serverUrl: String?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
            .background(primaryContainer)
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.MusicNote
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Draw waveform overlay at the bottom
        val waveformPainter = rememberWaveformPainter(primary)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.BottomCenter)
        ) {
            with(waveformPainter) {
                draw(size)
            }
        }
    }
}

/**
 * Artist media item with circular image.
 *
 * @param item The artist item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemArtist(
    modifier: Modifier = Modifier,
    item: AppMediaItem.Artist,
    serverUrl: String?,
    onClick: (AppMediaItem.Artist) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            ArtistImage(itemSize, item, serverUrl)
            Badges(
                item = item,
                providerIconFetcher = providerIconFetcher
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ArtistImage(
    itemSize: Dp,
    item: AppMediaItem.Artist,
    serverUrl: String?
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(CircleShape)
            .background(primaryContainer)
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.Mic
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Album media item with vinyl record design.
 *
 * @param item The album item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemAlbum(
    modifier: Modifier = Modifier,
    item: AppMediaItem.Album,
    serverUrl: String?,
    onClick: (AppMediaItem.Album) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            AlbumImage(itemSize, item, serverUrl)
            Badges(
                item = item,
                providerIconFetcher = providerIconFetcher
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun AlbumImage(
    itemSize: Dp,
    item: AppMediaItem.Album,
    serverUrl: String?
) {
    val primaryContainer = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val vinylRecord = rememberVinylRecordPainter(
            backgroundColor = background,
            labelColor = primaryContainer,
        )
        val stripWidth = 10.dp
        val holeRadius = 16.dp

        val cutStripShape = remember(stripWidth) { CutStripShape(stripWidth) }
        val holeShape = remember(holeRadius) { HoleShape(holeRadius) }

        Image(
            painter = vinylRecord,
            contentDescription = "Vinyl Record",
            modifier = Modifier.fillMaxSize().clip(CircleShape)
        )

        AsyncImage(
            placeholder = vinylRecord,
            fallback = vinylRecord,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(cutStripShape)
                .clip(holeShape)
        )
    }
}

/**
 * Playlist media item.
 *
 * @param item The playlist item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemPlaylist(
    modifier: Modifier = Modifier,
    item: AppMediaItem.Playlist,
    serverUrl: String?,
    onClick: (AppMediaItem.Playlist) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)? = null
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            PlaylistImage(itemSize, item, serverUrl)
            Badges(
                item = item,
                providerIconFetcher = providerIconFetcher
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PlaylistImage(
    itemSize: Dp,
    item: AppMediaItem.Playlist,
    serverUrl: String?
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val bindingWidth = 10.dp
        val notebookCutShape = remember(bindingWidth) { NotebookCutShape(bindingWidth) }

        // Draw notebook cover background (clipped)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(notebookCutShape)
                .background(primaryContainer)
        )

        // Draw binding ellipses in the cut area
        val ellipseRadius = 4.dp
        val ellipseCount = 7
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val ellipseRadiusPx = ellipseRadius.toPx()
            val topPadding = ellipseRadiusPx * 2
            val bottomPadding = ellipseRadiusPx * 2
            val availableHeight = size.height - topPadding - bottomPadding
            val spacing = if (ellipseCount > 1) {
                availableHeight / (ellipseCount - 1)
            } else {
                0f
            }

            // Draw ellipses in the binding strip area
            for (i in 0 until ellipseCount) {
                val y = topPadding + (i * spacing)
                drawCircle(
                    color = primary,
                    radius = ellipseRadiusPx,
                    center = Offset(x = bindingWidth.toPx() / 2f, y = y)
                )
            }
        }

        // Draw artwork (clipped)
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.AutoMirrored.Filled.FeaturedPlayList
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(notebookCutShape)
        )
    }
}

@Composable
fun MediaItemPodcast(
    modifier: Modifier = Modifier,
    item: AppMediaItem.Podcast,
    serverUrl: String?,
    onClick: (AppMediaItem.Podcast) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)? = null
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            PodcastImage(itemSize, item, serverUrl)
            Badges(
                item = item,
                providerIconFetcher = providerIconFetcher
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PodcastImage(
    itemSize: Dp,
    item: AppMediaItem.Podcast,
    serverUrl: String?
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val cutSize = itemSize / 3
        val cornerCutShape = remember(cutSize) { CornerCutShape(cutSize) }

        // Draw background (clipped)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cornerCutShape)
                .background(primaryContainer)
        )

        // Draw concentric circles in the cut corner area (centered on cut edge, rippling outward)
        val circleCount = 10
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = cutSize.toPx() * 0.7f
            val spacing = center / circleCount

            for (i in 1 until circleCount) {
                drawCircle(
                    color = primary,
                    radius = i * spacing,
                    center = Offset(center, center),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Draw artwork (clipped)
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.Podcasts
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(cornerCutShape)
        )
    }
}

@Composable
fun MediaItemPodcastEpisode(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    serverUrl: String?,
    onClick: (PlayableItem) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            PodcastEpisodeImage(itemSize, item, serverUrl)
            (item as? AppMediaItem)?.let {
                Badges(
                    item = item,
                    providerIconFetcher = providerIconFetcher
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(itemSize),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PodcastEpisodeImage(
    itemSize: Dp,
    item: PlayableItem,
    serverUrl: String?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
            .background(primaryContainer)
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.Podcasts
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Draw concentric circles from bottom center
        val circleCount = 8
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val bottomCenter = Offset(size.width / 2f, size.height)
            val maxRadius = size.height / 2f
            val spacing = maxRadius / circleCount

            for (i in 1..circleCount) {
                val alpha = 1f - (i.toFloat() / circleCount) // Fade as circles get bigger
                if (alpha > 0f) {
                    drawCircle(
                        color = primary.copy(alpha = alpha),
                        radius = i * spacing,
                        center = bottomCenter,
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

/**
 * Radio station media item with wavy octagon shape.
 *
 * @param item The radio station item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemRadio(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    serverUrl: String?,
    onClick: (PlayableItem) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
) {
    MediaItemWrapper(
        modifier = modifier,
        onClick = { onClick(item) }
    ) {
        Box {
            RadioImage(itemSize, item, serverUrl)
            (item as? AppMediaItem)?.let {
                Badges(
                    item = it,
                    providerIconFetcher = providerIconFetcher
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(itemSize),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun RadioImage(
    itemSize: Dp,
    item: PlayableItem,
    serverUrl: String?
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(WavyHexagonShape())
            .background(primaryContainer)
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.Radio
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Common wrapper for media items with click handling.
 */
@Composable
private fun MediaItemWrapper(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
fun BoxScope.Badges(
    item: AppMediaItem,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
) {
    val modifier =
        Modifier.align(Alignment.BottomEnd).size(16.dp)
    if (item.favorite == true) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Favorite",
            tint = Color(0xFFEF7BC4)
        )
    } else {
        providerIconFetcher?.invoke(modifier.background(Color.Gray, CircleShape), item.provider)
    }
}
