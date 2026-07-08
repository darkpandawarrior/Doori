package com.mileway.feature.profile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.MembershipViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * PLAN_V24 P6.1: "Mileway Club" surface. Non-members see the join pitch → a real consent sheet
 * (accept activates, decline is a genuine path). Members see a member card (join date) + the
 * benefits list, with a one-time celebration on first activation (a scale-in banner — the confetti
 * flag persists so it never replays). Closes MASTER_GAP:68.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubBenefitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembershipViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConsent by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(state.isMember, state.confettiShown) {
        if (state.isMember && !state.confettiShown) {
            celebrate = true
            viewModel.markConfettiShown()
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB8860B), Color(0xFF6B4E00))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = cb("club_back", "Back"), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cb("club_title", "Mileway Club"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            cb("club_subtitle", "Member perks and benefits"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                if (celebrate) {
                    val scale by animateFloatAsState(if (celebrate) 1f else 0.7f, label = "confetti")
                    Surface(
                        color = Color(0xFF16A34A).copy(alpha = 0.12f),
                        shape = DesignTokens.Shape.roundedMd,
                        modifier =
                            Modifier.fillMaxWidth().graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                    ) {
                        Text(
                            cb("club_confetti", "Welcome to Mileway Club!"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                        )
                    }
                }

                if (!state.isMember) {
                    JoinPitch(onJoin = { showConsent = true })
                } else {
                    MemberCard(activatedAtMs = state.activatedAtMs)
                    Text(cb("club_benefits_heading", "Your benefits"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    (1..4).forEach { i -> BenefitRow(cb("club_benefit_${i}_title", "Benefit"), cb("club_benefit_${i}_desc", "")) }
                }
            }
        }
    }

    if (showConsent) {
        ModalBottomSheet(onDismissRequest = { showConsent = false }, sheetState = sheetState) {
            ConsentContent(
                onAccept = {
                    viewModel.activate()
                    showConsent = false
                },
                onDecline = { showConsent = false },
            )
        }
    }
}

@Composable
private fun JoinPitch(onJoin: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.height(40.dp))
            Text(
                cb("club_join_headline", "Join Mileway Club"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                cb("club_join_body", "Unlock member-only perks."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) { Text(cb("club_join_cta", "Join now")) }
        }
    }
}

@Composable
private fun MemberCard(activatedAtMs: Long?) {
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Row(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFB8860B))
            Spacer(Modifier.width(DesignTokens.Spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(cb("club_member_badge", "Club member"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                activatedAtMs?.let {
                    Text(
                        cbArg("club_member_since", "Member since %1\$s", formatJoinDate(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(
    title: String,
    description: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConsentContent(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(DesignTokens.Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Text(cb("club_consent_title", "Membership terms"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            cb("club_consent_body", "By joining you agree to the terms."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text(cb("club_consent_accept", "I agree — activate")) }
        OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) { Text(cb("club_consent_decline", "Not now")) }
        Spacer(Modifier.height(DesignTokens.Spacing.s))
    }
}

private fun formatJoinDate(ms: Long): String =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
        "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
    }

@Composable
private fun cb(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback

@Composable
private fun cbArg(
    key: String,
    fallback: String,
    arg: String,
): String = Res.allStringResources[key]?.let { stringResource(it, arg) } ?: fallback.replace("%1\$s", arg)
