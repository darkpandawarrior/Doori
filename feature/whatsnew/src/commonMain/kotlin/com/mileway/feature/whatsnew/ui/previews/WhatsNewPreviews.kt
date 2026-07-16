package com.mileway.feature.whatsnew.ui.previews

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewMatrix
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import com.mileway.feature.whatsnew.ui.WhatsNewDetailContent
import com.mileway.feature.whatsnew.ui.WhatsNewListContent
import com.mileway.feature.whatsnew.viewmodel.WhatsNewDetailUiState
import com.mileway.feature.whatsnew.viewmodel.WhatsNewListUiState
import kotlinx.datetime.LocalDate

// ---------------------------------------------------------------------------
// PLAN_V36 P8, What's New preview matrix.
//
// Deliberately inline fake entries, NOT WhatsNewCatalog — previews must stay stable (and these
// screenshot baselines must stay meaningful) as the real catalog grows. Media paths point at the
// three real bundled images already shipped for P5's seed entries, so AsyncImage/Coil resolves a
// real image in both the IDE preview and the Roborazzi capture instead of a broken-image icon.
// Renders WhatsNewListContent/WhatsNewDetailContent directly (state in, no ViewModel/koinViewModel)
// — same "DI-free previews" convention as TrackingPreviews.kt.
// ---------------------------------------------------------------------------

private val previewEntryWithHero =
    WhatsNewEntry(
        id = "preview-plugins",
        version = 1,
        title = "Your plugins",
        description = "Turn any feature on or off from Settings → Plugins.",
        media = listOf(WhatsNewMedia(path = "files/whatsnew/plugins-registry/01-plugins-screen.png")),
        releasedOn = LocalDate(2026, 7, 8),
        modules = listOf("Settings"),
    )

private val previewEntryNoHero =
    WhatsNewEntry(
        id = "preview-signin",
        version = 2,
        title = "Phone sign-in",
        description = "Sign in with your phone number and a one-time code.",
        media = emptyList(),
        releasedOn = LocalDate(2026, 7, 10),
        modules = listOf("Auth"),
    )

private val previewEntrySingleMedia =
    WhatsNewEntry(
        id = "preview-security",
        version = 3,
        title = "Two-factor & security",
        description = "Optional MFA plus a stronger PIN lockout keep your account safe.",
        media = listOf(WhatsNewMedia(path = "files/whatsnew/two-factor-security/01-pin-lock-screen.png", caption = "Stronger PIN lockout")),
        releasedOn = LocalDate(2026, 7, 12),
        modules = listOf("Auth", "Security"),
        contactEmail = "support@mileway.app",
    )

private val previewEntryMultiMedia =
    WhatsNewEntry(
        id = "preview-carousel",
        version = 4,
        title = "A tour of what shipped",
        description = "Plugins, sign-in and security — swipe through the highlights.",
        media =
            listOf(
                WhatsNewMedia(path = "files/whatsnew/plugins-registry/01-plugins-screen.png", caption = "Plugins"),
                WhatsNewMedia(path = "files/whatsnew/phone-signin/01-sign-in-screen.png", caption = "Sign-in"),
                WhatsNewMedia(path = "files/whatsnew/two-factor-security/01-pin-lock-screen.png", caption = "Security"),
            ),
        releasedOn = LocalDate(2026, 7, 14),
        modules = listOf("Settings", "Auth", "Security"),
        link = "https://mileway.app/whats-new",
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@PreviewLightDark
@Composable
fun PreviewWhatsNewListPopulated() {
    PreviewSurface {
        WhatsNewListContent(
            state =
                WhatsNewListUiState(
                    entries = listOf(previewEntryWithHero, previewEntryNoHero),
                    newEntryIds = setOf(previewEntryWithHero.id),
                ),
            onBack = {},
            onOpenEntry = {},
            sharedTransitionScope = null,
            animatedContentScope = null,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@PreviewLightDark
@Composable
fun PreviewWhatsNewListEmpty() {
    PreviewSurface {
        WhatsNewListContent(
            state = WhatsNewListUiState(entries = emptyList()),
            onBack = {},
            onOpenEntry = {},
            sharedTransitionScope = null,
            animatedContentScope = null,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@PreviewMatrix
@Composable
fun PreviewWhatsNewDetailSingleMedia() {
    PreviewSurface {
        WhatsNewDetailContent(
            state = WhatsNewDetailUiState(entry = previewEntrySingleMedia, selectedMediaIndex = 0),
            onBack = {},
            onSelectMedia = {},
            onShare = {},
            onOpenLink = {},
            onContact = { _, _ -> },
            sharedTransitionScope = null,
            animatedContentScope = null,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@PreviewMatrix
@Composable
fun PreviewWhatsNewDetailCarousel() {
    PreviewSurface {
        // selectedMediaIndex mirrors HeroCarousel's own initial page (always 0 — see
        // rememberPagerState there); WhatsNewDetailViewModel only advances this via the carousel's
        // own onPageChanged callback, so a preview never wants a value the real pager can't start at.
        WhatsNewDetailContent(
            state = WhatsNewDetailUiState(entry = previewEntryMultiMedia, selectedMediaIndex = 0),
            onBack = {},
            onSelectMedia = {},
            onShare = {},
            onOpenLink = {},
            onContact = { _, _ -> },
            sharedTransitionScope = null,
            animatedContentScope = null,
        )
    }
}
