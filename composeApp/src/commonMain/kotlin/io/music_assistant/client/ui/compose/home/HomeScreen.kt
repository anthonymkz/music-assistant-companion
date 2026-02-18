@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
import io.music_assistant.client.utils.formatDuration
import kotlin.time.DurationUnit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    // Focus requester for TV header nav tabs
    val headerFocusRequester = remember { FocusRequester() }

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

    // TV header context item — tracks the focused/selected item from child screens
    var tvContextItem by remember { mutableStateOf<AppMediaItem?>(null) }

    // TV header: derive which destination is selected from the back stack
    @Suppress("UNCHECKED_CAST")
    val currentRailDestination by remember(homeBackStack) {
        derivedStateOf {
            when (homeBackStack.last()) {
                is HomeNavScreen.Library -> TvNavDestination.Library
                is HomeNavScreen.Search -> TvNavDestination.Search
                is HomeNavScreen.Landing -> TvNavDestination.Home
                else -> TvNavDestination.Home // ItemDetails keeps current context
            }
        }
    }

    // Clear context item when navigating to a different destination
    LaunchedEffect(currentRailDestination) {
        if (currentRailDestination == TvNavDestination.Home ||
            currentRailDestination == TvNavDestination.Search
        ) {
            tvContextItem = null
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
                                    headerFocusRequester.requestFocus()
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
                val hasPlayers = data?.playerData?.isNotEmpty() == true
                // Local TV player index — avoids race condition in ViewModel where
                // selectedPlayerIndex resets when playersData re-emits
                var tvPlayerIndex by remember { mutableStateOf(data?.selectedPlayerIndex ?: 0) }

                // Derive context art/title/subtitle from focused item, fallback to now-playing
                val currentPlayerData = data?.playerData?.getOrNull(tvPlayerIndex)
                val contextArtUrl = tvContextItem?.imageInfo?.url(serverUrl)
                    ?: currentPlayerData?.queueInfo?.currentItem?.track?.imageInfo?.url(serverUrl)
                val contextTitle = tvContextItem?.name
                    ?: currentPlayerData?.queueInfo?.currentItem?.track?.name
                val contextSubtitle = tvContextItem?.subtitle
                    ?: currentPlayerData?.queueInfo?.currentItem?.track?.subtitle

                // TV: AnimatedContent for smooth transition between home and expanded player
                AnimatedContent(
                    targetState = showPlayersView,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "tv_player_transition"
                ) { isPlayersViewShown ->
                    if (!isPlayersViewShown) {
                        val headerHeight = 280.dp
                        @Suppress("UNCHECKED_CAST")
                        val typedBackStack =
                            homeBackStack as NavBackStack<HomeNavScreen>

                        // Box: header overlay at top, content below
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .focusGroup()
                        ) {
                            // Content area — starts below header
                            HomeContent(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = headerHeight),
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
                                },
                                onTvContextItemChanged = { tvContextItem = it },
                            )

                            // Top header with nav tabs, context artwork, now playing widget
                            TvTopHeader(
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

                                        TvNavDestination.Settings -> {
                                            navigateTo(NavScreen.Settings)
                                        }
                                    }
                                },
                                headerFocusRequester = headerFocusRequester,
                                contextArtUrl = contextArtUrl,
                                contextTitle = contextTitle,
                                contextSubtitle = contextSubtitle,
                                playerData = data?.playerData?.getOrNull(tvPlayerIndex),
                                serverUrl = serverUrl,
                                onNowPlayingClick = { showPlayersView = true },
                            )
                        }
                    } else {
                        // Expanded Now Playing — full screen, controls pinned at bottom
                        val controlsFocusRequester = remember { FocusRequester() }

                        val currentPlayerData = data?.playerData?.getOrNull(tvPlayerIndex)
                        val currentTrack = currentPlayerData?.queueInfo?.currentItem?.track
                        val artUrl = currentTrack?.imageInfo?.url(serverUrl)
                        val bgColor = MaterialTheme.colorScheme.background
                        val isLocalPlayer = currentPlayerData?.playerId == data?.localPlayerId


                        var showVolumeDialog by remember { mutableStateOf(false) }
                        var showSpeakerPicker by remember { mutableStateOf(false) }
                        val volumeLevel = currentPlayerData?.player?.currentVolume ?: 0f

                        // Progress data
                        val duration = currentTrack?.duration?.takeIf { it > 0 }?.toFloat()
                        val elapsed = currentPlayerData?.queueInfo?.elapsedTime?.toFloat() ?: 0f

                        // Close queue when leaving expanded player
                        LaunchedEffect(Unit) {
                            isQueueExpanded = false
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgColor)
                                .focusGroup()
                        ) {
                            // Layer 1: Backdrop art — top ~55%, fades to bg
                            if (artUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.55f)
                                        .align(Alignment.TopCenter)
                                ) {
                                    AsyncImage(
                                        model = artUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().alpha(0.3f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.3f to Color.Transparent,
                                                    1f to bgColor,
                                                )
                                            )
                                    )
                                }
                            }

                            // Layer 2: Main Column — controls always pinned at bottom
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Close button (top center)
                                IconButton(
                                    onClick = { showPlayersView = false },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        "Collapse",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                // Bottom control panel — always pinned at bottom
                                LaunchedEffect(Unit) {
                                    try { controlsFocusRequester.requestFocus() } catch (_: Exception) {}
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp)
                                        .padding(bottom = 32.dp)
                                        .focusRequester(controlsFocusRequester)
                                        .focusGroup()
                                ) {
                                    // Row: mini art on left, track info on right
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Mini album art (100dp)
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = if (artUrl != null) 1f else 0.4f
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (artUrl != null) {
                                                AsyncImage(
                                                    model = artUrl,
                                                    contentDescription = currentTrack?.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(24.dp))

                                        // Track title + subtitle
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentTrack?.name ?: "--idle--",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            currentTrack?.subtitle?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(20.dp))

                                    // Selectors row — TvNavTab style, centered
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            TvPlayerTab(
                                                icon = Icons.Default.Speaker,
                                                label = currentPlayerData?.player?.displayName ?: "Speaker",
                                                isSelected = showSpeakerPicker,
                                                onClick = { showSpeakerPicker = true },
                                            )
                                            TvPlayerTab(
                                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                                label = "Queue",
                                                isSelected = isQueueExpanded,
                                                onClick = { isQueueExpanded = !isQueueExpanded },
                                            )
                                            if (currentPlayerData?.player?.canSetVolume == true && !isLocalPlayer) {
                                                TvPlayerTab(
                                                    icon = if (currentPlayerData.player.volumeMuted)
                                                        Icons.AutoMirrored.Filled.VolumeMute
                                                    else Icons.AutoMirrored.Filled.VolumeUp,
                                                    label = "Vol ${volumeLevel.toInt()}%",
                                                    isSelected = false,
                                                    onClick = { showVolumeDialog = true },
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // Progress bar + timestamps
                                    LinearProgressIndicator(
                                        progress = {
                                            if (duration != null && duration > 0f)
                                                (elapsed / duration).coerceIn(0f, 1f)
                                            else 0f
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = elapsed.takeIf { currentTrack != null }
                                                .formatDuration(DurationUnit.SECONDS)
                                                .takeIf { duration != null } ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = currentTrack?.let {
                                                duration?.formatDuration(DurationUnit.SECONDS) ?: "\u221E"
                                            } ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // Transport controls
                                    if (currentPlayerData != null) {
                                        PlayerControls(
                                            modifier = Modifier.fillMaxWidth(),
                                            playerData = currentPlayerData,
                                            playerAction = { pd, action -> viewModel.playerAction(pd, action) },
                                            enabled = !currentPlayerData.player.isAnnouncing,
                                            showVolumeButtons = false,
                                            mainButtonSize = 64.dp,
                                        )
                                    }
                                }
                            }

                            // Layer 3: Queue slide-up overlay panel
                            val queueFocusRequester = remember { FocusRequester() }
                            AnimatedVisibility(
                                visible = isQueueExpanded,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(tween(200)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(250)
                                ) + fadeOut(tween(200)),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                LaunchedEffect(Unit) {
                                    try { queueFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                // Scrim + panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        ) { isQueueExpanded = false },
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    // Queue panel — bottom 65% of screen
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.65f)
                                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ) { /* consume clicks on panel */ }
                                            .focusRequester(queueFocusRequester)
                                            .focusGroup()
                                            .padding(top = 16.dp),
                                    ) {
                                        // Handle bar
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .size(width = 40.dp, height = 4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                        )

                                        // Header row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.QueueMusic,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = "Queue",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { isQueueExpanded = false }
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    "Close queue",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        // Queue content (no toggle button — our header handles that)
                                        if (currentPlayerData != null) {
                                            currentPlayerData.queue?.let { queue ->
                                                CollapsibleQueue(
                                                    modifier = Modifier.weight(1f),
                                                    queue = queue,
                                                    isQueueExpanded = true,
                                                    onQueueExpandedSwitch = { isQueueExpanded = false },
                                                    onGoToLibrary = {
                                                        isQueueExpanded = false
                                                        showPlayersView = false
                                                    },
                                                    serverUrl = serverUrl,
                                                    queueAction = { action -> viewModel.queueAction(action) },
                                                    players = data?.playerData ?: emptyList(),
                                                    onPlayerSelected = { playerId ->
                                                        data?.playerData?.indexOfFirst { it.player.id == playerId }
                                                            ?.takeIf { it >= 0 }?.let { idx ->
                                                                tvPlayerIndex = idx
                                                                viewModel.selectPlayer(data.playerData[idx].player)
                                                            }
                                                    },
                                                    isCurrentPage = true,
                                                    showToggleButton = false,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Layer 4: Speaker picker slide-up overlay
                            val speakerFocusRequester = remember { FocusRequester() }
                            AnimatedVisibility(
                                visible = showSpeakerPicker && data != null,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(tween(200)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(250)
                                ) + fadeOut(tween(200)),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                LaunchedEffect(Unit) {
                                    try { speakerFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        ) { showSpeakerPicker = false },
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ) { /* consume */ }
                                            .focusRequester(speakerFocusRequester)
                                            .focusGroup()
                                            .padding(top = 16.dp, bottom = 24.dp),
                                    ) {
                                        // Handle bar
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .size(width = 40.dp, height = 4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                        )

                                        // Header row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.Speaker,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = "Select Speaker",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { showSpeakerPicker = false }
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    "Close speaker picker",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        // Speaker list
                                        data?.playerData?.forEachIndexed { index, pd ->
                                            val isCurrentSpeaker = index == tvPlayerIndex
                                            val primary = MaterialTheme.colorScheme.primary
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        tvPlayerIndex = index
                                                        viewModel.selectPlayer(pd.player)
                                                        showSpeakerPicker = false
                                                    }
                                                    .then(
                                                        if (isCurrentSpeaker)
                                                            Modifier.background(primary.copy(alpha = 0.12f))
                                                        else Modifier
                                                    )
                                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    Icons.Default.Speaker,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (isCurrentSpeaker) primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Spacer(Modifier.width(16.dp))
                                                Text(
                                                    text = pd.player.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isCurrentSpeaker) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrentSpeaker) primary
                                                    else MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Volume dialog
                            if (showVolumeDialog && currentPlayerData != null) {
                                VolumeDialog(
                                    player = currentPlayerData,
                                    playerAction = { pd, action -> viewModel.playerAction(pd, action) },
                                    onDismiss = { showVolumeDialog = false },
                                )
                            }
                        }
                    }
                }
            } else {
                // Phone: AnimatedContent with expressive spring transitions
                AnimatedContent(
                    targetState = showPlayersView,
                    transitionSpec = {
                        if (targetState) {
                            // Expanding: slide up + scale in + fade
                            (slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + scaleIn(
                                initialScale = 0.92f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(
                                animationSpec = tween(150)
                            )) togetherWith (slideOutVertically(
                                targetOffsetY = { -it / 4 },
                                animationSpec = tween(200)
                            ) + fadeOut(animationSpec = tween(150)))
                        } else {
                            // Collapsing: slide down + fade
                            (slideInVertically(
                                initialOffsetY = { -it / 4 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = tween(200)
                            )) togetherWith (slideOutVertically(
                                targetOffsetY = { it / 3 },
                                animationSpec = tween(250)
                            ) + scaleOut(
                                targetScale = 0.92f,
                                animationSpec = tween(250)
                            ) + fadeOut(animationSpec = tween(200)))
                        }
                    },
                    label = "player_transition"
                ) { isPlayersViewShown ->
                    if (!isPlayersViewShown) {
                        // Box layout: content fills screen, mini player overlays at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            HomeContent(
                                modifier = Modifier.fillMaxSize(),
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

                            // Mini player — frosted glass floating overlay at bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .wrapContentHeight()
                                    .defaultMinSize(minHeight = 100.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showPlayersView = true },
                                contentAlignment = Alignment.Center
                            ) {
                                // Frosted glass background: blurred tinted layer
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .blur(16.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f))
                                )
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
                            // Album art background — blurred for depth
                            if (artUrl != null) {
                                AsyncImage(
                                    model = artUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                        .blur(32.dp)
                                        .alpha(0.2f)
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
    providerIconFetcher: (@Composable (Modifier, String) -> Unit),
    onTvContextItemChanged: ((AppMediaItem?) -> Unit)? = null,
) {
    @Suppress("UNCHECKED_CAST")
    val typedBackStack = homeBackStack as NavBackStack<HomeNavScreen>

    val saveableStateHolderForHome = rememberSaveableStateHolder()

    // Handle back when library is open
    BackHandler(enabled = typedBackStack.last() !is HomeNavScreen.Landing) {
        typedBackStack.removeLastOrNull()
    }

    NavDisplay(
        modifier = modifier,
        backStack = typedBackStack,
        onBack = { typedBackStack.removeLastOrNull() },
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
                    providerIconFetcher = providerIconFetcher,
                    onItemFocused = onTvContextItemChanged,
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
                    },
                    onFocusedItemChanged = onTvContextItemChanged,
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
                    },
                    onItemLoaded = onTvContextItemChanged,
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

@Composable
private fun TvPlayerTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
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
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = defaultColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = defaultColor,
            )
        }
    }
}
