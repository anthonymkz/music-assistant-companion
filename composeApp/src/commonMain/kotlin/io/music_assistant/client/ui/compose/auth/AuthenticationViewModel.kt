package io.music_assistant.client.ui.compose.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.auth.AuthenticationManager
import io.music_assistant.client.data.model.server.AuthProvider
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val authManager: AuthenticationManager,
    private val settings: SettingsRepository,
    private val serviceClient: ServiceClient
) : ViewModel() {

    private val _providers = MutableStateFlow<List<AuthProvider>>(emptyList())
    val providers: StateFlow<List<AuthProvider>> = _providers.asStateFlow()

    private val _selectedProvider = MutableStateFlow<AuthProvider?>(null)
    val selectedProvider: StateFlow<AuthProvider?> = _selectedProvider.asStateFlow()

    val authState = authManager.authState
    val sessionState = serviceClient.sessionState

    val username = MutableStateFlow("")
    val password = MutableStateFlow("")

    init {
        // Trigger initial load if already connected and awaiting auth
        viewModelScope.launch {
            val currentState = sessionState.value
            if (currentState is SessionState.Connected) {
                val dataConnectionState = currentState.dataConnectionState
                if (dataConnectionState is DataConnectionState.AwaitingAuth) {
                    // Only load providers if we're not in a failed state
                    when (dataConnectionState.authProcessState) {
                        is io.music_assistant.client.utils.AuthProcessState.Failed -> {
                            // Don't reload providers when auth failed
                        }
                        else -> {
                            loadProviders()
                        }
                    }
                }
            }
        }

        // Auto-fetch providers when connected and awaiting auth
        viewModelScope.launch {
            sessionState.collect { state ->
                co.touchlab.kermit.Logger.d("AuthVM") { "SessionState changed: ${state::class.simpleName}" }
                if (state is SessionState.Connected) {
                    val dataConnectionState = state.dataConnectionState
                    co.touchlab.kermit.Logger.d("AuthVM") { "DataConnectionState: ${dataConnectionState::class.simpleName}" }
                    if (dataConnectionState is DataConnectionState.AwaitingAuth) {
                        co.touchlab.kermit.Logger.d("AuthVM") { "AwaitingAuth - checking auth process state" }
                        // Only load providers if we're not in a failed state
                        // (to avoid overriding error messages)
                        when (dataConnectionState.authProcessState) {
                            is io.music_assistant.client.utils.AuthProcessState.Failed -> {
                                co.touchlab.kermit.Logger.d("AuthVM") { "Auth failed - not reloading providers" }
                                // Don't reload providers when auth failed - keep the error visible
                            }
                            else -> {
                                co.touchlab.kermit.Logger.d("AuthVM") { "Calling loadProviders()" }
                                loadProviders()
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadProviders() {
        co.touchlab.kermit.Logger.d("AuthVM") { "loadProviders() called, current providers count: ${_providers.value.size}" }
        // Don't reload if we already have providers (to avoid overriding error states)
        if (_providers.value.isNotEmpty()) {
            co.touchlab.kermit.Logger.d("AuthVM") { "Skipping - providers already loaded" }
            return
        }

        val currentState = sessionState.value
        val isWebRTC = currentState is SessionState.Connected.WebRTC

        if (isWebRTC) {
            // For WebRTC, skip API call - only builtin auth works (OAuth requires HTTP redirects)
            co.touchlab.kermit.Logger.d("AuthVM") { "WebRTC connection - using builtin provider directly (skip API call)" }
            val builtinProvider = io.music_assistant.client.data.model.server.AuthProvider(
                id = "builtin",
                type = "builtin",
                requiresRedirect = false
            )
            _providers.value = listOf(builtinProvider)
            _selectedProvider.value = builtinProvider
            return
        }

        // For direct connections, fetch all providers from server
        co.touchlab.kermit.Logger.d("AuthVM") { "Direct connection - fetching providers from server" }
        viewModelScope.launch {
            authManager.getProviders()
                .onSuccess { providerList ->
                    co.touchlab.kermit.Logger.d("AuthVM") { "Received ${providerList.size} providers: ${providerList.map { it.id }}" }
                    _providers.value = providerList
                    if (_selectedProvider.value == null && providerList.isNotEmpty()) {
                        _selectedProvider.value = providerList.firstOrNull()
                    }
                }
                .onFailure { error ->
                    co.touchlab.kermit.Logger.e("AuthVM", error) { "Failed to load providers" }
                }
        }
    }

    fun selectProvider(provider: AuthProvider) {
        _selectedProvider.value = provider
    }

    fun login() {
        viewModelScope.launch {
            val provider = _selectedProvider.value ?: return@launch

            when (provider.type) {
                "builtin" -> {
                    authManager.loginWithCredentials(
                        provider.id,
                        username.value,
                        password.value
                    )
                }

                else -> {
                    // OAuth or other redirect-based auth
                    // Use custom URL scheme for reliable deep linking
                    val returnUrl = "musicassistant://auth/callback"
                    authManager.getOAuthUrl(provider.id, returnUrl)
                        .onSuccess { url -> authManager.startOAuthFlow(url) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // AuthenticationManager handles both flag setting and token clearing
            authManager.logout()
        }
    }
}
