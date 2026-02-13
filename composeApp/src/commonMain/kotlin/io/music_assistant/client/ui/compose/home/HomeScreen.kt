@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.music_assistant.client.ui.compose.common.action.PlayerAction
import io.music_assistant.client.ui.compose.common.action.QueueAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import io.music_assistant.client.ui.theme.HeaderFontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.PlayableItem
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.providers.ProviderIcon
import io.music_assistant.client.ui.compose.common.rememberToastState
import io.music_assistant.client.ui.compose.common.viewmodel.ActionsViewModel
import io.music_assistant.client.ui.compose.home.nav.HomeNavScreen
import io.music_assistant.client.ui.compose.home.nav.rememberHomeNavBackStack
import io.music_assistant.client.ui.compose.item.ItemDetailsScreen
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.nav.BackHandler
import io.music_assistant.client.ui.compose.nav.NavScreen
import io.music_assistant.client.ui.compose.search.SearchScreen
import io.music_assistant.client.utils.LocalPlatformType
import io.music_assistant.client.utils.PlatformType
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.conditional
import kotlinx.coroutines.flow.collectLatest
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.mass
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = koinViewModel(),
    actionsViewModel: ActionsViewModel = koinViewModel(),
    navigateTo: (NavScreen) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val toastState = rememberToastState()

    LaunchedEffect(Unit) {
        viewModel.links.collectLatest { url -> uriHandler.openUri(url) }
    }

    // Collect toasts
    LaunchedEffect(Unit) {
        actionsViewModel.toasts.collect { toast ->
            toastState.showToast(toast)
        }
    }

    val isTV = LocalPlatformType.current == PlatformType.TV

    var showPlayersView by remember { mutableStateOf(false) }
    var isQueueExpanded by remember { mutableStateOf(false) }

    // Focus requesters for TV quick-navigation
    val miniPlayerFocusRequester = remember { FocusRequester() }
    val railFocusRequester = remember { FocusRequester() }

    val recommendationsState = viewModel.recommendationsState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val playersState by viewModel.playersState.collectAsStateWithLifecycle()
    // Single pager state used across all views
    val data = playersState as? HomeScreenViewModel.PlayersState.Data
    val playerPagerState = rememberPagerState(
        initialPage = data?.selectedPlayerIndex ?: 0,
        pageCount = { data?.playerData?.size ?: 0 }
    )

    // Nested navigation backstack - hoisted to survive player view transitions
    val homeBackStack = rememberHomeNavBackStack()

    // TV rail: derive which destination is selected from the back stack
    @Suppress("UNCHECKED_CAST")
    val currentRailDestination by remember(homeBackStack, showPlayersView) {
        derivedStateOf {
            if (showPlayersView) TvNavDestination.NowPlaying
            else when (homeBackStack.last()) {
                is HomeNavScreen.Library -> TvNavDestination.Library
                is HomeNavScreen.Search -> TvNavDestination.Search
                is HomeNavScreen.Landing -> TvNavDestination.Home
                else -> TvNavDestination.Home // ItemDetails keeps current context
            }
        }
    }

    // Handle back when player view is shown
    BackHandler(enabled = showPlayersView) {
        showPlayersView = false
    }

    // Update selected player when pager changes to load queue items
    LaunchedEffect(playerPagerState, playersState) {
        snapshotFlow { playerPagerState.currentPage }.collect { currentPage ->
            data?.playerData?.getOrNull(currentPage)?.let { playerData ->
                viewModel.selectPlayer(playerData.player)
            }
        }
    }

    Scaffold(
        modifier = Modifier.conditional(isTV, ifTrue = {
            onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Menu -> {
                            if (!showPlayersView) {
                                try {
                                    railFocusRequester.requestFocus()
                                } catch (_: Exception) {
                                }
                            }
                            true
                        }

                        Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                            data?.playerData?.getOrNull(playerPagerState.currentPage)
                                ?.player?.id?.let {
                                    viewModel.playerAction(it, PlayerAction.TogglePlayPause)
                                }
                            true
                        }

                        Key.MediaNext -> {
                            data?.playerData?.getOrNull(playerPagerState.currentPage)
                                ?.player?.id?.let {
                                    viewModel.playerAction(it, PlayerAction.Next)
                                }
                            true
                        }

                        Key.MediaPrevious -> {
                            data?.playerData?.getOrNull(playerPagerState.currentPage)
                                ?.player?.id?.let {
                                    viewModel.playerAction(it, PlayerAction.Previous)
                                }
                            true
                        }

                        else -> false
                    }
                } else false
            }
        }),
        topBar = {
            if (!isTV) {
                // Phone top bar — TV uses the navigation rail instead
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.mass),
                            contentDescription = "Music Assistant Logo",
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)) {
                                    append("MASS")
                                }
                                append(" ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Light, fontSize = 22.sp)) {
                                    append("Companion")
                                }
                            },
                            fontFamily = HeaderFontFamily,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { navigateTo(NavScreen.Settings) }
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        val connectionState = recommendationsState.value.connectionState
        val dataState = recommendationsState.value.recommendations
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isTV) {
                // TV: if/else to prevent focus leak between home and expanded player.
                if (!showPlayersView) {
                    // Main layout: left navigation rail + content + mini player
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val typedBackStack =
                            homeBackStack as NavBackStack<HomeNavScreen>

                        TvNavigationRail(
                            selectedDestination = currentRailDestination,
                            onDestinationSelected = { destination ->
                                when (destination) {
                                    TvNavDestination.Home -> {
                                        // Pop back to Landing
                                        while (typedBackStack.last() !is HomeNavScreen.Landing) {
                                            typedBackStack.removeLastOrNull() ?: break
                                        }
                                    }

                                    TvNavDestination.Library -> {
                                        typedBackStack.add(HomeNavScreen.Library(type = null))
                                    }

                                    TvNavDestination.Search -> {
                                        typedBackStack.add(HomeNavScreen.Search)
                                    }

                                    TvNavDestination.NowPlaying -> {
                                        showPlayersView = true
                                    }

                                    TvNavDestination.Settings -> {
                                        navigateTo(NavScreen.Settings)
                                    }
                                }
                            },
                            railFocusRequester = railFocusRequester,
                            modifier = Modifier.fillMaxHeight(),
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            HomeContent(
                                modifier = Modifier.weight(1f),
                                homeBackStack = homeBackStack,
                                connectionState = connectionState,
                                dataState = dataState,
                                serverUrl = serverUrl,
                                onRecommendationItemClick = viewModel::onRecommendationItemClicked,
                                onTrackPlayOption = viewModel::onTrackPlayOption,
                                playlistActions = ActionsViewModel.PlaylistActions(
                                    onLoadPlaylists = actionsViewModel::getEditablePlaylists,
                                    onAddToPlaylist = actionsViewModel::addToPlaylist
                                ),
                                libraryActions = ActionsViewModel.LibraryActions(
                                    onLibraryClick = actionsViewModel::onLibraryClick,
                                    onFavoriteClick = actionsViewModel::onFavoriteClick
                                ),
                                providerIconFetcher = { modifier, provider ->
                                    actionsViewModel.getProviderIcon(provider)
                                        ?.let { ProviderIcon(modifier, it) }
                                }
                            )

                            // Mini player area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .defaultMinSize(minHeight = 100.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Gradient accent line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                PlayersStateContent(
                                    playersState = playersState,
                                    pagerModifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    playerPagerState = playerPagerState,
                                    serverUrl = serverUrl,
                                    showQueue = false,
                                    isQueueExpanded = isQueueExpanded,
                                    onQueueExpandedSwitch = { isQueueExpanded = !isQueueExpanded },
                                    simplePlayerAction = { playerId, action -> viewModel.playerAction(playerId, action) },
                                    playerAction = { playerData, action -> viewModel.playerAction(playerData, action) },
                                    onPlayersRefreshClick = viewModel::refreshPlayers,
                                    onFavoriteClick = actionsViewModel::onFavoriteClick,
                                    onGoToLibrary = { showPlayersView = false },
                                    onItemMoved = null,
                                    queueAction = { action -> viewModel.queueAction(action) },
                                    settingsAction = viewModel::openPlayerSettings,
                                    dspSettingsAction = viewModel::openPlayerDspSettings,
                                    tvFocusRequester = miniPlayerFocusRequester,
                                    onNavigateUp = {
                                        try { railFocusRequester.requestFocus() } catch (_: Exception) {}
                                    },
                                )
                            }
                        }
                    }
                } else {
                    // Expanded player — full screen, no rail
                    val expandedFocusRequester = remember { FocusRequester() }
                    val closeBtnFocusRequester = remember { FocusRequester() }

                    // Album art background
                    val currentTrack = data?.playerData
                        ?.getOrNull(playerPagerState.currentPage)
                        ?.queueInfo?.currentItem?.track
                    val artUrl = currentTrack?.imageInfo?.url(serverUrl)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        // Album art background — drawn on top of solid background
                        if (artUrl != null) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().alpha(0.25f)
                            )
                        }

                        // Scrim gradient for text readability over album art
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .focusGroup()
                        ) {
                            LaunchedEffect(Unit) {
                                try {
                                    expandedFocusRequester.requestFocus()
                                } catch (_: Exception) {
                                }
                            }
                            // Close button with Down key wiring to pager content
                            IconButton(
                                onClick = { showPlayersView = false },
                                modifier = Modifier.fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .focusRequester(closeBtnFocusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                            try {
                                                expandedFocusRequester.requestFocus()
                                            } catch (_: Exception) {
                                            }
                                            true
                                        } else false
                                    }
                            ) {
                                Icon(
                                    Icons.Default.ExpandMore,
                                    "Collapse",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayersStateContent(
                                    playersState = playersState,
                                    pagerModifier = Modifier.fillMaxSize(),
                                    playerPagerState = playerPagerState,
                                    serverUrl = serverUrl,
                                    showQueue = true,
                                    isQueueExpanded = isQueueExpanded,
                                    onQueueExpandedSwitch = { isQueueExpanded = !isQueueExpanded },
                                    simplePlayerAction = { playerId, action -> viewModel.playerAction(playerId, action) },
                                    playerAction = { playerData, action -> viewModel.playerAction(playerData, action) },
                                    onPlayersRefreshClick = viewModel::refreshPlayers,
                                    onFavoriteClick = actionsViewModel::onFavoriteClick,
                                    onGoToLibrary = { showPlayersView = false },
                                    onItemMoved = { indexShift ->
                                        data?.let { d ->
                                            val currentPlayer = d.playerData[playerPagerState.currentPage].player
                                            val newIndex = (playerPagerState.currentPage + indexShift).coerceIn(0, d.playerData.size - 1)
                                            val newPlayers = d.playerData.map { it.player.id }.toMutableList().apply {
                                                add(newIndex, removeAt(playerPagerState.currentPage))
                                            }
                                            viewModel.selectPlayer(currentPlayer)
                                            viewModel.onPlayersSortChanged(newPlayers)
                                        }
                                    },
                                    queueAction = { action -> viewModel.queueAction(action) },
                                    settingsAction = viewModel::openPlayerSettings,
                                    dspSettingsAction = viewModel::openPlayerDspSettings,
                                    tvFocusRequester = expandedFocusRequester,
                                    onNavigateUp = {
                                        try { closeBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                // Phone: AnimatedContent for smooth slide transitions
                AnimatedContent(
                    targetState = showPlayersView,
                    transitionSpec = {
                        slideInVertically(
                            initialOffsetY = { if (targetState) it else -it },
                            animationSpec = tween(300)
                        ) togetherWith slideOutVertically(
                            targetOffsetY = { if (targetState) -it else it },
                            animationSpec = tween(300)
                        )
                    },
                    label = "player_transition"
                ) { isPlayersViewShown ->
                    if (!isPlayersViewShown) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            HomeContent(
                                modifier = Modifier.weight(1f),
                                homeBackStack = homeBackStack,
                                connectionState = connectionState,
                                dataState = dataState,
                                serverUrl = serverUrl,
                                onRecommendationItemClick = viewModel::onRecommendationItemClicked,
                                onTrackPlayOption = viewModel::onTrackPlayOption,
                                playlistActions = ActionsViewModel.PlaylistActions(
                                    onLoadPlaylists = actionsViewModel::getEditablePlaylists,
                                    onAddToPlaylist = actionsViewModel::addToPlaylist
                                ),
                                libraryActions = ActionsViewModel.LibraryActions(
                                    onLibraryClick = actionsViewModel::onLibraryClick,
                                    onFavoriteClick = actionsViewModel::onFavoriteClick
                                ),
                                providerIconFetcher = { modifier, provider ->
                                    actionsViewModel.getProviderIcon(provider)
                                        ?.let { ProviderIcon(modifier, it) }
                                }
                            )

                            // Mini player
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .defaultMinSize(minHeight = 100.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .clickable { showPlayersView = true }
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Gradient accent line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                PlayersStateContent(
                                    playersState = playersState,
                                    pagerModifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    playerPagerState = playerPagerState,
                                    serverUrl = serverUrl,
                                    showQueue = false,
                                    isQueueExpanded = isQueueExpanded,
                                    onQueueExpandedSwitch = { isQueueExpanded = !isQueueExpanded },
                                    simplePlayerAction = { playerId, action -> viewModel.playerAction(playerId, action) },
                                    playerAction = { playerData, action -> viewModel.playerAction(playerData, action) },
                                    onPlayersRefreshClick = viewModel::refreshPlayers,
                                    onFavoriteClick = actionsViewModel::onFavoriteClick,
                                    onGoToLibrary = { showPlayersView = false },
                                    onItemMoved = null,
                                    queueAction = { action -> viewModel.queueAction(action) },
                                    settingsAction = viewModel::openPlayerSettings,
                                    dspSettingsAction = viewModel::openPlayerDspSettings,
                                )
                            }
                        }
                    } else {
                        // Phone expanded player with album art background
                        val currentTrack = data?.playerData
                            ?.getOrNull(playerPagerState.currentPage)
                            ?.queueInfo?.currentItem?.track
                        val artUrl = currentTrack?.imageInfo?.url(serverUrl)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            // Album art background
                            if (artUrl != null) {
                                AsyncImage(
                                    model = artUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().alpha(0.15f)
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Close button
                                IconButton(
                                    onClick = { showPlayersView = false },
                                    modifier = Modifier.fillMaxWidth()
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        "Collapse",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlayersStateContent(
                                        playersState = playersState,
                                        pagerModifier = Modifier.fillMaxSize(),
                                        playerPagerState = playerPagerState,
                                        serverUrl = serverUrl,
                                        showQueue = true,
                                        isQueueExpanded = isQueueExpanded,
                                        onQueueExpandedSwitch = { isQueueExpanded = !isQueueExpanded },
                                        simplePlayerAction = { playerId, action -> viewModel.playerAction(playerId, action) },
                                        playerAction = { playerData, action -> viewModel.playerAction(playerData, action) },
                                        onPlayersRefreshClick = viewModel::refreshPlayers,
                                        onFavoriteClick = actionsViewModel::onFavoriteClick,
                                        onGoToLibrary = { showPlayersView = false },
                                        onItemMoved = { indexShift ->
                                            data?.let { d ->
                                                val currentPlayer = d.playerData[playerPagerState.currentPage].player
                                                val newIndex = (playerPagerState.currentPage + indexShift).coerceIn(0, d.playerData.size - 1)
                                                val newPlayers = d.playerData.map { it.player.id }.toMutableList().apply {
                                                    add(newIndex, removeAt(playerPagerState.currentPage))
                                                }
                                                viewModel.selectPlayer(currentPlayer)
                                                viewModel.onPlayersSortChanged(newPlayers)
                                            }
                                        },
                                        queueAction = { action -> viewModel.queueAction(action) },
                                        settingsAction = viewModel::openPlayerSettings,
                                        dspSettingsAction = viewModel::openPlayerDspSettings,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayersStateContent(
    playersState: HomeScreenViewModel.PlayersState,
    pagerModifier: Modifier = Modifier,
    playerPagerState: PagerState,
    serverUrl: String?,
    showQueue: Boolean,
    isQueueExpanded: Boolean,
    onQueueExpandedSwitch: () -> Unit,
    simplePlayerAction: (String, PlayerAction) -> Unit,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onPlayersRefreshClick: () -> Unit,
    onFavoriteClick: (AppMediaItem) -> Unit,
    onGoToLibrary: () -> Unit,
    onItemMoved: ((Int) -> Unit)?,
    queueAction: (QueueAction) -> Unit,
    settingsAction: (String) -> Unit,
    dspSettingsAction: (String) -> Unit,
    tvFocusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
    when (playersState) {
        is HomeScreenViewModel.PlayersState.Loading -> Text(
            text = "Loading players...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is HomeScreenViewModel.PlayersState.Data -> {
            if (playersState.playerData.isEmpty()) {
                Text(
                    text = "No players available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PlayersPager(
                    modifier = pagerModifier,
                    tvFocusRequester = tvFocusRequester,
                    playerPagerState = playerPagerState,
                    playersState = playersState,
                    serverUrl = serverUrl,
                    simplePlayerAction = simplePlayerAction,
                    playerAction = playerAction,
                    onPlayersRefreshClick = onPlayersRefreshClick,
                    onFavoriteClick = onFavoriteClick,
                    showQueue = showQueue,
                    isQueueExpanded = isQueueExpanded,
                    onQueueExpandedSwitch = onQueueExpandedSwitch,
                    onGoToLibrary = onGoToLibrary,
                    onItemMoved = onItemMoved,
                    queueAction = queueAction,
                    settingsAction = settingsAction,
                    dspSettingsAction = dspSettingsAction,
                    onNavigateUp = onNavigateUp,
                )
            }
        }

        else -> Text(
            text = "No players available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    homeBackStack: NavBackStack<*>,
    connectionState: SessionState,
    dataState: DataState<List<AppMediaItem.RecommendationFolder>>,
    serverUrl: String?,
    onRecommendationItemClick: (PlayableItem) -> Unit,
    onTrackPlayOption: (PlayableItem, QueueOption) -> Unit,
    playlistActions: ActionsViewModel.PlaylistActions,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit)
) {
    @Suppress("UNCHECKED_CAST")
    val typedBackStack = homeBackStack as NavBackStack<HomeNavScreen>

//    val homeBottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
//    val homeDialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    val saveableStateHolderForHome = rememberSaveableStateHolder()

    // Handle back when library is open
    BackHandler(enabled = typedBackStack.last() !is HomeNavScreen.Landing) {
        typedBackStack.removeLastOrNull()
    }

    NavDisplay(
        modifier = modifier,
        backStack = typedBackStack,
        onBack = { typedBackStack.removeLastOrNull() },
//        sceneStrategy = homeBottomSheetStrategy.then(homeDialogStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(saveableStateHolderForHome)
        ),
        entryProvider = entryProvider {
            entry<HomeNavScreen.Landing> {
                LandingPage(
                    Modifier.fillMaxSize(),
                    connectionState,
                    dataState,
                    serverUrl,
                    onItemClick = { item ->
                        when (item) {
                            is AppMediaItem.Artist,
                            is AppMediaItem.Album,
                            is AppMediaItem.Playlist,
                            is AppMediaItem.Podcast -> {
                                typedBackStack.add(
                                    HomeNavScreen.ItemDetails(
                                        itemId = item.itemId,
                                        mediaType = item.mediaType,
                                        providerId = item.provider
                                    )
                                )
                            }

                            is PlayableItem -> {
                                // For tracks and other types, play immediately
                                onRecommendationItemClick(item)
                            }
                            else -> Unit
                        }
                    },
                    onTrackPlayOption = onTrackPlayOption,
                    onLibraryItemClick = { type ->
                        if (type == null) {
                            typedBackStack.add(HomeNavScreen.Search)
                        } else {
                            typedBackStack.add(HomeNavScreen.Library(type))
                        }
                    },
                    playlistActions = playlistActions,
                    libraryActions = libraryActions,
                    providerIconFetcher = providerIconFetcher
                )
            }

            entry<HomeNavScreen.Library> {
                LibraryScreen(
                    initialTabType = it.type,
                    onBack = { typedBackStack.removeLastOrNull() },
                    onItemClick = { item ->
                        when (item) {
                            is AppMediaItem.Artist,
                            is AppMediaItem.Album,
                            is AppMediaItem.Playlist,
                            is AppMediaItem.Podcast -> {
                                typedBackStack.add(
                                    HomeNavScreen.ItemDetails(
                                        itemId = item.itemId,
                                        mediaType = item.mediaType,
                                        providerId = item.provider
                                    )
                                )
                            }

                            else -> {
                                // TODO: Handle track clicks or other item types
                            }
                        }
                    }
                )
            }

            entry<HomeNavScreen.ItemDetails> {
                ItemDetailsScreen(
                    itemId = it.itemId,
                    mediaType = it.mediaType,
                    providerId = it.providerId,
                    onBack = { typedBackStack.removeLastOrNull() },
                    onNavigateToItem = { itemId, mediaType, providerId ->
                        typedBackStack.add(
                            HomeNavScreen.ItemDetails(
                                itemId = itemId,
                                mediaType = mediaType,
                                providerId = providerId
                            )
                        )
                    }
                )
            }

            entry<HomeNavScreen.Search> {
                SearchScreen(
                    onBack = { typedBackStack.removeLastOrNull() },
                    onNavigateToItem = { itemId, mediaType, providerId ->
                        typedBackStack.add(
                            HomeNavScreen.ItemDetails(
                                itemId = itemId,
                                mediaType = mediaType,
                                providerId = providerId
                            )
                        )
                    }
                )
            }
        }
    )
}
