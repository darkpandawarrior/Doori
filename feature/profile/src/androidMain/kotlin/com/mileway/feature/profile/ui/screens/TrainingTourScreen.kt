package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.engagement.TourStatus
import com.mileway.core.ui.components.ConfettiBurst
import com.mileway.core.ui.components.coachmark.CoachMarkController
import com.mileway.core.ui.components.coachmark.CoachMarkOverlay
import com.mileway.core.ui.components.coachmark.CoachStep
import com.mileway.core.ui.components.coachmark.coachMarkTarget
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tour_back
import com.mileway.core.ui.resources.tour_control_pause
import com.mileway.core.ui.resources.tour_control_start
import com.mileway.core.ui.resources.tour_control_stop
import com.mileway.core.ui.resources.tour_control_submit
import com.mileway.core.ui.resources.tour_distance_value
import com.mileway.core.ui.resources.tour_finish
import com.mileway.core.ui.resources.tour_hud_distance
import com.mileway.core.ui.resources.tour_hud_duration
import com.mileway.core.ui.resources.tour_next
import com.mileway.core.ui.resources.tour_purpose_business
import com.mileway.core.ui.resources.tour_purpose_personal
import com.mileway.core.ui.resources.tour_skip
import com.mileway.core.ui.resources.tour_step_classify_body
import com.mileway.core.ui.resources.tour_step_classify_title
import com.mileway.core.ui.resources.tour_step_complete_body
import com.mileway.core.ui.resources.tour_step_complete_title
import com.mileway.core.ui.resources.tour_step_hud_body
import com.mileway.core.ui.resources.tour_step_hud_title
import com.mileway.core.ui.resources.tour_step_intro_body
import com.mileway.core.ui.resources.tour_step_intro_title
import com.mileway.core.ui.resources.tour_step_pause_body
import com.mileway.core.ui.resources.tour_step_pause_title
import com.mileway.core.ui.resources.tour_step_start_body
import com.mileway.core.ui.resources.tour_step_start_title
import com.mileway.core.ui.resources.tour_step_stop_body
import com.mileway.core.ui.resources.tour_step_stop_title
import com.mileway.core.ui.resources.tour_step_submit_body
import com.mileway.core.ui.resources.tour_step_submit_title
import com.mileway.core.ui.resources.tour_surface_title
import com.mileway.core.ui.resources.tour_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.viewmodel.TourPurpose
import com.mileway.feature.profile.viewmodel.TrainingTourViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P12.5 — the interactive training tour surface. A self-contained mock tracking screen
 * (Mileway design language) whose Start/HUD/Pause/Stop/Classify/Submit elements are the anchors for
 * an eight-step coach-mark walkthrough. The tour drives a locally simulated distance ramp between
 * steps (never the live GPS/location layer); completing it awards the P12.1 "Tour complete" badge
 * and fires the P6.1 confetti. Skipping (or finishing) navigates back via [onBack].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTourScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingTourViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Skipping is terminal — leave the tour once the state machine records it.
    if (state.status == TourStatus.SKIPPED) {
        onBack()
        return
    }

    val controller = remember { CoachMarkController() }
    val steps = tourCoachSteps()
    controller.steps = steps
    controller.currentIndex = state.step.ordinal.coerceIn(0, steps.lastIndex)
    controller.onNext = viewModel::next
    controller.onSkip = viewModel::skip

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.tour_title),
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.tour_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MockTrackingSurface(
                controller = controller,
                distanceKm = state.distanceKm,
                durationSec = state.durationSec,
                purpose = state.purpose,
                onSelectPurpose = viewModel::selectPurpose,
            )

            if (state.status == TourStatus.COMPLETED) {
                ConfettiBurst(modifier = Modifier.fillMaxSize())
                TourCompletionCard(onFinish = onBack)
            } else {
                CoachMarkOverlay(controller = controller, skipLabel = stringResource(Res.string.tour_skip))
            }
        }
    }
}

@Composable
private fun MockTrackingSurface(
    controller: CoachMarkController,
    distanceKm: Double,
    durationSec: Int,
    purpose: TourPurpose,
    onSelectPurpose: (TourPurpose) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Text(stringResource(Res.string.tour_surface_title), style = MaterialTheme.typography.titleMedium)

        // Live HUD.
        Card(
            modifier = Modifier.fillMaxWidth().coachMarkTarget("hud", controller),
            shape = DesignTokens.Shape.roundedLg,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                HudStat(
                    label = stringResource(Res.string.tour_hud_distance),
                    value = stringResource(Res.string.tour_distance_value, oneDecimal(distanceKm)),
                    modifier = Modifier.weight(1f),
                )
                HudStat(
                    label = stringResource(Res.string.tour_hud_duration),
                    value = formatDuration(durationSec),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Trip controls.
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            ControlButton(Icons.Filled.PlayArrow, stringResource(Res.string.tour_control_start), Modifier.coachMarkTarget("start", controller))
            ControlButton(Icons.Filled.Pause, stringResource(Res.string.tour_control_pause), Modifier.coachMarkTarget("pause", controller))
            ControlButton(Icons.Filled.Stop, stringResource(Res.string.tour_control_stop), Modifier.coachMarkTarget("stop", controller))
        }

        // Classification chips.
        Row(
            modifier = Modifier.coachMarkTarget("classify", controller),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            FilterChip(
                selected = purpose == TourPurpose.BUSINESS,
                onClick = { onSelectPurpose(TourPurpose.BUSINESS) },
                label = { Text(stringResource(Res.string.tour_purpose_business)) },
            )
            FilterChip(
                selected = purpose == TourPurpose.PERSONAL,
                onClick = { onSelectPurpose(TourPurpose.PERSONAL) },
                label = { Text(stringResource(Res.string.tour_purpose_personal)) },
            )
        }

        // Submit.
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().coachMarkTarget("submit", controller),
        ) {
            Text(stringResource(Res.string.tour_control_submit))
        }
    }
}

@Composable
private fun HudStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = {}, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = DesignTokens.Spacing.s))
    }
}

@Composable
private fun TourCompletionCard(onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(DesignTokens.Spacing.xl), contentAlignment = Alignment.Center) {
        Card(shape = DesignTokens.Shape.roundedLg) {
            Column(
                modifier = Modifier.padding(DesignTokens.Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Text(
                    stringResource(Res.string.tour_step_complete_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(Res.string.tour_step_complete_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onFinish) { Text(stringResource(Res.string.tour_finish)) }
            }
        }
    }
}

@Composable
private fun tourCoachSteps(): List<CoachStep> {
    val next = stringResource(Res.string.tour_next)
    return listOf(
        CoachStep(null, stringResource(Res.string.tour_step_intro_title), stringResource(Res.string.tour_step_intro_body), next),
        CoachStep("start", stringResource(Res.string.tour_step_start_title), stringResource(Res.string.tour_step_start_body), next),
        CoachStep("hud", stringResource(Res.string.tour_step_hud_title), stringResource(Res.string.tour_step_hud_body), next),
        CoachStep("pause", stringResource(Res.string.tour_step_pause_title), stringResource(Res.string.tour_step_pause_body), next),
        CoachStep("stop", stringResource(Res.string.tour_step_stop_title), stringResource(Res.string.tour_step_stop_body), next),
        CoachStep("classify", stringResource(Res.string.tour_step_classify_title), stringResource(Res.string.tour_step_classify_body), next),
        CoachStep("submit", stringResource(Res.string.tour_step_submit_title), stringResource(Res.string.tour_step_submit_body), next),
        // COMPLETE is rendered by the completion card, not the coach overlay — placeholder for the index.
        CoachStep(null, "", "", next),
    )
}

private fun oneDecimal(v: Double): String {
    val scaled = (v * 10).toLong()
    return "${scaled / 10}.${scaled % 10}"
}

private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
