package com.mileway.feature.whatsnew.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.model.UpdateRequirement
import com.mileway.feature.whatsnew.model.UpdateUrgency
import com.mileway.feature.whatsnew.resources.Res
import com.mileway.feature.whatsnew.resources.whatsnew_update_action
import com.mileway.feature.whatsnew.resources.whatsnew_update_forced_body
import com.mileway.feature.whatsnew.resources.whatsnew_update_forced_title
import com.mileway.feature.whatsnew.resources.whatsnew_update_later
import com.mileway.feature.whatsnew.resources.whatsnew_update_optional_title
import com.mileway.feature.whatsnew.resources.whatsnew_update_ready_body
import com.mileway.feature.whatsnew.resources.whatsnew_update_ready_title
import com.mileway.feature.whatsnew.resources.whatsnew_update_recommended_title
import com.mileway.feature.whatsnew.resources.whatsnew_update_release_notes_label
import com.mileway.feature.whatsnew.resources.whatsnew_update_restart
import org.jetbrains.compose.resources.stringResource

/**
 * Renders an [UpdateRequirement]: FORCED → full-screen blocking wall (no dismiss/back — see
 * below), OPTIONAL/RECOMMENDED → a dismissible sheet, NONE → nothing. Presentational only — no
 * Koin/ViewModel wiring, no in-app-update polling; a caller (e.g. `UpdateChecker.check()`, or a
 * future task that wires this into the app shell) supplies the already-derived [requirement].
 *
 * Reuses core:ui's [ActionConfirmationBottomSheet] for the non-forced case instead of a second
 * bespoke `ModalBottomSheet` — same dismissible-sheet-with-two-buttons shape this app already has
 * everywhere else.
 *
 * No back-press interception on the forced wall: mirrors core/ui's `UpdateGate.ForcedUpdateWall`
 * convention already in this app — the wall has no navigation destination to pop back to, so
 * system back simply exits rather than revealing gated content. `androidx.activity.compose.
 * BackHandler` is Android-only in this codebase's commonMain today; adding a commonMain
 * predictive-back dependency for this one screen isn't warranted (no new dependency needed).
 */
@Composable
fun UpdateRequirementContent(
    requirement: UpdateRequirement,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (requirement.urgency) {
        UpdateUrgency.NONE -> Unit
        UpdateUrgency.FORCED -> ForcedUpdateWall(requirement, onUpdate, modifier)
        UpdateUrgency.OPTIONAL, UpdateUrgency.RECOMMENDED -> OptionalUpdateSheet(requirement, onUpdate, onLater)
    }
}

@Composable
private fun ForcedUpdateWall(
    requirement: UpdateRequirement,
    onUpdate: () -> Unit,
    modifier: Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DesignTokens.Spacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.whatsnew_update_forced_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = DesignTokens.Spacing.xl),
            )
            Text(
                text = stringResource(Res.string.whatsnew_update_forced_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = DesignTokens.Spacing.m),
            )
            if (requirement.info.releaseNotes.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.whatsnew_update_release_notes_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.l),
                )
                Text(
                    text = requirement.info.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
                )
            }
            Button(
                onClick = onUpdate,
                shape = DesignTokens.Shape.button,
                modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.xxl),
            ) {
                Text(stringResource(Res.string.whatsnew_update_action))
            }
        }
    }
}

@Composable
private fun OptionalUpdateSheet(
    requirement: UpdateRequirement,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val recommended = requirement.urgency == UpdateUrgency.RECOMMENDED
    val marketingVersion = requirement.info.latestMarketingVersion
    ActionConfirmationBottomSheet(
        title = stringResource(if (recommended) Res.string.whatsnew_update_recommended_title else Res.string.whatsnew_update_optional_title),
        description = marketingVersion.takeIf { it.isNotBlank() }?.let { "Version $it" },
        confirmLabel = stringResource(Res.string.whatsnew_update_action),
        dismissLabel = stringResource(Res.string.whatsnew_update_later),
        icon = Icons.Rounded.SystemUpdate,
        tone = if (recommended) ActionConfirmationToneType.Warning else ActionConfirmationToneType.Info,
        onConfirm = { onUpdate() },
        onDismiss = onLater,
    ) {
        if (requirement.info.releaseNotes.isNotBlank()) {
            Text(
                text = requirement.info.releaseNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Compact, non-blocking prompt for the Play flexible-update "downloaded, restart to install"
 * state ([com.mileway.feature.whatsnew.data.UpdateChecker.readyToRestart]) — distinct from
 * [UpdateRequirementContent] because "ready to restart" isn't an [UpdateUrgency]: it's a follow-up
 * to an update the user already started, not a new ask.
 */
@Composable
fun UpdateReadyToRestartBanner(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(DesignTokens.Spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.whatsnew_update_ready_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(Res.string.whatsnew_update_ready_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRestart) {
                Text(stringResource(Res.string.whatsnew_update_restart))
            }
        }
    }
}
