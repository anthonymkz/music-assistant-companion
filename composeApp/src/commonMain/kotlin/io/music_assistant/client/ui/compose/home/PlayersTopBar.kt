package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
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
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.size(32.dp),
            onClick = onPlayersRefreshClick
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh players",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (isTV) {
            // On TV: show current speaker name + selector button + expand button
            var showSpeakerDialog by remember { mutableStateOf(false) }
            val currentPlayer = playerDataList.getOrNull(playerPagerState.currentPage)

            Text(
                text = currentPlayer?.player?.displayName ?: "No player",
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { showSpeakerDialog = true }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Speaker,
                    contentDescription = "Select speaker",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            if (onExpandClick != null) {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = onExpandClick
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand player",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (showSpeakerDialog) {
                SpeakerSelectorDialog(
                    players = playerDataList,
                    currentPlayerIndex = playerPagerState.currentPage,
                    localPlayerId = playersState.localPlayerId,
                    onPlayerSelected = { playerId ->
                        onMoveToPlayer(playerId)
                        showSpeakerDialog = false
                    },
                    onDismiss = { showSpeakerDialog = false }
                )
            }
        } else {
            HorizontalPagerIndicator(
                modifier = Modifier.weight(1f),
                pagerState = playerPagerState,
                onItemMoved = onItemMoved,
            )

            OverflowMenu(
                modifier = Modifier,
                buttonContent = { onClick ->
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onClick
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Filled.Speaker,
                            contentDescription = "Select player",
                            tint = MaterialTheme.colorScheme.primary,
                        )
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
