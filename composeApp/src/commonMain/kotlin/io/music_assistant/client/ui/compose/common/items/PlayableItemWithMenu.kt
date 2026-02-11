package io.music_assistant.client.ui.compose.common.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAddCircle
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trash
import compose.icons.tablericons.FolderMinus
import compose.icons.tablericons.FolderPlus
import compose.icons.tablericons.Heart
import compose.icons.tablericons.HeartBroken
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.PlayableItem
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.viewmodel.ActionsViewModel
import kotlinx.coroutines.launch

@Composable
fun TrackWithMenu(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    itemSize: Dp = 96.dp,
    onTrackPlayOption: ((PlayableItem, QueueOption, Boolean) -> Unit),
    onItemClick: ((PlayableItem) -> Unit)? = null,
    playlistActions: ActionsViewModel.PlaylistActions? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?,
    serverUrl: String?,
) {
    PlayableItemWithMenu(
        modifier = modifier,
        item = item,
        itemSize = itemSize,
        onTrackPlayOption = onTrackPlayOption,
        onItemClick = onItemClick,
        playlistActions = playlistActions,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        libraryActions = libraryActions,
        providerIconFetcher = providerIconFetcher,
        serverUrl = serverUrl,
        itemComposable = { mod, itm, srvUrl, onClick, size, showSubtitle, iconFetcher ->
            MediaItemTrack(
                modifier = mod,
                item = itm,
                serverUrl = srvUrl,
                onClick = onClick,
                itemSize = size,
                showSubtitle = showSubtitle,
                providerIconFetcher = iconFetcher
            )
        }
    )
}

@Composable
fun EpisodeWithMenu(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    itemSize: Dp = 96.dp,
    onTrackPlayOption: ((PlayableItem, QueueOption, Boolean) -> Unit),
    onItemClick: ((PlayableItem) -> Unit)? = null,
    playlistActions: ActionsViewModel.PlaylistActions? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?,
    serverUrl: String?,
) {
    PlayableItemWithMenu(
        modifier = modifier,
        item = item,
        itemSize = itemSize,
        onTrackPlayOption = onTrackPlayOption,
        onItemClick = onItemClick,
        playlistActions = playlistActions,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        libraryActions = libraryActions,
        providerIconFetcher = providerIconFetcher,
        serverUrl = serverUrl,
        itemComposable = { mod, itm, srvUrl, onClick, size, showSubtitle, iconFetcher ->
            MediaItemPodcastEpisode(
                modifier = mod,
                item = itm,
                serverUrl = srvUrl,
                onClick = onClick,
                itemSize = size,
                showSubtitle = showSubtitle,
                providerIconFetcher = iconFetcher
            )
        }
    )
}

@Composable
fun RadioWithMenu(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    itemSize: Dp = 96.dp,
    onTrackPlayOption: ((PlayableItem, QueueOption, Boolean) -> Unit),
    onItemClick: ((PlayableItem) -> Unit)? = null,
    playlistActions: ActionsViewModel.PlaylistActions? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?,
    serverUrl: String?,
) {
    PlayableItemWithMenu(
        modifier = modifier,
        item = item,
        itemSize = itemSize,
        onTrackPlayOption = onTrackPlayOption,
        onItemClick = onItemClick,
        playlistActions = playlistActions,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        libraryActions = libraryActions,
        providerIconFetcher = providerIconFetcher,
        serverUrl = serverUrl,
        itemComposable = { mod, itm, srvUrl, onClick, size, showSubtitle, iconFetcher ->
            MediaItemRadio(
                modifier = mod,
                item = itm,
                serverUrl = srvUrl,
                onClick = onClick,
                itemSize = size,
                showSubtitle = showSubtitle,
                providerIconFetcher = iconFetcher
            )
        }
    )
}

/**
 * A reusable composable that displays a track item with a dropdown menu for queue actions.
 * When onTrackClick is provided, clicking the item opens a menu with play options.
 * Otherwise, it behaves as a simple clickable track item.
 */
@Composable
private fun PlayableItemWithMenu(
    modifier: Modifier = Modifier,
    item: PlayableItem,
    itemSize: Dp = 96.dp,
    onTrackPlayOption: ((PlayableItem, QueueOption, Boolean) -> Unit),
    onItemClick: ((PlayableItem) -> Unit)? = null,
    playlistActions: ActionsViewModel.PlaylistActions? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)?,
    serverUrl: String?,
    itemComposable: @Composable (
        modifier: Modifier,
        item: PlayableItem,
        serverUrl: String?,
        onClick: (PlayableItem) -> Unit,
        itemSize: Dp,
        showSubtitle: Boolean,
        providerIconFetcher: (@Composable (Modifier, String) -> Unit)?
    ) -> Unit
) {
    var expandedTrackId by remember { mutableStateOf<String?>(null) }
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<AppMediaItem.Playlist>>(emptyList()) }
    var isLoadingPlaylists by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        itemComposable(
            Modifier.align(Alignment.Center),
            item,
            serverUrl,
            { expandedTrackId = item.itemId },
            itemSize,
            true,
            providerIconFetcher
        )
        DropdownMenu(
            expanded = expandedTrackId == item.itemId,
            onDismissRequest = { expandedTrackId = null }
        ) {
            DropdownMenuItem(
                text = { Text("Play now") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.REPLACE, false)
                    expandedTrackId = null
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play now"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Insert next and play") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.PLAY, false)
                    expandedTrackId = null
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlaylistAddCircle,
                        contentDescription = "Insert next and play"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Insert next") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.NEXT, false)
                    expandedTrackId = null
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.QueuePlayNext,
                        contentDescription = "Insert next"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Add to bottom") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.ADD, false)
                    expandedTrackId = null
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AddToQueue,
                        contentDescription = "Add to bottom"
                    )
                }
            )
            if (item.canStartRadio) {
                DropdownMenuItem(
                    text = { Text("Start radio") },
                    onClick = {
                        onTrackPlayOption(item, QueueOption.REPLACE, true)
                        expandedTrackId = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Start radio"
                        )
                    }
                )
            }

            (item as? AppMediaItem)?.let {
                val libText = if (item.isInLibrary) "Remove from Library" else "Add to Library"
                DropdownMenuItem(
                    text = { Text(libText) },
                    onClick = {
                        libraryActions.onLibraryClick(item as AppMediaItem)
                        expandedTrackId = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                if (item.isInLibrary) TablerIcons.FolderMinus
                                else TablerIcons.FolderPlus,
                            contentDescription = libText
                        )
                    }
                )
            }


            // Favorite management (only for library items)
            if (item.isInLibrary) {
                val favText = if (item.favorite == true) "Unfavorite" else "Favorite"
                DropdownMenuItem(
                    text = { Text(favText) },
                    onClick = {
                        (item as? AppMediaItem)?.let {
                            libraryActions.onFavoriteClick(it)
                            expandedTrackId = null
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                if (item.favorite == true) TablerIcons.HeartBroken
                                else TablerIcons.Heart,
                            contentDescription = favText
                        )
                    }
                )
            }

            if (playlistActions != null && item is AppMediaItem.Track) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        showPlaylistDialog = true
                        expandedTrackId = null
                        // Load playlists when dialog opens
                        coroutineScope.launch {
                            isLoadingPlaylists = true
                            playlists = playlistActions.onLoadPlaylists()
                            isLoadingPlaylists = false
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist"
                        )
                    }
                )
            }
            if (onRemoveFromPlaylist != null) {
                DropdownMenuItem(
                    text = { Text("Remove from Playlist") },
                    onClick = {
                        onRemoveFromPlaylist()
                        expandedTrackId = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.Trash,
                            contentDescription = "Add to Playlist"
                        )
                    }
                )
            }
        }

        // Add to Playlist Dialog
        if (showPlaylistDialog && item is AppMediaItem.Track) {
            AlertDialog(
                onDismissRequest = {
                    showPlaylistDialog = false
                    playlists = emptyList()
                    isLoadingPlaylists = false
                },
                title = { Text("Add to Playlist") },
                text = {
                    if (isLoadingPlaylists) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (playlists.isEmpty()) {
                        Text("No editable playlists available")
                    } else {
                        Column {
                            playlists.forEach { playlist ->
                                TextButton(
                                    onClick = {
                                        playlistActions?.onAddToPlaylist
                                            ?.invoke(item, playlist)
                                        showPlaylistDialog = false
                                        playlists = emptyList()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = playlist.name,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        showPlaylistDialog = false
                        playlists = emptyList()
                        isLoadingPlaylists = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


