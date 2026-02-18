package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.utils.conditional
import io.music_assistant.client.ui.compose.common.HorizontalPagerIndicator
import io.music_assistant.client.ui.compose.common.OverflowMenu
import io.music_assistant.client.ui.compose.common.OverflowMenuOption

@Composable
fun PlayersTopBar(
    playerDataList: List<PlayerData>,
    playersState: HomeScreenViewModel.PlayersState.Data,
    playerPagerState: PagerState,
    onPlayersRefreshClick: () -> Unit,
    onItemMoved: ((Int) -> Unit)?,
    isTV: Boolean = false,
    onExpandClick: (() -> Unit)? = null,
    onMoveToPlayer: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .conditional(isTV, ifTrue = { padding(vertical = 8.dp) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.size(if (isTV) 48.dp else 32.dp),
            onClick = onPlayersRefreshClick
        ) {
            Icon(
                modifier = Modifier.size(if (isTV) 28.dp else 20.dp),
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh players",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (isTV) {
            Spacer(Modifier.width(4.dp))

            // TV: consolidated speaker chip using OverflowMenu (matches phone pattern)
            OverflowMenu(
                modifier = Modifier.weight(1f),
                buttonContent = { onClick ->
                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Speaker icon in a tinted circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = Icons.Filled.Speaker,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = playerDataList.getOrNull(playerPagerState.currentPage)
                                    ?.player?.displayName ?: "Speaker",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (playerDataList.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Select speaker",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                options = playerDataList.map { data ->
                    val isLocalPlayer = data.playerId == playersState.localPlayerId
                    OverflowMenuOption(
                        title = data.player.displayName + (if (isLocalPlayer) " (local)" else "")
                    ) {
                        onMoveToPlayer(data.player.id)
                    }
                }
            )

            if (onExpandClick != null) {
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = onExpandClick
                ) {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand player",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            Spacer(Modifier.width(4.dp))

            // Phone: speaker name is the picker — tappable Surface chip
            OverflowMenu(
                modifier = Modifier.weight(1f),
                buttonContent = { onClick ->
                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Speaker icon in a tinted circle
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Filled.Speaker,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = playerDataList.getOrNull(playerPagerState.currentPage)
                                    ?.player?.displayName ?: "Speaker",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (playerDataList.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Select speaker",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                options = playerDataList.map { data ->
                    val isLocalPlayer = data.playerId == playersState.localPlayerId
                    OverflowMenuOption(
                        title = data.player.displayName + (if (isLocalPlayer) " (local)" else "")
                    ) {
                        onMoveToPlayer(data.player.id)
                    }
                }
            )
        }
    }
}
