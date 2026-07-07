package com.mileway.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Hoisted state for [WelcomeDisclaimerSheet]'s once-per-install visibility and its "queue a
 * sign-in tap made while the sheet is showing" behavior, extracted from [LoginScreen] to keep
 * that composable's own branching flat.
 *
 * [isShowing] starts as `!initiallyShown` (a local `mutableStateOf`, not the caller-passed flag
 * directly) so [dismiss] hides the sheet immediately without waiting on the DataStore round-trip
 * that persists `SessionState.hasShownWelcomeDisclaimer` — [onShown] fires that persistence as a
 * side effect once, the first time [dismiss] is called.
 */
class WelcomeDisclaimerState internal constructor(
    initiallyShown: Boolean,
    private val onShown: () -> Unit,
    private val onResumeSignIn: (isGuest: Boolean) -> Unit,
) {
    var isShowing by mutableStateOf(!initiallyShown)
        private set

    private var pendingSignIn: Boolean? = null

    /** Begins sign-in immediately, or — if the sheet is still showing — queues it for [dismiss]. */
    fun beginSignInOrQueue(isGuest: Boolean) {
        if (isShowing) {
            pendingSignIn = isGuest
        } else {
            onResumeSignIn(isGuest)
        }
    }

    /** Hides the sheet, persists that it's been shown, and resumes any queued sign-in tap. */
    fun dismiss() {
        if (!isShowing) return
        isShowing = false
        onShown()
        pendingSignIn?.let { isGuest ->
            pendingSignIn = null
            onResumeSignIn(isGuest)
        }
    }
}

@Composable
fun rememberWelcomeDisclaimerState(
    initiallyShown: Boolean,
    onShown: () -> Unit,
    onResumeSignIn: (isGuest: Boolean) -> Unit,
): WelcomeDisclaimerState {
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnResumeSignIn by rememberUpdatedState(onResumeSignIn)
    return remember {
        WelcomeDisclaimerState(
            initiallyShown = initiallyShown,
            onShown = { currentOnShown() },
            onResumeSignIn = { isGuest -> currentOnResumeSignIn(isGuest) },
        )
    }
}
