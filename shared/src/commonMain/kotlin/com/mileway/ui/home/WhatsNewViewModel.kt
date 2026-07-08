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

/*
 * PLAN_V24 P2.2 — "What's new" content, version-keyed (the reference app WHATS_NEW_ACK /
 * WHATS_NEW_LAST_SEEN_VERSION). ponytail: the static list lives here as UI data rather than in
 * :stub — shared doesn't depend on :stub and this is a small in-app changelog, not backend mock.
 */

/** The current changelog version. Bumped whenever [WHATS_NEW_ENTRIES] gains a new release block. */
const val CURRENT_WHATS_NEW_VERSION: Int = 24

/** One "what's new" line. */
data class WhatsNewItem(
    val title: String,
    val description: String,
)

/** Static changelog for the current version. */
val WHATS_NEW_ENTRIES: List<WhatsNewItem> =
    listOf(
        WhatsNewItem("Phone sign-in", "Sign in with your phone number and a one-time code."),
        WhatsNewItem("Two-factor & security", "Optional MFA plus a stronger PIN lockout keep your account safe."),
        WhatsNewItem("Your plugins", "Turn any feature on or off from Settings → Plugins."),
    )

data class WhatsNewUiState(
    val isVisible: Boolean = false,
    val items: List<WhatsNewItem> = emptyList(),
)

/**
 * PLAN_V24 P2.2 — shows the "What's new in Mileway" sheet once after login when the persisted
 * last-seen version is behind [CURRENT_WHATS_NEW_VERSION]. Acknowledging advances the stored
 * version so it never replays for this release.
 */
class WhatsNewViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<WhatsNewUiState> =
        sessionRepository.sessionState
            .map { session ->
                WhatsNewUiState(
                    isVisible = session.whatsNewLastSeenVersion < CURRENT_WHATS_NEW_VERSION,
                    items = WHATS_NEW_ENTRIES,
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, WhatsNewUiState())

    /** Called once the sheet has been acknowledged — advances the stored version. */
    fun acknowledge() {
        viewModelScope.launch { sessionRepository.markWhatsNewSeen(CURRENT_WHATS_NEW_VERSION) }
    }
}

/** Koin module for [WhatsNewViewModel]. */
val whatsNewModule =
    module {
        viewModelOf(::WhatsNewViewModel)
    }
