package com.mileway.feature.tracking.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.platform.PermissionTierId
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_continue
import com.mileway.core.ui.resources.tracking_action_close
import com.mileway.core.ui.resources.tracking_action_not_now
import com.mileway.core.ui.resources.tracking_action_retry
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.tracking.ui.sheets.PermissionOnboardingSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The onboarding that decides whether the app can record anything at all. Shown once, at the moment
 * the caller chooses (a first-run flow, or right before the very first "Start tracking" tap — never
 * inside `TrackMilesScreen`'s own optional-tier prompt, which fires only after tracking is already
 * active and assumes the required tier is already satisfied some other way). Replaces the old
 * full-screen `SetupGuideScreen` checklist, which a previous audit found unreachable.
 *
 * Drives [PermissionPrimerController.stage] through three phases: an upfront explainer
 * ([PrimerStage.Intro]), one system-permission ask per tier (delegated straight to the existing
 * [PermissionOnboardingSheet] — extended via composition here, not duplicated), then a single honest
 * terminal screen ([PrimerStage.Done]) covering every real outcome.
 *
 * @param onOpenAppSettings deep-links to this app's system settings page (Android:
 *   `ACTION_APPLICATION_DETAILS_SETTINGS` with a `package:` URI, exactly what
 *   `feature:profile`'s `SettingsScreen`/`PreferencesScreen` already do for the same purpose; iOS:
 *   `UIApplication.openSettingsURLString` via the existing `UrlOpener`). Required — this is the only
 *   recovery path once a permission is permanently denied, since re-requesting in-app is a no-op.
 * @param onOpenBatterySettings opens this OEM's battery/auto-start exemption screen. Android has no
 *   single intent for this across manufacturers (unlike app-details settings); wire it per-OEM the
 *   same way `onOpenAppSettings` is wired, or fall back to `onOpenAppSettings` if a dedicated deep
 *   link isn't worth building yet.
 * @param onFinished called once the user has acted on the terminal screen (continue / not now / OK) —
 *   the host dismisses the sheet and, for [PrimerOutcome.RestrictedByPolicy] or repeated
 *   [PrimerOutcome.Denied], should let the user proceed without tracking rather than blocking them here.
 */
@Composable
fun PermissionPrimerSheet(
    controller: PermissionPrimerController,
    oemHint: String?,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit = onOpenAppSettings,
    onFinished: () -> Unit,
) {
    val stage by controller.stage.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(controller) { controller.start() }

    when (val s = stage) {
        PrimerStage.Intro ->
            PermissionPrimerIntroSheet(onContinue = controller::beginRequesting)

        is PrimerStage.Requesting ->
            PermissionOnboardingSheet(
                tier = s.tier,
                oemHint = if (s.tier.id == PermissionTierId.BACKGROUND_LOCATION) oemHint else null,
                onGrant = { scope.launch { controller.requestCurrent() } },
                onSkip = controller::skipCurrent,
            )

        is PrimerStage.Done ->
            PermissionPrimerOutcomeSheet(
                outcome = s.outcome,
                oemHint = oemHint,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onRetry = { scope.launch { controller.retry() } },
                onRecheck = { scope.launch { controller.recheckAfterSettings() } },
                onFinished = onFinished,
            )
    }
}

/**
 * Explain-then-ask: WHY location is needed and WHAT is recorded, before the first system dialog ever
 * appears. Asking before explaining converts far worse, and on both platforms a denied background-location
 * prompt is expensive to recover from (see [PrimerOutcome.PermanentlyDenied]) — this screen is the
 * one chance to earn the ask instead of triggering a reflexive "Deny".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionPrimerIntroSheet(onContinue: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
            )
            Text(
                text = "Mileway needs your location to work",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text =
                    "While you're tracking a trip, Mileway records your route and distance so it can " +
                        "log your mileage automatically. If you also allow background access, that " +
                        "continues when the app isn't on screen — but only during an active trip. " +
                        "Mileway never records your location when you aren't tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.button,
            ) {
                Text(stringResource(Res.string.action_continue), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** One (icon, title, body, [action-slot]) triple per [PrimerOutcome] — the honest terminal screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionPrimerOutcomeSheet(
    outcome: PrimerOutcome,
    oemHint: String?,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRetry: () -> Unit,
    onRecheck: () -> Unit,
    onFinished: () -> Unit,
) {
    val (icon, title, body) = outcomeCopy(outcome)
    val showBatteryHint = oemHint != null && (outcome == PrimerOutcome.FullyGranted || outcome == PrimerOutcome.ForegroundOnly)

    ModalBottomSheet(
        onDismissRequest = onFinished,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (outcome == PrimerOutcome.FullyGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (showBatteryHint && oemHint != null) {
                Spacer(Modifier.height(DesignTokens.Spacing.l))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.BatteryFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = DesignTokens.Spacing.xs),
                        )
                        Text(
                            text = "Before you go",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.xs))
                    Text(
                        text = oemHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    OutlinedButton(onClick = onOpenBatterySettings, shape = DesignTokens.Shape.button) {
                        Text("Open Battery Settings")
                    }
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))

            when (outcome) {
                PrimerOutcome.FullyGranted, PrimerOutcome.ForegroundOnly ->
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.button,
                    ) {
                        Text(stringResource(Res.string.action_continue), fontWeight = FontWeight.Bold)
                    }

                PrimerOutcome.Denied -> {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.button,
                    ) {
                        Text(stringResource(Res.string.tracking_action_retry), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    TextButton(onClick = onFinished) {
                        Text(stringResource(Res.string.tracking_action_not_now))
                    }
                }

                PrimerOutcome.PermanentlyDenied -> {
                    Button(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.button,
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = DesignTokens.Spacing.xs))
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    OutlinedButton(
                        onClick = onRecheck,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.button,
                    ) {
                        Text("I've enabled it")
                    }
                }

                PrimerOutcome.RestrictedByPolicy ->
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.button,
                    ) {
                        Text(stringResource(Res.string.tracking_action_close), fontWeight = FontWeight.Bold)
                    }
            }
        }
    }
}

private data class OutcomeCopy(val icon: ImageVector, val title: String, val body: String)

// ponytail: copy is plain Kotlin strings, not string-resource IDs — matches the existing precedent
// set by PermissionTier.rationale/skipImpact and OemBatteryHints (core/platform), which this screen
// reads from directly. Localizing the whole permission-primer surface is a follow-up, not new debt
// introduced here.
private fun outcomeCopy(outcome: PrimerOutcome): OutcomeCopy =
    when (outcome) {
        PrimerOutcome.FullyGranted ->
            OutcomeCopy(
                icon = Icons.Filled.CheckCircle,
                title = "You're all set",
                body = "Mileway will record your location automatically while a trip is being tracked — in the background too.",
            )

        PrimerOutcome.ForegroundOnly ->
            OutcomeCopy(
                icon = Icons.Filled.WarningAmber,
                title = "Tracking will pause in the background",
                body =
                    "Trips will stop recording if you switch apps or lock your phone mid-journey. " +
                        "You can turn on background access anytime from Settings.",
            )

        PrimerOutcome.Denied ->
            OutcomeCopy(
                icon = Icons.Filled.ErrorOutline,
                title = "Location access is required",
                body = "Mileway can't track a trip without location. You can try again, or continue without tracking for now.",
            )

        PrimerOutcome.PermanentlyDenied ->
            OutcomeCopy(
                icon = Icons.Filled.ErrorOutline,
                title = "Location was turned off",
                body =
                    "Mileway can't ask again in-app — Android/iOS blocks repeat prompts after a second " +
                        "denial. Turn location back on for Mileway from Settings, then come back here.",
            )

        PrimerOutcome.RestrictedByPolicy ->
            OutcomeCopy(
                icon = Icons.Filled.Block,
                title = "Location is blocked by your organization",
                body = "A device policy on this phone prevents Mileway from accessing location. Contact your IT admin to enable it for this app.",
            )
    }
