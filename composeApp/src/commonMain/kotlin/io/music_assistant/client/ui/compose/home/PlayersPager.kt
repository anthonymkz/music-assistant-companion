package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
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
        if (isTV && !showQueue) {
            // TV mini mode: wrap top bar in a focusGroup with Down key interception
            // to explicitly jump focus into the pager content.
            // In expanded mode (showQueue), skip the top bar entirely — HomeScreen handles it.
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
        } else if (!isTV) {
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
                        MaterialTheme.colorScheme.surfaceContainer
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
                        // Only TV mini mode gets the opaque gradient background.
                        // Phone mini: floating semi-transparent player.
                        // Phone/TV expanded: album art shows through.
                        condition = isTV && !showQueue,
                        ifTrue = { background(brush = pageBrush) }
                    )
            ) {
                    // Phone or TV mini mode: existing layout
                    AnimatedVisibility(
                        visible = isQueueExpanded.takeIf { showQueue } != false,
                        enter = fadeIn(tween(200)) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
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

                    Column(
                        modifier = Modifier
                            .conditional(
                                condition = isQueueExpanded.takeIf { showQueue } == false,
                                ifTrue = { weight(1f) },
                                ifFalse = { wrapContentHeight() }
                            )
                    ) {
                        AnimatedVisibility(
                            visible = isQueueExpanded.takeIf { showQueue } == false,
                            enter = fadeIn(tween(200)) + expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
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
                        && !isLocalPlayer
                    ) {
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
                    } else if (
                        showQueue
                        && isLocalPlayer
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            text = "use device buttons to adjust the volume",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
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
internal fun VolumeDialog(
    player: PlayerData,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentVolume by remember(player.player.currentVolume) {
        mutableStateOf(player.player.currentVolume ?: 0f)
    }

    val primary = MaterialTheme.colorScheme.primary
    val borderShape = RoundedCornerShape(12.dp)

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
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Large centered percentage
                Text(
                    text = "${currentVolume.toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Progress bar
                LinearProgressIndicator(
                    progress = { (currentVolume / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.size(52.dp)
                            .border(1.5.dp, primary.copy(alpha = 0.5f), borderShape)
                            .clip(borderShape),
                        onClick = { setVolume(currentVolume - 5f) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Volume down",
                            tint = primary
                        )
                    }

                    IconButton(
                        modifier = Modifier.size(52.dp)
                            .border(1.5.dp, primary.copy(alpha = 0.5f), borderShape)
                            .clip(borderShape),
                        onClick = { playerAction(player, PlayerAction.ToggleMute) },
                        enabled = player.player.canMute
                    ) {
                        Icon(
                            imageVector = if (player.player.volumeMuted)
                                Icons.AutoMirrored.Filled.VolumeMute
                            else
                                Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Toggle mute",
                            tint = primary
                        )
                    }

                    IconButton(
                        modifier = Modifier.size(52.dp)
                            .border(1.5.dp, primary.copy(alpha = 0.5f), borderShape)
                            .clip(borderShape),
                        onClick = { setVolume(currentVolume + 5f) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Volume up",
                            tint = primary
                        )
                    }
                }

                // Done button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .border(1.5.dp, primary, borderShape)
                        .clip(borderShape)
                        .padding(horizontal = 24.dp)
                ) {
                    Text("Done", color = primary)
                }
            }
        },
        confirmButton = {},
    )
}
