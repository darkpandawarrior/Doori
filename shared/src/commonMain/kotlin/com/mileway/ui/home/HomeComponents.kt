package com.mileway.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.banner.HomeBanner
import com.mileway.core.ui.components.AutoSizeGreeting
import com.mileway.core.ui.components.CountBadge
import com.mileway.core.ui.components.CurrentLocationPinMap
import com.mileway.core.ui.components.LocationPin
import com.mileway.core.ui.components.ScanlineOverlay
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_dismiss
import com.mileway.core.ui.resources.core_cd_search
import com.mileway.core.ui.resources.shared_home_action_required
import com.mileway.core.ui.resources.shared_home_balance
import com.mileway.core.ui.resources.shared_home_benefits
import com.mileway.core.ui.resources.shared_home_cd_dismiss_welcome
import com.mileway.core.ui.resources.shared_home_cd_notifications
import com.mileway.core.ui.resources.shared_home_check_in
import com.mileway.core.ui.resources.shared_home_check_in_in
import com.mileway.core.ui.resources.shared_home_check_in_office
import com.mileway.core.ui.resources.shared_home_check_in_out
import com.mileway.core.ui.resources.shared_home_check_in_subtitle
import com.mileway.core.ui.resources.shared_home_greeting
import com.mileway.core.ui.resources.shared_home_greeting_subtitle
import com.mileway.core.ui.resources.shared_home_km_total
import com.mileway.core.ui.resources.shared_home_log_miles
import com.mileway.core.ui.resources.shared_home_mileage
import com.mileway.core.ui.resources.shared_home_my_cards
import com.mileway.core.ui.resources.shared_home_pay
import com.mileway.core.ui.resources.shared_home_recent
import com.mileway.core.ui.resources.shared_home_recent_activity
import com.mileway.core.ui.resources.shared_home_request
import com.mileway.core.ui.resources.shared_home_request_card
import com.mileway.core.ui.resources.shared_home_signed_in_at
import com.mileway.core.ui.resources.shared_home_start
import com.mileway.core.ui.resources.shared_home_stat_amount
import com.mileway.core.ui.resources.shared_home_stat_distance
import com.mileway.core.ui.resources.shared_home_stat_trips
import com.mileway.core.ui.resources.shared_home_take_action
import com.mileway.core.ui.resources.shared_home_track_journey
import com.mileway.core.ui.resources.shared_home_view_all
import com.mileway.core.ui.resources.shared_home_welcome
import com.mileway.core.ui.resources.shared_home_welcome_named
import com.mileway.core.ui.resources.shared_status_approved
import com.mileway.core.ui.resources.shared_status_pending
import com.mileway.core.ui.resources.shared_status_rejected
import com.mileway.core.ui.resources.shared_status_submitted
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.agent.ui.AssistantEntryMode
import com.mileway.feature.agent.ui.AssistantFabSessionState
import com.mileway.feature.agent.ui.components.ChatAgentIndicator
import com.mileway.feature.agent.ui.components.ChatIndicatorMode
import com.mileway.stub.ActionRequiredBanner
import com.mileway.stub.AtAGlanceCounts
import com.mileway.stub.MarketingCarouselItem
import com.siddharth.kmp.common.formatDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// Header
// =============================================================================

/** Height of the gradient header content below the status bar inset. */
private val HeaderContentHeight = 96.dp

/**
 * How far the body sheet's rounded top overlaps the header art. The header paints this much
 * extra gradient below its content; HomeScreenContent offsets the sheet up by the same amount.
 */
internal val SheetOverlap = 16.dp

/** Diameter of the leading avatar circle in the header. */
private val AvatarSize = 44.dp

/**
 * Primary-gradient home header that draws behind the status bar.
 *
 * Owns the status-bar inset itself (`statusBarsPadding` inside the gradient box) so the
 * colour bleeds to the top edge while the content stays clear of the system clock. A faint
 * Canvas-drawn world-map dot texture (with a location pin) plus scanline and sheen overlays give
 * the header its terminal-map depth — all vector, so it also renders in Roborazzi screenshots.
 *
 * Layout: avatar circle, an amber auto-sizing "Hello, <name>" greeting with a
 * "Let's manage your spends!" subtitle, then a trailing search icon and a notification bell
 * carrying a red [CountBadge].
 *
 * @param name greeting name (already reduced to a first name by the ViewModel).
 * @param notificationCount unread count rendered in the bell badge.
 */
@Composable
fun HomeProfileHeader(
    name: String,
    notificationCount: Int,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onOpenAgent: (() -> Unit)? = null,
    currentPin: LocationPin? = DemoHomeLocationPin,
    // V29 P29.H.6: local profile-photo path (Profile tab's avatar picker). `null` keeps the
    // terminal `>_` glyph — the fallback state, never a broken-image placeholder.
    avatarPath: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(DesignTokens.topBarGradientBrush()),
    ) {
        // Terminal world-map backdrop — pure Canvas dots from an embedded land mask (no raster
        // asset), so it renders on device *and* in host-side Roborazzi screenshots (which can't
        // rasterize PNGs), and pins the user's current location. Tinted onPrimary (not white) so
        // the watermark stays a single low-contrast hue against the primary gradient — the same
        // treatment that keeps the fixed-contrast header text legible on ANY theme's primary.
        // statusBarsPadding: the map (and crucially its TAPPABLE location pin + callout) must
        // never render inside the status-bar inset — taps there land on the system bar, and the
        // clock/icons would collide with the art. The gradient alone paints the inset strip.
        CurrentLocationPinMap(
            modifier = Modifier.matchParentSize().statusBarsPadding(),
            pin = currentPin,
            dotColor = MaterialTheme.colorScheme.onPrimary,
            dotAlpha = 0.24f,
            pinColor = MaterialTheme.colorScheme.error,
        )
        // Terminal scanline overlay + sheen: matchParentSize, NOT fillMaxSize — under the pinned
        // header's finite constraints a fillMaxSize child would balloon the header to full screen
        // (it was harmless only while the header lived inside a scroll column's infinite height).
        ScanlineOverlay(modifier = Modifier.matchParentSize(), lineAlpha = 0.04f)
        // Subtle diagonal sheen
        Canvas(modifier = Modifier.matchParentSize().statusBarsPadding()) {
            val sheenBrush =
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.55f, size.height),
                )
            drawRect(brush = sheenBrush)
        }

        // Header content is FIXED-CONTRAST against the primary gradient: everything below uses
        // onPrimary (never primary — primary-on-primary made the greeting unreadable on themes
        // whose top-bar gradient is built from a saturated primary, e.g. the amber map header).
        val headerContent = MaterialTheme.colorScheme.onPrimary
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(HeaderContentHeight)
                    .padding(horizontal = DesignTokens.Spacing.l)
                    // Extra painted band below the content so the body sheet's rounded top
                    // corners overlap header art instead of a hard edge (see HomeScreenContent).
                    .padding(bottom = SheetOverlap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            // Terminal avatar — thin phosphor border; a picked profile photo fills it, otherwise
            // the `>_` glyph is the fallback state (never a broken-image placeholder).
            Box(
                modifier =
                    Modifier
                        .size(AvatarSize)
                        .border(
                            width = 1.dp,
                            color = headerContent,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            headerContent.copy(alpha = 0.12f),
                            RoundedCornerShape(6.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarPath != null) {
                    coil3.compose.AsyncImage(
                        model = avatarPath,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = ">_",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = headerContent,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                AutoSizeGreeting(
                    greeting = stringResource(Res.string.shared_home_greeting),
                    name = name,
                    color = headerContent,
                )
                Text(
                    text = stringResource(Res.string.shared_home_greeting_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = headerContent.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val fabMode by AssistantFabSessionState.mode.collectAsStateWithLifecycle()
            if (fabMode == AssistantEntryMode.TOPBAR && onOpenAgent != null) {
                ChatAgentIndicator(
                    mode = ChatIndicatorMode.FULL,
                    onClick = onOpenAgent,
                )
            }

            HeaderIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(Res.string.core_cd_search),
                onClick = onSearch,
            )

            // Notification bell with red count badge anchored at the top-end.
            Box {
                HeaderIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = stringResource(Res.string.shared_home_cd_notifications),
                    onClick = onNotifications,
                )
                if (notificationCount > 0) {
                    CountBadge(
                        count = notificationCount,
                        modifier = Modifier.align(Alignment.TopEnd),
                        backgroundColor = DesignTokens.StatusColors.error,
                        textColor = Color.White,
                    )
                }
            }
        }
    }
}

/** Round translucent icon button used in the gradient header (onPrimary = fixed contrast). */
@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier =
                Modifier
                    .padding(9.dp)
                    .size(DesignTokens.IconSize.navigation),
        )
    }
}

/** Asset-free dot grid used as a faint "world map" texture behind the header. */
@Composable
private fun WorldMapDotGrid(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White.copy(alpha = 0.1f),
) {
    Canvas(modifier = modifier) {
        val step = 18.dp.toPx()
        val radius = 1.4.dp.toPx()
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

// =============================================================================
// Action Required card
// =============================================================================

/**
 * White card with a red top accent surfacing items that need the user's attention.
 *
 * Shows "₹X in N transactions awaiting voucher submission" composed from the
 * [ActionRequiredBanner], a tonal "Take Action" button, and a single-page [DotsIndicator]
 * footer hinting that more alerts could exist.
 */
@Composable
fun ActionRequiredCard(
    banner: ActionRequiredBanner,
    onTakeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.Elevation.card,
        shadowElevation = DesignTokens.Elevation.card,
    ) {
        Column {
            // Red top accent strip.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(DesignTokens.StatusColors.error),
            )
            Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        tint = DesignTokens.StatusColors.error,
                        modifier = Modifier.size(DesignTokens.IconSize.badge),
                    )
                    Text(
                        text = stringResource(Res.string.shared_home_action_required),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(DesignTokens.Spacing.s))

                Text(
                    text =
                        "${banner.amountText} in ${banner.count} transactions " +
                            "awaiting voucher submission",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(DesignTokens.Spacing.m))

                Button(
                    onClick = onTakeAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedSm,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text(
                        text = stringResource(Res.string.shared_home_take_action),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// =============================================================================
// PLAN_V22 P7.1 — one-time welcome banner
// =============================================================================

/**
 * The one-shot "Welcome" banner shown exactly once after a fresh sign-in — gated by
 * [FirstLoginBannerUiState.isVisible] (from [com.mileway.core.data.session.SessionState
 * .isFirstLoginPending]). Mirrors [ActionRequiredCard]'s card idiom with a success-green accent
 * instead of the error-red one, plus a dismiss affordance like [AnimatedBannerStrip]'s dismissible
 * banners — own design, not a port of any reference app chrome.
 */
@Composable
fun WelcomeBanner(
    displayName: String?,
    officeName: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.Elevation.card,
        shadowElevation = DesignTokens.Elevation.card,
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MilewayColors.success),
            )
            Row(
                modifier = Modifier.padding(DesignTokens.Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MilewayColors.success,
                    modifier = Modifier.size(DesignTokens.IconSize.badge),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            if (displayName.isNullOrBlank()) {
                                stringResource(Res.string.shared_home_welcome)
                            } else {
                                stringResource(Res.string.shared_home_welcome_named, displayName)
                            },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!officeName.isNullOrBlank()) {
                        Spacer(Modifier.height(DesignTokens.Spacing.xs))
                        Text(
                            text = stringResource(Res.string.shared_home_signed_in_at, officeName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(DesignTokens.IconSize.minTouchTarget)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.shared_home_cd_dismiss_welcome),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Quick Actions
// =============================================================================

/** A single quick-action: label + the icon shown inside its tonal tile. */
data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/** Four evenly distributed tonal icon tiles. */
@Composable
fun QuickActionsRow(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        actions.forEach { action ->
            QuickActionTile(
                action = action,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickAction,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Surface(
            onClick = action.onClick,
            shape = DesignTokens.Shape.actionTile,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.size(DesignTokens.ActionTileSize.defaultContainer),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTile),
                )
            }
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(DesignTokens.ActionTileSize.defaultWidth),
        )
    }
}

/**
 * Builds the four canonical quick actions. V29 P29.H.2: "Add Expense" and "Add Invoice" are real
 * entry points, and "Ask Advance" is config-aware — [onAskAdvance] is pre-resolved by the caller
 * (QR advance flow vs the classic form, per [com.mileway.core.ui.home.HomePluginConfig
 * .useQrForAdvance]) so this function stays free of plugin-config concerns. "Create Voucher"
 * remains illustrative — no local voucher-creation surface exists yet.
 */
fun quickActions(
    onAddExpense: () -> Unit,
    onAskAdvance: () -> Unit,
    onAddInvoice: () -> Unit,
    onIllustrative: () -> Unit,
    // L10N follow-up: called from `remember`-free composable context (see HomeScreen), so the
    // caller resolves stringResource() and passes the labels in; English literals are the default
    // so callers/tests that don't care about localisation (e.g. HomeQuickActionsTest) still work.
    addExpenseLabel: String = "Add Expense",
    createVoucherLabel: String = "Create Voucher",
    askAdvanceLabel: String = "Ask Advance",
    addInvoiceLabel: String = "Add Invoice",
): List<QuickAction> =
    listOf(
        QuickAction(addExpenseLabel, Icons.AutoMirrored.Filled.NoteAdd, onAddExpense),
        QuickAction(createVoucherLabel, Icons.Filled.CreditCard, onIllustrative),
        QuickAction(askAdvanceLabel, Icons.Filled.RequestQuote, onAskAdvance),
        QuickAction(addInvoiceLabel, Icons.Filled.AttachFile, onAddInvoice),
    )

// =============================================================================
// Mileage carousel
// =============================================================================

/**
 * One page of the home feature carousel. [primary] cards render the dark "Start ›" pill
 * and a stronger tint; non-primary cards are illustrative.
 */
data class FeatureCarouselCard(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val icon: ImageVector,
    val primary: Boolean,
    val onAction: () -> Unit,
)

/** Builds the four feature carousel cards. Card 1 (Mileage) is the only primary one. */
fun featureCarouselCards(
    onStartTracking: () -> Unit,
    onIllustrative: () -> Unit,
    // L10N follow-up: called inside `remember { }` in FeatureCarousel, so it can't call
    // stringResource() itself — the caller resolves the strings in composable scope first and
    // passes them in. English literals default so non-UI/test call sites keep working unchanged.
    mileageTitle: String = "Mileage",
    mileageSubtitle: String = "Get reimbursed for fuel by your miles travelled",
    navTitle: String = "Customer Navigation",
    navSubtitle: String = "Navigate to your next customer location",
    reporteesTitle: String = "Track Reportees",
    reporteesSubtitle: String = "View team analytics and field activity",
    checkinTitle: String = "Center Check-In",
    checkinSubtitle: String = "Check in at a center near you",
): List<FeatureCarouselCard> =
    listOf(
        FeatureCarouselCard(
            title = mileageTitle,
            subtitle = mileageSubtitle,
            accent = DesignTokens.StatusColors.success,
            icon = Icons.Filled.DirectionsCar,
            primary = true,
            onAction = onStartTracking,
        ),
        FeatureCarouselCard(
            title = navTitle,
            subtitle = navSubtitle,
            accent = DesignTokens.StatusColors.info,
            icon = Icons.Filled.Navigation,
            primary = false,
            onAction = onIllustrative,
        ),
        FeatureCarouselCard(
            title = reporteesTitle,
            subtitle = reporteesSubtitle,
            accent = DesignTokens.StatusColors.success,
            icon = Icons.Filled.Group,
            primary = false,
            onAction = onIllustrative,
        ),
        FeatureCarouselCard(
            title = checkinTitle,
            subtitle = checkinSubtitle,
            accent = DesignTokens.StatusColors.warning,
            icon = Icons.Filled.Storefront,
            primary = false,
            onAction = onIllustrative,
        ),
    )

/**
 * Fixed height for the feature carousel pages. Must fit the full stack — 40dp badge + title +
 * 2-line subtitle + the "Start ›" pill + 16dp paddings — 150dp clipped the pill on device.
 */
val FeatureCarouselCardHeight = 192.dp

/**
 * A single feature carousel page: tinted illustrative panel, title + subtitle, and either a
 * dark "Start ›" pill (primary) or a forward chevron affordance (illustrative).
 */
@Composable
fun FeatureCarouselCardView(
    card: FeatureCarouselCard,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(FeatureCarouselCardHeight),
        shape = DesignTokens.Shape.carouselCard,
        color = card.accent.copy(alpha = 0.14f),
    ) {
        Box {
            // Large translucent icon "art" anchored to the right edge.
            Icon(
                imageVector = card.icon,
                contentDescription = null,
                tint = card.accent.copy(alpha = 0.22f),
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = DesignTokens.Spacing.s)
                        .size(112.dp),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    // Accent badge icon.
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(card.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(DesignTokens.IconSize.header),
                        )
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.7f),
                    )
                }

                if (card.primary) {
                    StartPill(onClick = card.onAction)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = card.title,
                        tint = card.accent,
                        modifier =
                            Modifier
                                .align(Alignment.End)
                                .size(DesignTokens.IconSize.header),
                    )
                }
            }
        }
    }
}

/** Dark rounded "Start ›" call-to-action pill used on the primary mileage card. */
@Composable
private fun StartPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.l,
                    vertical = DesignTokens.Spacing.s,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.shared_home_start),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(DesignTokens.IconSize.inline),
            )
        }
    }
}

// =============================================================================
// At A Glance
// =============================================================================

/** One "At A Glance" row: a big count, a title, a subtitle and a trailing chevron. */
data class AtAGlanceRow(
    val count: Int,
    val title: String,
    val subtitle: String,
)

/** Maps the [AtAGlanceCounts] into the three rows the section renders, in order. */
fun atAGlanceRows(counts: AtAGlanceCounts): List<AtAGlanceRow> =
    listOf(
        AtAGlanceRow(
            count = counts.unreportedTransactions,
            title = "Unreported Transactions",
            subtitle = "expenses & vouchers this month",
        ),
        AtAGlanceRow(
            count = counts.upcomingTrips,
            title = "Upcoming Trips",
            subtitle = "scheduled in the coming weeks",
        ),
        AtAGlanceRow(
            count = counts.pendingInvoices,
            title = "Pending Invoices",
            subtitle = "awaiting processing",
        ),
    )

/** A single tappable "At A Glance" row. */
@Composable
fun AtAGlanceRowView(
    row: AtAGlanceRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(DesignTokens.Shape.roundedSm)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${row.count} ${row.title}, ${row.subtitle}"
                }
                .fillMaxWidth()
                .padding(vertical = DesignTokens.Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Text(
            text = row.count.toString(),
            style = MaterialTheme.typography.headlineSmall.dataStyle(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize.navigation),
        )
    }
}

// =============================================================================
// Marketing strip
// =============================================================================

/** Width of one marketing/benefits card in the horizontal strip. */
val MarketingCardWidth = 260.dp

/** A single static marketing/benefits card. */
@Composable
fun MarketingCardView(
    item: MarketingCarouselItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width(MarketingCardWidth)
                .height(132.dp),
        shape = DesignTokens.Shape.carouselCard,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Surface(
                shape = DesignTokens.Shape.chip,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            ) {
                Text(
                    text = item.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.padding(
                            horizontal = DesignTokens.Spacing.s,
                            vertical = 2.dp,
                        ),
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * PLAN_V24 P13.2: the ONE home banner carousel — supersedes the P5.4 marketing strip. Auto-advances
 * every [BANNER_CAROUSEL_ADVANCE_MS], logging an id + dwell impression per shown card to the local
 * analytics stub (via [onImpression]); a tap on a deep-linkable card routes through [onBannerClick]
 * (the host maps the [HomeBanner.deepLink] to its existing nav lambdas). Empty [items] renders the
 * header + an empty row exactly as the old strip did, so the home golden stays byte-identical.
 */
@Composable
fun BannerCarousel(
    items: List<HomeBanner>,
    onBannerClick: (HomeBanner) -> Unit,
    onImpression: (HomeBanner, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Auto-advance (same LaunchedEffect + delay loop as AnimatedBannerStrip, safe under the golden test):
    // the first tick only fires after a delay, so nothing scrolls / no impression logs at capture time.
    LaunchedEffect(items) {
        if (items.isEmpty()) return@LaunchedEffect
        var index = 0
        while (true) {
            delay(BANNER_CAROUSEL_ADVANCE_MS)
            if (items.isEmpty()) break
            onImpression(items[index % items.size], BANNER_CAROUSEL_ADVANCE_MS)
            index = (index + 1) % items.size
            listState.animateScrollToItem(index)
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        Box(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal)) {
            HomeSectionHeader(title = stringResource(Res.string.shared_home_benefits), leadingIcon = Icons.Filled.CardGiftcard)
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.carouselSpacing),
        ) {
            items(items, key = { it.id }) { banner ->
                MarketingCardView(
                    item = MarketingCarouselItem(title = banner.title, subtitle = banner.subtitle, badge = banner.style),
                    modifier = if (banner.deepLink != null) Modifier.clickable { onBannerClick(banner) } else Modifier,
                )
            }
        }
    }
}

private const val BANNER_CAROUSEL_ADVANCE_MS = 4000L

/** Shared section heading used between the home sections. Terminal `//` prefix style. */
@Composable
fun HomeSectionHeader(
    title: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Text(
            text = "//",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// =============================================================================
// Phase O, Animated Banner Strip
// =============================================================================

private data class BannerSpec(
    val icon: ImageVector,
    val text: String,
    val color: Color,
    val dismissible: Boolean = false,
)

@Composable
fun AnimatedBannerStrip(
    isTrackingActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val successTone = MilewayColors.success
    val warningTone = MilewayColors.warning
    val infoTone = MilewayColors.info
    val banners =
        remember(isTrackingActive, successTone, warningTone, infoTone) {
            buildList {
                if (isTrackingActive) add(BannerSpec(Icons.Filled.RadioButtonChecked, "Tracking active · 4.2 km · 00:18:32", successTone))
                add(BannerSpec(Icons.Filled.NotificationImportant, "3 items need your attention", warningTone))
                add(BannerSpec(Icons.Filled.Info, "Submit your Oct expenses before 31st Nov.", infoTone, dismissible = true))
            }
        }
    var currentIndex by remember { mutableIntStateOf(0) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(banners.size) {
        while (true) {
            delay(4000L)
            currentIndex = (currentIndex + 1) % banners.size
        }
    }

    if (!dismissed && banners.isNotEmpty()) {
        val banner = banners[currentIndex % banners.size]
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = modifier.fillMaxWidth(),
        ) {
            Surface(
                color = banner.color.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Icon(
                        imageVector = banner.icon,
                        contentDescription = null,
                        tint = banner.color,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = banner.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = banner.color,
                        modifier = Modifier.weight(1f),
                    )
                    if (banner.dismissible) {
                        IconButton(
                            onClick = { dismissed = true },
                            modifier = Modifier.size(DesignTokens.IconSize.minTouchTarget),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.action_dismiss),
                                tint = banner.color,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Phase O, At A Glance 2×2 grid
// =============================================================================

/**
 * One "At A Glance" cell. [destinationLabel] names where a tap goes (surfaced in the cell and
 * the content description) so the affordance is explicit; [onClick] is the per-cell destination.
 */
private data class GlanceCell(
    val count: Int,
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val destinationLabel: String,
    val onClick: () -> Unit,
)

/**
 * "At A Glance" 2×2 grid (Bug 5).
 *
 * Each cell now maps to one consistent mock count *and* a distinct destination with a clear
 * tap affordance (icon header + trailing chevron + "View …" hint), instead of every cell
 * routing through a single generic callback. Cell colours come from the semantic tokens so
 * they read as status, not decoration.
 *
 * @param onPendingExpenses opens the expenses/spends surface.
 * @param onUpcomingTrips opens the travel surface.
 * @param onPendingApprovals opens the approvals surface.
 * @param onNotifications opens the notification centre.
 */
@Composable
fun AtAGlanceGrid(
    counts: AtAGlanceCounts,
    onPendingExpenses: () -> Unit,
    onUpcomingTrips: () -> Unit,
    onPendingApprovals: () -> Unit,
    onNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells =
        listOf(
            GlanceCell(counts.pendingExpenses, "Pending Expenses", MilewayColors.warning, Icons.Filled.ReceiptLong, "expenses", onPendingExpenses),
            GlanceCell(counts.upcomingTrips, "Upcoming Trips", MilewayColors.info, Icons.Filled.Flight, "trips", onUpcomingTrips),
            GlanceCell(counts.pendingApprovals, "Pending Approvals", MilewayColors.danger, Icons.Filled.FactCheck, "approvals", onPendingApprovals),
            GlanceCell(counts.unreadNotifications, "Unread Notifications", MilewayColors.premium, Icons.Filled.Notifications, "notifications", onNotifications),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
            GlanceCell(cells[0], Modifier.weight(1f))
            GlanceCell(cells[1], Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
            GlanceCell(cells[2], Modifier.weight(1f))
            GlanceCell(cells[3], Modifier.weight(1f))
        }
    }
}

/**
 * V29 P29.H.3: manager-only "who's waiting on me" summary — pending/approved/rejected counts,
 * shown only when [HomeUiState.isManager] gates it on. Tapping opens the Approvals surface.
 */
@Composable
fun ApprovalBreakdownCard(
    breakdown: ApprovalBreakdown,
    onOpenApprovals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpenApprovals,
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.Elevation.card,
        shadowElevation = DesignTokens.Elevation.card,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Icon(
                    imageVector = Icons.Filled.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.badge),
                )
                Text(
                    text = "Team Approvals",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BreakdownStat(breakdown.pending, "Pending", MilewayColors.warning)
                BreakdownStat(breakdown.approved, "Approved", MilewayColors.success)
                BreakdownStat(breakdown.rejected, "Rejected", MilewayColors.danger)
            }
        }
    }
}

@Composable
private fun BreakdownStat(
    count: Int,
    label: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge.dataStyle(), fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GlanceCell(
    cell: GlanceCell,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = cell.onClick,
        modifier =
            modifier.semantics {
                contentDescription = "${cell.count} ${cell.label}. Tap to view ${cell.destinationLabel}."
            },
        shape = DesignTokens.Shape.roundedMd,
        color = cell.color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, cell.color.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Icon(cell.icon, contentDescription = null, tint = cell.color, modifier = Modifier.size(DesignTokens.IconSize.badge))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
            }
            // VII.3: Animate count from 0 → final value on first composition. Mono for tabular digits.
            val animatedCount by animateIntAsState(
                targetValue = cell.count,
                animationSpec = tween(durationMillis = 900),
                label = "glanceCount",
            )
            Text(animatedCount.toString(), style = MaterialTheme.typography.headlineSmall.dataStyle(), fontWeight = FontWeight.Bold, color = cell.color)
            Text(cell.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

// =============================================================================
// Phase O, My Cards carousel
// =============================================================================

private data class MockCard(val label: String, val balance: String, val last4: String, val gradient: Brush)

private val MOCK_CARDS =
    listOf(
        MockCard("PETTY CASH", "₹2,400", "QR1", Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF00ACC1)))),
        MockCard("CORPORATE", "₹48,000", "4821", Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF283593)))),
    )

@Composable
fun MyCardsSection(
    onSnackbar: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeSectionHeader(stringResource(Res.string.shared_home_my_cards), Icons.Filled.CreditCard)
            TextButton(onClick = { scope.launch { onSnackbar() } }) {
                Text(stringResource(Res.string.shared_home_request_card), style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
        ) {
            items(MOCK_CARDS) { card ->
                MockCardView(card = card, onAction = { scope.launch { onSnackbar() } })
            }
        }
    }
}

@Composable
private fun MockCardView(
    card: MockCard,
    onAction: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(260.dp)
                .height(140.dp)
                .clip(DesignTokens.Shape.roundedLg)
                .background(card.gradient)
                .padding(DesignTokens.Spacing.l),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(card.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("**** ${card.last4}", style = MaterialTheme.typography.bodySmall.dataStyle(), color = Color.White.copy(alpha = 0.8f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(stringResource(Res.string.shared_home_balance), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(card.balance, style = MaterialTheme.typography.titleMedium.dataStyle(), fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(onClick = onAction, shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                        Text(
                            stringResource(Res.string.shared_home_pay),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Surface(
                        onClick = onAction,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            stringResource(Res.string.shared_home_request),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Phase O, Recent Activity feed
// =============================================================================

private enum class ActivityStatus { APPROVED, PENDING, SUBMITTED, REJECTED }

private data class ActivityItem(val id: String, val title: String, val amount: String, val status: ActivityStatus, val date: String)

private val RECENT_ACTIVITIES =
    listOf(
        ActivityItem("EXP-001", "Business dinner", "₹3,200", ActivityStatus.APPROVED, "Today"),
        ActivityItem("EXP-002", "Office supplies", "₹680", ActivityStatus.PENDING, "Today"),
        ActivityItem("MI-001", "Client visit · 48 km", "₹576", ActivityStatus.SUBMITTED, "Yesterday"),
        ActivityItem("ADV-001", "Field advance", "₹5,000", ActivityStatus.APPROVED, "Yesterday"),
        ActivityItem("INV-001", "Vendor invoice", "₹12,000", ActivityStatus.PENDING, "2 days ago"),
    )

@Composable
fun RecentActivitySection(
    onSnackbar: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeSectionHeader(stringResource(Res.string.shared_home_recent_activity), Icons.Filled.CheckCircle)
            TextButton(onClick = { scope.launch { onSnackbar() } }) {
                Text(stringResource(Res.string.shared_home_view_all), style = MaterialTheme.typography.labelMedium)
            }
        }
        RECENT_ACTIVITIES.forEachIndexed { index, item ->
            ActivityRow(item = item, onClick = { scope.launch { onSnackbar() } })
            if (index < RECENT_ACTIVITIES.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun ActivityRow(
    item: ActivityItem,
    onClick: () -> Unit,
) {
    val statusColor =
        when (item.status) {
            ActivityStatus.APPROVED -> MilewayColors.success
            ActivityStatus.PENDING -> MilewayColors.warning
            ActivityStatus.SUBMITTED -> MilewayColors.info
            ActivityStatus.REJECTED -> MilewayColors.danger
        }
    val statusLabel =
        when (item.status) {
            ActivityStatus.APPROVED -> stringResource(Res.string.shared_status_approved)
            ActivityStatus.PENDING -> stringResource(Res.string.shared_status_pending)
            ActivityStatus.SUBMITTED -> stringResource(Res.string.shared_status_submitted)
            ActivityStatus.REJECTED -> stringResource(Res.string.shared_status_rejected)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${item.title}, ${item.id}, ${item.date}, ${item.amount}, $statusLabel"
                }
                .padding(vertical = DesignTokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("${item.id} · ${item.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                item.amount,
                style = MaterialTheme.typography.bodyMedium.dataStyle(),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(shape = DesignTokens.Shape.chip, color = statusColor.copy(alpha = 0.12f)) {
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.TextButton(onClick = onClick) { content() }
}

// =============================================================================
// Phase VII, HomeMileageCard (ref 12)
// =============================================================================

private const val WEEK_KM_GOAL = 300.0
private const val WEEK_KM_DEMO = 248.0
private const val WEEK_TRIPS_DEMO = 12
private const val WEEK_AMOUNT_DEMO = 14820.0

/**
 * VII.1: Dedicated mileage card with gradient header, Canvas progress ring (120dp), stat row,
 * and "Track Journey" (primary) / "Log Miles" (outlined) quick-action buttons.
 */
@Composable
fun HomeMileageCard(
    onTrackJourney: () -> Unit,
    onLogMiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column {
            // Gradient header
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(DesignTokens.topBarGradientBrush())
                        .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.shared_home_mileage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(Res.string.shared_home_km_total, WEEK_KM_DEMO.formatDecimal(0)),
                        style = MaterialTheme.typography.bodyLarge.dataStyle(),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                // Canvas progress ring (120dp) + centre text. Merge into one TalkBack node so the
                // ring is announced as a single "X of Y km" progress reading, not loose digits.
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .align(Alignment.CenterHorizontally)
                            .semantics(mergeDescendants = true) {
                                contentDescription =
                                    "Mileage progress: ${WEEK_KM_DEMO.toInt()} of ${WEEK_KM_GOAL.toInt()} kilometres this week"
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                        val offset = stroke.width / 2
                        drawArc(
                            color = surfaceVariant,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(offset, offset),
                            size = arcSize,
                            style = stroke,
                        )
                        val progress = (WEEK_KM_DEMO / WEEK_KM_GOAL).coerceIn(0.0, 1.0).toFloat()
                        drawArc(
                            color = primaryColor,
                            startAngle = 135f,
                            sweepAngle = 270f * progress,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(offset, offset),
                            size = arcSize,
                            style = stroke,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${WEEK_KM_DEMO.formatDecimal(0)}",
                            style = MaterialTheme.typography.titleLarge.dataStyle(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "/ ${WEEK_KM_GOAL.formatDecimal(0)} km",
                            style = MaterialTheme.typography.labelSmall.dataStyle(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 3-stat row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MileageStat(label = stringResource(Res.string.shared_home_stat_trips), value = WEEK_TRIPS_DEMO.toString(), modifier = Modifier.weight(1f))
                    MileageStat(
                        label = stringResource(Res.string.shared_home_stat_distance),
                        value = "${WEEK_KM_DEMO.formatDecimal(0)} km",
                        modifier = Modifier.weight(1f),
                    )
                    MileageStat(
                        label = stringResource(Res.string.shared_home_stat_amount),
                        value = "₹${WEEK_AMOUNT_DEMO.formatDecimal(0)}",
                        modifier = Modifier.weight(1f),
                    )
                }

                // Quick-action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Button(
                        onClick = onTrackJourney,
                        modifier = Modifier.weight(1f),
                        shape = DesignTokens.Shape.roundedSm,
                    ) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
                        Spacer(Modifier.width(DesignTokens.Spacing.xs))
                        Text(stringResource(Res.string.shared_home_track_journey), style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onLogMiles,
                        modifier = Modifier.weight(1f),
                        shape = DesignTokens.Shape.roundedSm,
                    ) {
                        Text(stringResource(Res.string.shared_home_log_miles), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MileageStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.dataStyle(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// =============================================================================
// Phase VII, HomeCheckInCard (ref 11)
// =============================================================================

private data class DemoCheckIn(val location: String, val time: String, val isIn: Boolean)

private val DEMO_CHECK_INS =
    listOf(
        DemoCheckIn("Head Office, Baner", "09:12 AM · Today", isIn = true),
        DemoCheckIn("Client Site, Koregaon Park", "02:35 PM · Yesterday", isIn = false),
        DemoCheckIn("Head Office, Baner", "09:05 AM · Yesterday", isIn = true),
    )

/**
 * VII.2: Check-In card with location pin header, count badge, "Check In at Office" primary
 * button, and a compact recent-check-ins list (last 3 entries).
 */
@Composable
fun HomeCheckInCard(
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            // Header: location pin + title + count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Navigation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            stringResource(Res.string.shared_home_check_in),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(Res.string.shared_home_check_in_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CountBadge(count = DEMO_CHECK_INS.size, modifier = Modifier)
            }

            // "Check In at Office" primary button
            Button(
                onClick = onCheckIn,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
                Spacer(Modifier.width(DesignTokens.Spacing.xs))
                Text(stringResource(Res.string.shared_home_check_in_office), style = MaterialTheme.typography.labelMedium)
            }

            // Recent check-ins list
            Text(
                stringResource(Res.string.shared_home_recent),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DEMO_CHECK_INS.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (entry.isIn) MilewayColors.success else MilewayColors.warning),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.location,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(entry.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (entry.isIn) stringResource(Res.string.shared_home_check_in_in) else stringResource(Res.string.shared_home_check_in_out),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isIn) MilewayColors.success else MilewayColors.warning,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Home Header")
@Composable
private fun PreviewHomeProfileHeader() {
    PreviewSurface {
        HomeProfileHeader(
            name = "Priya Sharma",
            notificationCount = 3,
            onSearch = {},
            onNotifications = {},
        )
    }
}

@Preview(name = "Section Header")
@Composable
private fun PreviewHomeSectionHeader() {
    PreviewSurface {
        HomeSectionHeader(
            title = "Recent Trips",
            leadingIcon = androidx.compose.material.icons.Icons.Default.DirectionsCar,
        )
    }
}
