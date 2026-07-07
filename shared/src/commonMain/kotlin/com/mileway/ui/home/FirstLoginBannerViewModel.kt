package com.mileway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** What the one-time welcome banner shows — a thin projection of [com.mileway.core.data.session.SessionState]. */
data class FirstLoginBannerUiState(
    val isVisible: Boolean = false,
    val displayName: String? = null,
    val officeName: String? = null,
)

/**
 * PLAN_V22 P7.1: gates the Home tab's one-time "Welcome, {name}" banner off
 * [com.mileway.core.data.session.SessionState.isFirstLoginPending] — the flag
 * [SessionRepository.signInWithCredentials] sets on a fresh sign-in and this ViewModel clears
 * once the banner has actually been shown, so it never reappears on a later Home recomposition
 * or app relaunch until the next fresh sign-in.
 */
class FirstLoginBannerViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<FirstLoginBannerUiState> = sessionRepository.sessionState
        .map { session ->
            FirstLoginBannerUiState(
                isVisible = session.isFirstLoginPending,
                displayName = session.displayName,
                officeName = session.officeName,
            )
        }
        // Eagerly (not WhileSubscribed): this is a cheap DataStore-derived projection read once
        // at Home composition, and eager sharing keeps `.value` correct even before Compose has
        // a chance to collect (matters for unit tests that assert on `.value` directly).
        .stateIn(viewModelScope, SharingStarted.Eagerly, FirstLoginBannerUiState())

    /** Called once the banner has actually been composed/shown — clears the one-shot flag. */
    fun onBannerShown() {
        viewModelScope.launch {
            sessionRepository.clearFirstLoginPending()
        }
    }
}

/** Koin module for [FirstLoginBannerViewModel] — registered alongside [homeModule]'s own entries. */
val firstLoginBannerModule = module {
    viewModelOf(::FirstLoginBannerViewModel)
}
