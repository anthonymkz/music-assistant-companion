package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.mass
import org.jetbrains.compose.resources.painterResource

enum class TvNavDestination(val icon: ImageVector, val label: String) {
    Home(Icons.Default.Home, "Home"),
    Library(Icons.Default.LibraryMusic, "Library"),
    Search(Icons.Default.Search, "Search"),
    NowPlaying(Icons.Default.MusicNote, "Now Playing"),
    Settings(Icons.Default.Settings, "Settings"),
}

@Composable
fun TvNavigationRail(
    selectedDestination: TvNavDestination,
    onDestinationSelected: (TvNavDestination) -> Unit,
    railFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        header = {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(Res.drawable.mass),
                contentDescription = "Music Assistant",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    ) {
        TvNavDestination.entries.forEachIndexed { index, destination ->
            NavigationRailItem(
                modifier = if (index == 0) {
                    Modifier.focusRequester(railFocusRequester)
                } else {
                    Modifier
                },
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
