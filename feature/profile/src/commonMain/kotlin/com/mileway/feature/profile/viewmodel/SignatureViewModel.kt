package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.feature.profile.repository.SignatureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P12.7 — the digital-signature tile shown in Personal Details. [enabled] gates the tile on
 * the `signature` plugin (Gig persona); [signaturePath] is the saved PNG path (null while none). The
 * platform screen rasterises the drawn strokes to a PNG and hands the path to [save].
 */
data class SignatureUiState(
    val enabled: Boolean = false,
    val signaturePath: String? = null,
)

class SignatureViewModel(
    private val repository: SignatureRepository,
    private val pluginRegistry: PluginRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(SignatureUiState())
    val state: StateFlow<SignatureUiState> = _state.asStateFlow()

    init {
        combine(
            pluginRegistry.observe("signature"),
            repository.observe(),
        ) { enabled, path -> SignatureUiState(enabled = enabled, signaturePath = path) }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    fun save(imagePath: String) {
        viewModelScope.launch { repository.save(imagePath) }
    }

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}
