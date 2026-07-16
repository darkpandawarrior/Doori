package com.mileway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.SessionRepository
import com.mileway.feature.whatsnew.data.WhatsNewEngagementRecorder
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/*
 * PLAN_V24 P2.2 / PLAN_V36 P2 — "What's new" digest, now driven by :feature:whatsnew's bundled
 * catalog (WhatsNewRepository) instead of the static in-file WHATS_NEW_ENTRIES list it replaces.
 * The badge/seen comparand is [WhatsNewRepository.currentVersion] — authors bump it by adding a
 * catalog entry, not by hand-editing a constant here.
 */

/** How many entries the digest sheet shows (the full catalog lives on the P3 list screen). */
private const val SHEET_DIGEST_SIZE = 3

data class WhatsNewUiState(
    val isVisible: Boolean = false,
    val entries: List<WhatsNewEntry> = emptyList(),
)

/**
 * PLAN_V24 P2.2 — shows the "What's new in Mileway" sheet once after login when the persisted
 * last-seen version is behind [WhatsNewRepository.currentVersion]. Acknowledging advances the
 * stored version so it never replays for this release.
 */
class WhatsNewViewModel(
    private val sessionRepository: SessionRepository,
    private val whatsNewRepository: WhatsNewRepository,
    private val engagementRecorder: WhatsNewEngagementRecorder,
) : ViewModel() {
    val uiState: StateFlow<WhatsNewUiState> =
        sessionRepository.sessionState
            .map { session ->
                WhatsNewUiState(
                    isVisible = session.whatsNewLastSeenVersion < whatsNewRepository.currentVersion,
                    entries = whatsNewRepository.entries().take(SHEET_DIGEST_SIZE),
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, WhatsNewUiState())

    /** Called once the sheet has been acknowledged — advances the stored version. */
    fun acknowledge() {
        viewModelScope.launch { sessionRepository.markWhatsNewSeen(whatsNewRepository.currentVersion) }
    }

    /** PLAN_V36 P7 (spec §5.4/§8): the home banner (fed by [uiState.entries]'s latest entry) was tapped. */
    fun recordBannerOpen(entryId: String) {
        viewModelScope.launch { engagementRecorder.record(WhatsNewEngagementRecorder.TYPE_BANNER_OPEN, entryId) }
    }
}

/** Koin module for [WhatsNewViewModel]. */
val whatsNewModule =
    module {
        viewModelOf(::WhatsNewViewModel)
    }
