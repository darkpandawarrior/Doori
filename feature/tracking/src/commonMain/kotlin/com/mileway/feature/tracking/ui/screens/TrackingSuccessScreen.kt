package com.mileway.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.ConfettiBurst
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_insights_distance
import com.mileway.core.ui.resources.tracking_success_add_to_claim
import com.mileway.core.ui.resources.tracking_success_cd_badge
import com.mileway.core.ui.resources.tracking_success_cd_view_expense
import com.mileway.core.ui.resources.tracking_success_policy_issues
import com.mileway.core.ui.resources.tracking_success_reimbursable
import com.mileway.core.ui.resources.tracking_success_title
import com.mileway.core.ui.resources.tracking_success_track_new
import com.mileway.core.ui.resources.tracking_success_txn_id
import com.mileway.core.ui.resources.tracking_success_view_expense
import com.mileway.core.ui.resources.tracking_success_voucher
import com.mileway.core.ui.resources.tracking_voucher_create_button
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round

/**
 * Celebratory success screen shown after a mileage expense is submitted.
 *
 * This is a fully **stateless** composable: every value it renders arrives via
 * parameters and every user intent leaves via a callback (unidirectional data
 * flow). It owns no [androidx.lifecycle.ViewModel], navigation, or DI wiring,
 * the integrator supplies the data and handles the events. As a result the
 * screen is trivially previewable and testable in isolation.
 *
 * Visual structure (top to bottom), matching the reference design:
 *  1. A spring-animated, gently pulsing success badge with a check mark.
 *  2. The "Expense Submitted Successfully" title.
 *  3. A red "N Policy Issue(s) Found" summary chip (only when [violationCount] > 0).
 *  4. A prominent Distance card with an animated count-up of [distanceKm].
 *  5. A clickable Transaction card: transaction id, service tag, and date line.
 *  6. A red-tinted Policy Issues card carrying [violationMessage] (when violations exist).
 *  7. A voucher section (when [voucherNumber] != null) with "Add to Claim" /
 *     "Create Voucher" actions.
 *  8. A bottom action bar with the primary "Track New Journey" CTA and an
 *     optional secondary "View Expense" button.
 *
 * Cards animate in with a small stagger so the screen assembles itself, and a
 * one-shot [ConfettiBurst] fires on first composition unless the submission was
 * a hard stop.
 *
 * @param distanceKm Tracked distance in kilometres (drives the animated counter).
 * @param reimbursableAmount Reimbursable portion of the expense; a value > 0 shows the row.
 * @param vehicleName Human-readable vehicle/service label shown in the transaction card.
 * @param startTime Journey start time, epoch millis (used for the date line).
 * @param endTime Journey end time, epoch millis (used for the date line).
 * @param transactionId Ledger transaction id, or null when none was issued.
 * @param submissionStatus Name of the submission status enum (e.g. "POLICY_VIOLATION",
 *   "HARD_STOP", "SUCCESS"). Anything other than a hard stop is treated as a
 *   celebratory outcome and triggers the confetti.
 * @param violationCount Number of policy issues; > 0 reveals the chip and red card.
 * @param violationMessage First violation's message, or null.
 * @param voucherNumber Voucher number; non-null reveals the voucher section and switches
 *   the primary CTA to "Add to Claim".
 * @param voucherAmount Monetary amount associated with the voucher.
 * @param onTrackNewJourney Invoked by the primary CTA (default flow).
 * @param onViewExpense Invoked when the user opens the transaction (card tap or secondary CTA).
 * @param onCreateVoucher Invoked from the voucher section's "Create Voucher" action.
 * @param modifier Applied to the screen's root content column.
 */
@Composable
fun TrackingSuccessScreen(
    distanceKm: Double,
    reimbursableAmount: Double,
    vehicleName: String,
    startTime: Long,
    endTime: Long,
    transactionId: String?,
    submissionStatus: String,
    violationCount: Int,
    violationMessage: String?,
    voucherNumber: String?,
    voucherAmount: Double,
    onTrackNewJourney: () -> Unit,
    onViewExpense: () -> Unit,
    onCreateVoucher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasViolations = violationCount > 0
    val hasTransaction = !transactionId.isNullOrBlank()
    val hasVoucher = voucherNumber != null
    // A hard stop is the only non-celebratory outcome, every other status earns confetti.
    val isHardStop = submissionStatus.equals("HARD_STOP", ignoreCase = true)

    // Date line: prefer the journey window when both ends are present, else fall back
    // to whichever timestamp we have.
    val dateLine =
        remember(startTime, endTime) {
            when {
                startTime > 0L && endTime > 0L && endTime != startTime ->
                    "${DateUtils.epochToDateTime(startTime)}  •  ${DateUtils.epochToTime12h(endTime)}"
                startTime > 0L -> DateUtils.epochToDateTime(startTime)
                endTime > 0L -> DateUtils.epochToDateTime(endTime)
                else -> ""
            }
        }

    val density = LocalDensity.current

    // ── Stagger driver ────────────────────────────────────────────────────────
    // Each visible block has its own transition state that we flip in sequence so
    // the screen assembles top-to-bottom rather than appearing all at once.
    val iconState = remember { MutableTransitionState(false) }
    val titleState = remember { MutableTransitionState(false) }
    val distanceCardState = remember { MutableTransitionState(false) }
    val transactionCardState = remember { MutableTransitionState(false) }
    val policyCardState = remember { MutableTransitionState(false) }
    val voucherCardState = remember { MutableTransitionState(false) }
    val buttonsState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        iconState.targetState = true
        delay(300)
        titleState.targetState = true
        delay(200)
        distanceCardState.targetState = true
        delay(100)
        transactionCardState.targetState = true
        delay(100)
        if (hasViolations) policyCardState.targetState = true
        delay(100)
        if (hasVoucher) voucherCardState.targetState = true
        delay(200)
        buttonsState.targetState = true
    }

    // Spring pop for the badge ring + check mark.
    val checkScale by animateFloatAsState(
        targetValue = if (iconState.targetState) 1f else 0f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "checkScale",
    )

    // Continuous gentle pulse for the outer glow ring.
    val pulseTransition = updateTransition(targetState = true, label = "pulseTransition")
    val pulse by pulseTransition.animateFloat(
        transitionSpec = {
            repeatable(
                iterations = Int.MAX_VALUE,
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "pulse",
    ) { if (it) 1.05f else 0.95f }

    // Soft vertical gradient that lightens toward the bottom for depth.
    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
            startY = 0f,
            endY = 1500f,
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(
                    visibleState = buttonsState,
                    enter =
                        fadeIn(animationSpec = tween(700)) +
                            slideInVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                initialOffsetY = { with(density) { 40.dp.roundToPx() } },
                            ),
                ) {
                    SuccessActionBar(
                        hasVoucher = hasVoucher,
                        hasTransaction = hasTransaction,
                        onTrackNewJourney = onTrackNewJourney,
                        onViewExpense = onViewExpense,
                        onCreateVoucher = onCreateVoucher,
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(backgroundBrush),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier =
                        modifier
                            .verticalScroll(rememberScrollState())
                            .padding(innerPadding)
                            .padding(vertical = DesignTokens.Spacing.l)
                            .widthIn(max = 600.dp) // keep line lengths comfortable on tablets
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    // 1 ─ Success badge ───────────────────────────────────────
                    AnimatedVisibility(
                        visibleState = iconState,
                        enter =
                            fadeIn(animationSpec = tween(500)) +
                                slideInVertically(
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    initialOffsetY = { with(density) { -40.dp.roundToPx() } },
                                ),
                    ) {
                        SuccessBadge(checkScale = checkScale, pulse = pulse)
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))

                    // 2 + 3 ─ Title and policy chip ───────────────────────────
                    AnimatedVisibility(
                        visibleState = titleState,
                        enter =
                            fadeIn(animationSpec = tween(700)) +
                                expandVertically(animationSpec = tween(700)),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(Res.string.tracking_success_title),
                                style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 26.sp,
                                        letterSpacing = (-0.5).sp,
                                    ),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.xl),
                            )

                            if (hasViolations) {
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))
                                PolicyIssueChip(violationCount = violationCount)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.l))

                    // 4 ─ Distance card ───────────────────────────────────────
                    AnimatedVisibility(
                        visibleState = distanceCardState,
                        enter =
                            fadeIn(animationSpec = tween(400)) +
                                slideInVertically(
                                    animationSpec = tween(400),
                                    initialOffsetY = { it / 4 },
                                ),
                    ) {
                        DistanceCard(distanceKm = distanceKm)
                    }

                    // 5 ─ Transaction card ────────────────────────────────────
                    if (hasTransaction) {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))
                        AnimatedVisibility(
                            visibleState = transactionCardState,
                            enter =
                                fadeIn(animationSpec = tween(400)) +
                                    slideInVertically(
                                        animationSpec = tween(400),
                                        initialOffsetY = { it / 4 },
                                    ),
                        ) {
                            TransactionCard(
                                transactionId = transactionId,
                                vehicleName = vehicleName,
                                reimbursableAmount = reimbursableAmount,
                                dateLine = dateLine,
                                onClick = onViewExpense,
                            )
                        }
                    }

                    // 6 ─ Policy issues card ──────────────────────────────────
                    if (hasViolations) {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))
                        AnimatedVisibility(
                            visibleState = policyCardState,
                            enter =
                                fadeIn(animationSpec = tween(400)) +
                                    slideInVertically(
                                        animationSpec = tween(400),
                                        initialOffsetY = { it / 4 },
                                    ),
                        ) {
                            PolicyIssuesCard(
                                violationCount = violationCount,
                                violationMessage = violationMessage,
                            )
                        }
                    }

                    // 7 ─ Voucher section ─────────────────────────────────────
                    if (hasVoucher) {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))
                        AnimatedVisibility(
                            visibleState = voucherCardState,
                            enter =
                                fadeIn(animationSpec = tween(400)) +
                                    slideInVertically(
                                        animationSpec = tween(400),
                                        initialOffsetY = { it / 4 },
                                    ),
                        ) {
                            VoucherCard(
                                voucherNumber = voucherNumber,
                                voucherAmount = voucherAmount,
                                onCreateVoucher = onCreateVoucher,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.l))
                }
            }
        }

        // Confetti overlay, fires once on first composition for any non-hard-stop
        // outcome. It draws above the Scaffold so particles fall across the whole
        // screen, and removes itself after the burst.
        if (!isHardStop) {
            ConfettiBurst(
                modifier = Modifier.fillMaxSize(),
                particleCount = 50,
                durationMs = 1800,
            )
        }
    }
}

/** Animated, gently pulsing circular badge with a primary-tinted check mark. */
@Composable
private fun SuccessBadge(
    checkScale: Float,
    pulse: Float,
) {
    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .scale(pulse)
                    .alpha(0.3f)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    Color.Transparent,
                                ),
                        ),
                        shape = DesignTokens.Shape.button,
                    ),
        )

        // Middle outline ring
        Box(
            modifier =
                Modifier
                    .size(70.dp)
                    .scale(checkScale)
                    .border(
                        width = 2.dp,
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    ),
                            ),
                        shape = DesignTokens.Shape.button,
                    ),
        )

        // Filled check container
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .scale(checkScale)
                    .shadow(
                        elevation = DesignTokens.Elevation.prominent,
                        shape = DesignTokens.Shape.button,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    )
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surface,
                                    ),
                            ),
                        shape = DesignTokens.Shape.button,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(Res.string.tracking_success_cd_badge),
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Compact red chip summarising the number of policy issues found. */
@Composable
private fun PolicyIssueChip(violationCount: Int) {
    Row(
        modifier =
            Modifier
                .padding(horizontal = DesignTokens.Spacing.l)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    DesignTokens.Shape.button,
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(DesignTokens.IconSize.inline),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$violationCount ${if (violationCount == 1) "Policy Issue" else "Policy Issues"} Found",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** Prominent card with the headline distance and an animated count-up. */
@Composable
private fun DistanceCard(distanceKm: Double) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        shape = DesignTokens.Shape.roundedMd,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = DesignTokens.Elevation.card,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.tracking_insights_distance),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp,
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.s))

            // Count-up: ramp from 0 to the final value over ~1.5s for a lively reveal.
            var animatedDistance by remember { mutableDoubleStateOf(0.0) }
            LaunchedEffect(distanceKm) {
                val steps = 100
                val stepDuration = 1500L / steps
                for (i in 0..steps) {
                    animatedDistance = distanceKm * i / steps
                    delay(stepDuration)
                }
                animatedDistance = distanceKm
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = (round(animatedDistance * 100).toLong() / 100.0).toString(),
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 48.sp,
                            letterSpacing = (-0.5).sp,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.xs))
                Text(
                    text = "km",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = DesignTokens.Spacing.s),
                )
            }
        }
    }
}

/**
 * Clickable transaction card: id row with chevron, optional reimbursable amount,
 * a service tag, and a date line. Tapping opens the expense.
 */
@Composable
private fun TransactionCard(
    transactionId: String,
    vehicleName: String,
    reimbursableAmount: Double,
    dateLine: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = DesignTokens.Shape.actionTile,
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = DesignTokens.Elevation.card,
                pressedElevation = DesignTokens.Elevation.raised,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.m + 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.header),
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.m))
                Text(
                    text = stringResource(Res.string.tracking_success_txn_id, transactionId),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(Res.string.tracking_success_cd_view_expense),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }

            // Optional reimbursable amount, highlighted in a tinted pill.
            if (reimbursableAmount > 0.0) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                DesignTokens.Shape.roundedSm,
                            )
                            .padding(DesignTokens.Spacing.m),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.tracking_success_reimbursable),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "₹${round(reimbursableAmount * 100).toLong() / 100.0}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.s + 2.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.s + 2.dp))

            // Service tag
            if (vehicleName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.IconSize.inline),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.s))
                    Text(
                        text = vehicleName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                }
            }

            // Date line
            if (dateLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.xs))
                    Text(
                        text = dateLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/** Red-tinted card listing the policy issues with the first violation's message. */
@Composable
private fun PolicyIssuesCard(
    violationCount: Int,
    violationMessage: String?,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        shape = DesignTokens.Shape.roundedSm,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
            ),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.s),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                DesignTokens.Shape.button,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(DesignTokens.IconSize.inline),
                    )
                }
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.s))
                Column {
                    Text(
                        text = stringResource(Res.string.tracking_success_policy_issues),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "$violationCount ${if (violationCount == 1) "issue" else "issues"} found",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    )
                }
            }

            if (!violationMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        ),
                    shape = DesignTokens.Shape.button,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.Spacing.s))
                        Text(
                            text = violationMessage,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Tertiary-tinted card showing the issued voucher number and amount. */
@Composable
private fun VoucherCard(
    voucherNumber: String,
    voucherAmount: Double,
    onCreateVoucher: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            ),
        shape = DesignTokens.Shape.roundedSm,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            DesignTokens.Shape.button,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(DesignTokens.IconSize.badge),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.tracking_success_voucher),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "#$voucherNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                )
            }
            if (voucherAmount > 0.0) {
                Text(
                    text = "₹${round(voucherAmount * 100).toLong() / 100.0}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

/**
 * Bottom action bar. The primary CTA depends on context:
 *  - With a voucher: "Add to Claim" (primary) + "Create Voucher" (secondary).
 *  - Otherwise: "Track New Journey" (primary) + optional "View Expense" (secondary).
 */
@Composable
private fun SuccessActionBar(
    hasVoucher: Boolean,
    hasTransaction: Boolean,
    onTrackNewJourney: () -> Unit,
    onViewExpense: () -> Unit,
    onCreateVoucher: () -> Unit,
) {
    Surface(
        tonalElevation = DesignTokens.Elevation.prominent,
        shadowElevation = DesignTokens.Elevation.prominent,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                        ),
                    )
                    .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            if (hasVoucher) {
                PrimaryCta(
                    label = stringResource(Res.string.tracking_success_add_to_claim),
                    icon = Icons.Filled.AccountBalance,
                    onClick = onViewExpense,
                )
                SecondaryCta(
                    label = stringResource(Res.string.tracking_voucher_create_button),
                    icon = Icons.Outlined.Description,
                    onClick = onCreateVoucher,
                )
            } else {
                PrimaryCta(
                    label = stringResource(Res.string.tracking_success_track_new),
                    icon = Icons.Filled.DirectionsCar,
                    onClick = onTrackNewJourney,
                )
                if (hasTransaction) {
                    SecondaryCta(
                        label = stringResource(Res.string.tracking_success_view_expense),
                        icon = Icons.Filled.Receipt,
                        onClick = onViewExpense,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryCta(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = DesignTokens.Shape.roundedMd,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.m))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun SecondaryCta(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        shape = DesignTokens.Shape.chip,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
