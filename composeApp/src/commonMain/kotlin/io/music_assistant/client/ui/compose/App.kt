package io.music_assistant.client.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.ui.compose.nav.NavigationRoot
import io.music_assistant.client.ui.theme.AppTheme
import io.music_assistant.client.ui.theme.SystemAppearance
import io.music_assistant.client.ui.theme.ThemeSetting
import io.music_assistant.client.ui.theme.ThemeViewModel
import io.music_assistant.client.utils.LocalPlatformType
import io.music_assistant.client.utils.PlatformType
import io.music_assistant.client.utils.getPlatformType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun App() {
    val platformType = getPlatformType()
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val theme = themeViewModel.theme.collectAsStateWithLifecycle(ThemeSetting.FollowSystem)
    val darkTheme = when {
        platformType == PlatformType.TV -> true
        theme.value == ThemeSetting.Dark -> true
        theme.value == ThemeSetting.Light -> false
        else -> isSystemInDarkTheme()
    }
    SystemAppearance(isDarkTheme = darkTheme)
    CompositionLocalProvider(LocalPlatformType provides platformType) {
        AppTheme(darkTheme = darkTheme) {
            NavigationRoot()
        }
    }
}


