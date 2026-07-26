package com.mileway.core.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.Clock

/**
 * "Now", for UI that renders time *relatively* ("5 hours ago", "in 2 days").
 *
 * Why this exists: a composable that calls [Clock.System.now] directly renders differently depending
 * on when it happens to be composed. Against fixed demo data that is not a hypothetical — the
 * approvals list drifted from "5 hours ago" to "6 hours ago" to "1 day ago" as wall-clock time
 * advanced, which changed the rendered text, which changed the PNG's byte size. That made
 * `approvals_screen_pending_tab.png` re-record on essentially every CI run, so the screenshot bot
 * opened a fresh baseline-refresh PR each time and they piled up (7 branches before this was traced).
 * `screenshots.yml`'s premise — "CI renders deterministically run-to-run, so an unchanged screen
 * produces a byte-identical PNG" — is only true if the screens themselves are deterministic.
 *
 * Screenshot and UI tests pin it:
 * ```
 * CompositionLocalProvider(LocalNowMs provides { FIXED_INSTANT_MS }) { ScreenUnderTest() }
 * ```
 *
 * The default is the real clock, so production behaviour is unchanged.
 *
 * ponytail: a `staticCompositionLocalOf` rather than a threaded parameter because the readers are
 * private composables several levels down a list item — threading it would touch a chain of
 * signatures to inject one ambient value. Matches the existing `LocalReducedMotion` /
 * `LocalManagerProviders` idiom in this package. `static` because the value changes rarely (never, in
 * practice) and reads are frequent.
 *
 * Not yet applied everywhere: several other screens still call [Clock.System.now] inside composition
 * — grep for it under each feature module's `ui` package. They have the same latent flapping problem;
 * route them through this seam when one of them starts re-recording.
 */
val LocalNowMs =
    staticCompositionLocalOf<() -> Long> {
        { Clock.System.now().toEpochMilliseconds() }
    }
