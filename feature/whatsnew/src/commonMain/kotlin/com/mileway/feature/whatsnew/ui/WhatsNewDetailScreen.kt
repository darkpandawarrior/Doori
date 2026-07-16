package com.mileway.feature.whatsnew.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.platform.UrlOpener
import com.mileway.core.ui.platform.LocalShareSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.whatsnew_cd_share
import com.mileway.core.ui.resources.whatsnew_detail_learn_more
import com.mileway.core.ui.resources.whatsnew_detail_not_found_subtitle
import com.mileway.core.ui.resources.whatsnew_detail_not_found_title
import com.mileway.core.ui.resources.whatsnew_detail_step
import com.mileway.core.ui.resources.whatsnew_detail_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.feature.whatsnew.ui.components.ContactSection
import com.mileway.feature.whatsnew.ui.components.HeroCarousel
import com.mileway.feature.whatsnew.ui.components.ReleasedDateFormat
import com.mileway.feature.whatsnew.ui.components.ReleasedDateTag
import com.mileway.feature.whatsnew.viewmodel.WhatsNewDetailUiState
import com.mileway.feature.whatsnew.viewmodel.WhatsNewDetailViewModel
import kotlinx.datetime.format
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * PLAN_V36 P4 — spec §5.2, full parity: pinned header ("Feature details", subtitle = step count
 * or release date, Share action) → hero carousel → RELEASED chip → title/description → optional
 * "Learn more" link → optional contact section. Replaces the P3 placeholder.
 */
@Composable
fun WhatsNewDetailScreen(
    entryId: String,
    onBack: () -> Unit,
    viewModel: WhatsNewDetailViewModel = koinViewModel { parametersOf(entryId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shareSheet = LocalShareSheet.current
    val urlOpener = koinInject<UrlOpener>()

    WhatsNewDetailContent(
        state = state,
        onBack = onBack,
        onSelectMedia = viewModel::selectMedia,
        onShare = { entry -> shareSheet.share(text = buildShareText(entry), subject = entry.title) },
        onOpenLink = urlOpener::open,
        onContact = { email, subject -> urlOpener.open(mailtoUri(email, subject)) },
    )
}

@Composable
private fun WhatsNewDetailContent(
    state: WhatsNewDetailUiState,
    onBack: () -> Unit,
    onSelectMedia: (Int) -> Unit,
    onShare: (WhatsNewEntry) -> Unit,
    onOpenLink: (String) -> Unit,
    onContact: (email: String, subject: String) -> Unit,
) {
    val entry = state.entry
    Column(modifier = Modifier.fillMaxSize()) {
        WhatsNewDetailHeader(
            entry = entry,
            mediaPage = state.selectedMediaIndex,
            onBack = onBack,
            onShare = { entry?.let(onShare) },
        )

        if (entry == null) {
            WhatsNewDetailNotFound(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                if (entry.media.isNotEmpty()) {
                    Surface(
                        shape = DesignTokens.Shape.carouselCard,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        HeroCarousel(
                            media = entry.media,
                            onPageChanged = onSelectMedia,
                            modifier = Modifier.padding(DesignTokens.Spacing.s),
                        )
                    }
                }

                ReleasedDateTag(entry.releasedOn)

                Text(text = entry.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Text(text = entry.description, style = MaterialTheme.typography.bodyLarge)

                val link = entry.link
                if (link != null) {
                    FilledTonalButton(
                        shape = DesignTokens.Shape.button,
                        onClick = { onOpenLink(link) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.whatsnew_detail_learn_more))
                    }
                }

                val contactEmail = entry.contactEmail
                if (contactEmail != null) {
                    ContactSection(
                        email = contactEmail,
                        onContact = { onContact(contactEmail, entry.title) },
                    )
                }
            }
        }
    }
}

/** Pinned header, fixed-contrast against the primary gradient — same shape as `WhatsNewListHeader`. */
@Composable
private fun WhatsNewDetailHeader(
    entry: WhatsNewEntry?,
    mediaPage: Int,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    val headerContent = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(DesignTokens.topBarGradientBrush())
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.core_cd_back), tint = headerContent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.whatsnew_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = headerContent,
                )
                if (entry != null) {
                    Text(
                        text = detailSubtitle(entry, mediaPage),
                        style = MaterialTheme.typography.bodySmall,
                        color = headerContent.copy(alpha = 0.85f),
                    )
                }
            }
            if (entry != null) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(Res.string.whatsnew_cd_share), tint = headerContent)
                }
            }
        }
    }
}

/** "Step X of N" for multi-media entries, else the plain release date (the RELEASED chip below already says "Released"). */
@Composable
private fun detailSubtitle(
    entry: WhatsNewEntry,
    mediaPage: Int,
): String =
    if (entry.media.size > 1) {
        stringResource(Res.string.whatsnew_detail_step, mediaPage + 1, entry.media.size)
    } else {
        entry.releasedOn.format(ReleasedDateFormat)
    }

@Composable
private fun WhatsNewDetailNotFound(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
        ) {
            Text(stringResource(Res.string.whatsnew_detail_not_found_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                stringResource(Res.string.whatsnew_detail_not_found_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildShareText(entry: WhatsNewEntry): String =
    buildString {
        append(entry.title)
        append("\n\n")
        append(entry.description)
        entry.link?.let {
            append("\n")
            append(it)
        }
    }

// ponytail: space-only escaping — catalog subjects are short marketing titles, not free-form text.
// Upgrade to full percent-encoding if a title ever needs other reserved mailto/URI characters.
private fun mailtoUri(
    email: String,
    subject: String,
): String = "mailto:$email?subject=${subject.replace(" ", "%20")}"
