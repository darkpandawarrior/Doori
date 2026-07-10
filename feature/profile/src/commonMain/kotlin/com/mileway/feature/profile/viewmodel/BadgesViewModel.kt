package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.engagement.BadgeBoard
import com.mileway.core.data.engagement.BadgeId
import com.mileway.core.data.engagement.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * PLAN_V24 P12.1: the profile-hub badge board. Surfaces earned badges + seeded compliments/rating
 * from [BadgeRepository] (real completed trips, no fabricated totals), and fires a one-shot confetti
 * signal ([justEarned]) when a badge becomes newly earned while the section is on screen.
 *
 * ponytail: [justEarned] diffs against the badges seen since this ViewModel started — a milestone
 * crossed while the app was in the foreground confettis; one crossed while the app was closed simply
 * shows earned on next open (no persisted "already-celebrated" set). Enough for the demo; add a
 * DataStore celebrated-set if confetti must survive process death.
 */
class BadgesViewModel(badgeRepository: BadgeRepository) : ViewModel() {
    private var known: Set<BadgeId>? = null
    private val _justEarned = MutableStateFlow(false)
    val justEarned: StateFlow<Boolean> = _justEarned.asStateFlow()

    val state: StateFlow<BadgeBoard> =
        badgeRepository.observeBoard()
            .onEach { board ->
                val earnedNow = board.badges.filter { it.earned }.map { it.id }.toSet()
                val previous = known
                if (previous != null && (earnedNow - previous).isNotEmpty()) {
                    _justEarned.value = true
                }
                known = earnedNow
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BadgeBoard())

    fun consumeConfetti() {
        _justEarned.value = false
    }
}
