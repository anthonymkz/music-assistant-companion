package io.music_assistant.client.auth

import android.app.Activity
import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

actual class OAuthHandler(private val activity: Activity) {
    actual fun openOAuthUrl(url: String) {
        val uiModeManager = activity.getSystemService(UiModeManager::class.java)
        val isTV = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        if (isTV) {
            // On TV: use in-app WebView since Custom Tabs / browsers aren't available
            val intent = Intent(activity, TvOAuthActivity::class.java)
            intent.putExtra(TvOAuthActivity.EXTRA_URL, url)
            activity.startActivity(intent)
        } else {
            // On phone/tablet: use Chrome Custom Tabs
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(activity, url.toUri())
        }
    }
}
