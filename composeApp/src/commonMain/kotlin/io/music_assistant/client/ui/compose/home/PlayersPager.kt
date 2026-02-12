package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.ui.compose.common.action.PlayerAction
import io.music_assistant.client.ui.compose.common.action.QueueAction
import io.music_assistant.client.utils.LocalPlatformType
import io.music_assistant.client.utils.PlatformType
import io.music_assistant.client.utils.conditional
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import io.music_assistant.client.utils.formatDuration
import kotlin.time.DurationUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayersPager(
    modifier: Modifier = Modifier,
    tvFocusRequester: FocusRequester? = null,
    playerPagerState: PagerState,
    playersState: HomeScreenViewModel.PlayersState.Data,
    serverUrl: String?,
    simplePlayerAction: (String, PlayerAction) -> Unit,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onPlayersRefreshClick: () -> Unit,
    onFavoriteClick: (AppMediaItem) -> Unit,
    showQueue: Boolean,
    isQueueExpanded: Boolean,
    onQueueExpandedSwitch: () -> Unit,
    onGoToLibrary: () -> Unit,
    onItemMoved: ((Int) -> Unit)?,
    queueAction: (QueueAction) -> Unit,
    settingsAction: (String) -> Unit,
    dspSettingsAction: (String) -> Unit,
    onNavigateUp: (() -> Unit)? = null,
    onExpandClick: (() -> Unit)? = null,
) {
    // Extract playerData list to ensure proper recomposition
    val playerDataList = playersState.playerData
    val coroutineScope = rememberCoroutineScope()

    fun moveToPlayer(playerId: String) {
        val targetIndex =
            playerDataList.indexOfFirst { it.player.id == playerId }
        if (targetIndex != -1) {
            coroutineScope.launch {
                playerPagerState.animateScrollToPage(targetIndex)
            }
        }
    }

    val isTV = LocalPlatformType.current == PlatformType.TV

    // Focus requesters for explicit TV focus wiring between top bar and pager content.
    // HorizontalPager is opaque to the focus system, so D-pad Down from the speaker
    // chips can't naturally reach the controls inside the pager. We wire it explicitly.
    val topBarAreaFocusRequester = remember { FocusRequester() }
    val playerControlsFocusRequester = remember { FocusRequester() }

    Column(modifier = modifier
        .conditional(tvFocusRequester != null, ifTrue = { focusRequester(tvFocusRequester!!) })
        .conditional(isTV, ifTrue = { focusGroup() })
    ) {
        if (isTV) {
            // TV: wrap top bar in a focusGroup with Down key interception
            // to explicitly jump focus into the pager content
            Box(
                modifier = Modifier
                    .focusRequester(topBarAreaFocusRequester)
                    .focusGroup()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    try {
                                        playerControlsFocusRequester.requestFocus()
                                    } catch (_: Exception) {
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    onNavigateUp?.invoke()
                                    onNavigateUp != null
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                PlayersTopBar(
                    playerDataList = playerDataList,
                    playersState = playersState,
                    playerPagerState = playerPagerState,
                    onPlayersRefreshClick = onPlayersRefreshClick,
                    onItemMoved = onItemMoved,
                    isTV = true,
                    onExpandClick = onExpandClick,
                ) { moveToPlayer(it) }
            }
        } else {
            PlayersTopBar(
                playerDataList = playerDataList,
                playersState = playersState,
                playerPagerState = playerPagerState,
                onPlayersRefreshClick = onPlayersRefreshClick,
                onItemMoved = onItemMoved,
                isTV = false,
            ) { moveToPlayer(it) }
        }

        HorizontalPager(
            modifier = Modifier
                .conditional(
                    condition = showQueue,
                    ifTrue = { weight(1f) },
                    ifFalse = { wrapContentHeight() }
                )
,
            state = playerPagerState,
            userScrollEnabled = !isTV,
            key = { page -> playerDataList.getOrNull(page)?.player?.id ?: page }
        ) { page ->

            val player = playerDataList.getOrNull(page) ?: return@HorizontalPager
            val isLocalPlayer = player.playerId == playersState.localPlayerId
            val isCurrentPage = page == playerPagerState.currentPage

            // Compute gradient outside modifier chain (conditional lambda isn't @Composable)
            val pageBrush = if (isLocalPlayer) {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Column(
                Modifier
                    .conditional(isTV && isCurrentPage, ifTrue = {
                        focusRequester(playerControlsFocusRequester)
                    })
                    .conditional(isTV, ifTrue = {
                        focusGroup()
                            .onPreviewKeyEvent { event ->
                                // In mini player mode only: Up jumps back to speaker chips.
                                // In expanded mode (showQueue), let Up navigate normally between controls.
                                if (!showQueue && event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                    try {
                                        topBarAreaFocusRequester.requestFocus()
                                    } catch (_: Exception) {
                                    }
                                    true
                                } else false
                            }
                    })
                    .conditional(
                        // TV expanded: transparent so album art background shows through
                        condition = !(isTV && showQueue),
                        ifTrue = { background(brush = pageBrush) }
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = player.player.displayName + (if (isLocalPlayer) " (local)" else ""),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

//                    // Overflow menu on the right TODO re-enable when settings are fixed in MA
//                    OverflowMenuThreeDots(
//                        modifier = Modifier.align(Alignment.CenterEnd)
//                            .padding(end = 8.dp),
//                        options = listOf(
//                            OverflowMenuOption(
//                                title = "Settings",
//                                onClick = { settingsAction(player.player.id) }
//                            ),
//                            OverflowMenuOption(
//                                title = "DSP settings",
//                                onClick = { dspSettingsAction(player.player.id) }
//                            ),
//                        )
//                    )
                }
                AnimatedVisibility(
                    // TV expanded mode: always show CompactPlayerItem
                    // (FullPlayerItem's progress Slider traps D-pad focus)
                    visible = if (isTV && showQueue) true
                    else isQueueExpanded.takeIf { showQueue } != false,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize()
                            .conditional(
                                showQueue && !isTV,
                                { clickable { onQueueExpandedSwitch() } }
                            )
                    ) {
                        CompactPlayerItem(
                            item = player,
                            serverUrl = serverUrl,
                            playerAction = playerAction,
                        )
                    }
                }

                // TV expanded: non-interactive progress bar + time labels
                if (isTV && showQueue) {
                    val track = player.queueInfo?.currentItem?.track
                    val duration = track?.duration?.takeIf { it > 0 }?.toFloat()
                    val elapsed = player.queueInfo?.elapsedTime?.toFloat() ?: 0f

                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                if (duration != null && duration > 0f)
                                    (elapsed / duration).coerceIn(0f, 1f)
                                else 0f
                            },
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = elapsed.takeIf { track != null }
                                    .formatDuration(DurationUnit.SECONDS)
                                    .takeIf { duration != null } ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = track?.let {
                                    duration?.formatDuration(DurationUnit.SECONDS) ?: "\u221E"
                                } ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .conditional(
                            condition = if (isTV && showQueue) false
                            else isQueueExpanded.takeIf { showQueue } == false,
                            ifTrue = { weight(1f) },
                            ifFalse = { wrapContentHeight() }
                        )
                ) {
                    AnimatedVisibility(
                        // TV expanded mode: never show FullPlayerItem
                        visible = if (isTV && showQueue) false
                        else isQueueExpanded.takeIf { showQueue } == false,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {

                        FullPlayerItem(
                            modifier = Modifier.fillMaxSize(),
                            item = player,
                            isLocal = isLocalPlayer,
                            serverUrl = serverUrl,
                            simplePlayerAction = simplePlayerAction,
                            playerAction = playerAction,
                            onFavoriteClick = onFavoriteClick,
                        )
                    }
                }

                if (
                    showQueue
                    && player.player.canSetVolume
                    && player.player.currentVolume != null
                ) {
                    if (isTV && !isLocalPlayer) {
                        // TV: volume button that opens a dialog with +/- controls
                        var showVolumeDialog by remember { mutableStateOf(false) }
                        val volumeLevel = player.player.currentVolume ?: 0f

                        Row(
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                .padding(horizontal = 64.dp)
                                .clickable { showVolumeDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = if (player.player.volumeMuted)
                                    Icons.AutoMirrored.Filled.VolumeMute
                                else
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "${volumeLevel.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showVolumeDialog) {
                            VolumeDialog(
                                player = player,
                                playerAction = playerAction,
                                onDismiss = { showVolumeDialog = false }
                            )
                        }
                    } else if (!isLocalPlayer) {
                        // Phone: existing volume slider
                        var currentVolume by remember(player.player.currentVolume) {
                            mutableStateOf(player.player.currentVolume)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                .padding(horizontal = 64.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .alpha(if (player.player.canMute) 1F else 0.5f)
                                    .clickable(enabled = player.player.canMute) {
                                        playerAction(player, PlayerAction.ToggleMute)
                                    },
                                imageVector = if (player.player.volumeMuted)
                                    Icons.AutoMirrored.Filled.VolumeMute
                                else
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                modifier = Modifier.weight(1f),
                                value = currentVolume,
                                valueRange = 0f..100f,
                                onValueChange = {
                                    currentVolume = it
                                },
                                onValueChangeFinished = {
                                    playerAction(
                                        player,
                                        if (player.groupChildren.none { it.isBound }) {
                                            PlayerAction.VolumeSet(currentVolume.toDouble())
                                        } else {
                                            PlayerAction.GroupVolumeSet(currentVolume.toDouble())
                                        }
                                    )
                                },
                                thumb = {
                                    SliderDefaults.Thumb(
                                        interactionSource = remember { MutableInteractionSource() },
                                        thumbSize = DpSize(16.dp, 16.dp),
                                        colors = SliderDefaults.colors()
                                            .copy(thumbColor = MaterialTheme.colorScheme.secondary),
                                    )
                                },
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        sliderState = sliderState,
                                        thumbTrackGapSize = 0.dp,
                                        trackInsideCornerSize = 0.dp,
                                        drawStopIndicator = null,
                                        modifier = Modifier.height(4.dp)
                                    )
                                }
                            )
                        }
                    } else {
                        Text(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            text = "use device buttons to adjust the volume",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))

                player.queue.takeIf { showQueue }?.let { queue ->
                    CollapsibleQueue(
                        modifier = Modifier
                            .conditional(
                                condition = isQueueExpanded,
                                ifTrue = { weight(1f) },
                                ifFalse = { wrapContentHeight() }
                            ),
                        queue = queue,
                        isQueueExpanded = isQueueExpanded,
                        onQueueExpandedSwitch = { onQueueExpandedSwitch() },
                        onGoToLibrary = onGoToLibrary,
                        serverUrl = serverUrl,
                        queueAction = queueAction,
                        players = playerDataList,
                        onPlayerSelected = { playerId ->
                            moveToPlayer(playerId)
                        },
                        isCurrentPage = page == playerPagerState.currentPage
                    )
                }
            }
        }

    }
}

@Composable
private fun VolumeDialog(
    player: PlayerData,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentVolume by remember(player.player.currentVolume) {
        mutableStateOf(player.player.currentVolume ?: 0f)
    }

    fun setVolume(newVolume: Float) {
        currentVolume = newVolume.coerceIn(0f, 100f)
        playerAction(
            player,
            if (player.groupChildren.none { it.isBound }) {
                PlayerAction.VolumeSet(currentVolume.toDouble())
            } else {
                PlayerAction.GroupVolumeSet(currentVolume.toDouble())
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Volume") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${currentVolume.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LinearProgressIndicator(
                    progress = { (currentVolume / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { setVolume(currentVolume - 5f) }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Volume down",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { playerAction(player, PlayerAction.ToggleMute) },
                        enabled = player.player.canMute
                    ) {
                        Icon(
                            imageVector = if (player.player.volumeMuted)
                                Icons.AutoMirrored.Filled.VolumeMute
                            else
                                Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Toggle mute",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { setVolume(currentVolume + 5f) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Volume up",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}
