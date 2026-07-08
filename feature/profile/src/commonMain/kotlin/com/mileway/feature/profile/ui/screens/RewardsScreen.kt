package com.mileway.feature.profile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.rewards.RewardCard
import com.mileway.core.data.rewards.RewardStatus
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.RewardsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Drag coverage at which an unscratched card reveals. */
private const val SCRATCH_THRESHOLD = 0.6f

/**
 * PLAN_V24 P5.3: scratch-card rewards — a grid of earned cards. An unscratched card reveals when
 * dragged over; the cover fades with drag coverage and the reward scales in.
 *
 * ponytail: a genuine drag-to-fade reveal (cover alpha driven by accumulated drag), not a
 * clear-blend erase shader — the erase-mask fights Compose Multiplatform's software layer on the
 * noGms target. Noted in PROGRESS; the shipped interaction reveals reliably on every platform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RewardsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFFEA580C), Color(0xFFB45309))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = rw("rewards_back", "Back"), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            rw("rewards_title", "Scratch Rewards"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            rwArg("rewards_total", "Earned: %1\$d credits", state.totalCredits),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            if (state.cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        rw("rewards_empty", "No rewards yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.Spacing.xl),
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    items(state.cards, key = { it.id }) { card ->
                        RewardCardCell(card = card, onScratch = { viewModel.scratch(card.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardCardCell(
    card: RewardCard,
    onScratch: () -> Unit,
) {
    val revealed = card.status == RewardStatus.SCRATCHED
    var scratched by remember(card.id) { mutableFloatStateOf(0f) }
    val revealScale by animateFloatAsState(if (revealed) 1f else 0.85f, label = "reveal")

    Card(modifier = Modifier.aspectRatio(1f), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Revealed reward underneath.
            Column(
                modifier =
                    Modifier.fillMaxSize().padding(DesignTokens.Spacing.m).graphicsLayer {
                        scaleX = revealScale
                        scaleY = revealScale
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEA580C))
                Text(card.rewardLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(card.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }

            // Scratch cover — fades with drag coverage, then reveals.
            if (!revealed) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .alpha(1f - (scratched / SCRATCH_THRESHOLD).coerceIn(0f, 1f))
                            .background(Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF6B7280))))
                            .pointerInput(card.id) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    scratched += 0.05f
                                    if (scratched >= SCRATCH_THRESHOLD) onScratch()
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        rw("rewards_scratch_hint", "Scratch to reveal"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun rw(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback

@Composable
private fun rwArg(
    key: String,
    fallback: String,
    arg: Int,
): String = Res.allStringResources[key]?.let { stringResource(it, arg) } ?: fallback.replace("%1\$d", arg.toString())
