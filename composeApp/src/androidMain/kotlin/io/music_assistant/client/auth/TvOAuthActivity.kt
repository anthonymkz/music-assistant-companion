package io.music_assistant.client.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * A simple Activity that loads an OAuth URL in a WebView.
 * Used on Android TV where Chrome Custom Tabs are unavailable.
 * Intercepts the musicassistant:// callback redirect and delivers
 * the token back to MainActivity via an Intent.
 */
class TvOAuthActivity : Activity() {

    companion object {
        const val EXTRA_URL = "oauth_url"
        private const val CALLBACK_SCHEME = "musicassistant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url == null) {
            finish()
            return
        }

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val requestUrl = request?.url ?: return false
                    if (requestUrl.scheme == CALLBACK_SCHEME) {
                        // Intercept the callback and send it back to MainActivity
                        val callbackIntent = Intent(
                            Intent.ACTION_VIEW,
                            requestUrl
                        ).apply {
                            setPackage(packageName)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(callbackIntent)
                        finish()
                        return true
                    }
                    return false
                }
            }
        }

        setContentView(webView)
        webView.loadUrl(url)
    }
}
