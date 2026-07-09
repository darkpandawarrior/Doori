package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.network.model.CompletionCategory
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.ui.components.CategoryCompletionDisplay
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.GridProfileTile
import com.mileway.core.ui.components.MissingItemDisplay
import com.mileway.core.ui.components.ProfileCompletionBanner
import com.mileway.core.ui.components.ProfileGridItem
import com.mileway.core.ui.components.ProfileItemStatus
import com.mileway.core.ui.components.ProfileSectionHeader
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.resources.profile_details_back
import com.mileway.core.ui.resources.profile_details_change_phone
import com.mileway.core.ui.resources.profile_details_contact_info
import com.mileway.core.ui.resources.profile_details_custom_fields
import com.mileway.core.ui.resources.profile_details_field_code
import com.mileway.core.ui.resources.profile_details_field_gender
import com.mileway.core.ui.resources.profile_details_field_home
import com.mileway.core.ui.resources.profile_details_field_manager
import com.mileway.core.ui.resources.profile_details_field_name
import com.mileway.core.ui.resources.profile_details_field_organization
import com.mileway.core.ui.resources.profile_details_field_passport
import com.mileway.core.ui.resources.profile_details_field_phone
import com.mileway.core.ui.resources.profile_details_field_role
import com.mileway.core.ui.resources.profile_details_field_vehicle
import com.mileway.core.ui.resources.profile_details_not_set
import com.mileway.core.ui.resources.profile_details_section_apps
import com.mileway.core.ui.resources.profile_details_section_location
import com.mileway.core.ui.resources.profile_details_section_organization
import com.mileway.core.ui.resources.profile_details_section_personal
import com.mileway.core.ui.resources.profile_details_section_policy
import com.mileway.core.ui.resources.profile_details_section_travel
import com.mileway.core.ui.resources.profile_details_subtitle
import com.mileway.core.ui.resources.profile_details_title
import com.mileway.core.ui.resources.profile_email_link_sent
import com.mileway.core.ui.resources.profile_email_simulate_click
import com.mileway.core.ui.resources.profile_email_unverified
import com.mileway.core.ui.resources.profile_email_verified
import com.mileway.core.ui.resources.profile_email_verify
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.model.PassportDetails
import com.mileway.feature.profile.model.PayoutDetails
import com.mileway.feature.profile.model.ProfileFieldCompletion
import com.mileway.feature.profile.model.ProfileRoute
import com.mileway.feature.profile.model.VehicleDetails
import com.mileway.feature.profile.viewmodel.PersonalDetailsViewModel
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Sheets [ProfileDetailsScreen] can push over itself — at most one at a time. */
private enum class ProfileDetailSheet { VEHICLE, PASSPORT, PHONE_CHANGE, CORPORATE }

/**
 * Profile Details, a full-detail editor surface pushed from the Account hub.
 *
 * Layout (top to bottom):
 *  - LEVEL_2 [com.mileway.core.ui.components.topbar.DepthAwareTopBar] with back
 *  - avatar + name + role/gender chips
 *  - "Contact Info" [CollapsibleSectionCard] (email / code / phone rows)
 *  - [ProfileCompletionBanner] fed by the repository's completion checklist
 *  - per-category [GridProfileTile] sections grouped by [ProfileSectionHeader]
 *
 * Full-screen flow: there is no bubble bar, so the pinned bottom of the grid uses
 * navigation-bars padding via the content insets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    onBack: () -> Unit,
    onOpenOrgChart: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    personalDetailsViewModel: PersonalDetailsViewModel = koinViewModel(),
    pluginRegistry: com.mileway.core.data.plugin.PluginRegistry = org.koin.compose.koinInject(),
    sessionRepository: com.mileway.core.data.session.SessionRepository = org.koin.compose.koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val personalDetailsState by personalDetailsViewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    // PLAN_V24 P3.1: phone shown from the session (changed value) falling back to the profile.
    val phoneChangeEnabled by pluginRegistry.observe("phoneChangeEnabled").collectAsStateWithLifecycle(initialValue = true)
    val emailVerificationEnabled by pluginRegistry.observe("emailVerificationEnabled").collectAsStateWithLifecycle(initialValue = true)
    val corporateVerificationEnabled by pluginRegistry.observe("corporateVerificationEnabled").collectAsStateWithLifecycle(initialValue = true)
    // PLAN_V24 P8.2: payout identity (bank + editable UPI + QR) — off by default, driver-ish persona.
    val payoutDetailsEnabled by pluginRegistry.observe("payoutDetailsEnabled").collectAsStateWithLifecycle(initialValue = false)
    val session by sessionRepository.sessionState.collectAsStateWithLifecycle(initialValue = com.mileway.core.data.session.SessionState())
    val completion = state.completion
    var activeSheet by remember { mutableStateOf<ProfileDetailSheet?>(null) }

    // P6.1: real per-field completion, derived from actual blank/non-blank field presence
    // (`ProfileFieldCompletion.derive`) rather than the static category-level `done/total` pair.
    val fieldCompletion = remember(profile) { ProfileFieldCompletion.derive(profile) }
    val missingFieldIds = remember(fieldCompletion) { fieldCompletion.missingFields.map { it.fieldId }.toSet() }
    val missingItems =
        fieldCompletion.missingFields.map {
            MissingItemDisplay(
                id = it.fieldId,
                title = it.label,
                isRequired = it.fieldId in REQUIRED_FIELD_IDS,
            )
        }
    val categoryDisplays = completion.categories.map { it.toDisplay() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // P6.1: tapping a missing-field banner item (or a tile itself) scrolls to and highlights the
    // tile that owns it — every field today lives on this same screen (ProfileRoute.ProfileDetails),
    // so "navigating" to the field means bringing its tile into view rather than pushing a new
    // destination.
    var highlightedFieldId by remember { mutableStateOf<String?>(null) }

    val fieldOrder = remember { DETAIL_FIELD_ORDER }

    // P6.2: the Custom Fields card only renders (as its own full-width row) when the profile
    // actually carries custom fields, so the header-row offset scrollToField uses must account
    // for that conditionally-present row.
    val headerItemCount = HEADER_ITEM_COUNT + if (profile.customFields.isNotEmpty()) 1 else 0

    fun scrollToField(fieldId: String) {
        val targetIndex = fieldOrder.indexOf(fieldId)
        if (targetIndex >= 0) {
            highlightedFieldId = fieldId
            // Header + contact card + completion banner rows precede the first tile; offset the
            // flat index so the scroll target lands on-screen.
            scope.launch { gridState.animateScrollToItem((targetIndex + headerItemCount).coerceAtLeast(0)) }
        }
    }

    val detailItems =
        buildDetailItems(
            profile = profile,
            vehicle = personalDetailsState.vehicle,
            passport = personalDetailsState.passport,
            missingFieldIds = missingFieldIds,
            onTileClick = ::scrollToField,
            onOpenOrgChart = onOpenOrgChart,
            onOpenVehicleSheet = { activeSheet = ProfileDetailSheet.VEHICLE },
            onOpenPassportSheet = { activeSheet = ProfileDetailSheet.PASSPORT },
        )
    val grouped = detailItems.groupBy { it.category }

    fun onMissingFieldClick(route: ProfileRoute) {
        when (route) {
            is ProfileRoute.ProfileDetails -> scrollToField(route.fieldId)
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_details_title),
                subtitle = stringResource(Res.string.profile_details_subtitle),
                depth = NavigationDepth.LEVEL_2,
                titleIcon = Icons.Filled.Person,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_details_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            item(span = { GridItemSpan(2) }) {
                ProfileIdentityBlock(profile = profile)
            }

            item(span = { GridItemSpan(2) }) {
                CollapsibleSectionCard(
                    title = stringResource(Res.string.profile_details_contact_info),
                    initiallyExpanded = true,
                    leadingIcon = Icons.Default.Person,
                ) {
                    EmailVerificationRow(
                        email = profile.email,
                        enabled = emailVerificationEnabled,
                    )
                    ContactRow(icon = Icons.Default.Badge, value = profile.employeeCode)
                    ContactRow(icon = Icons.Default.Phone, value = session.phone.ifBlank { profile.phone })
                    if (phoneChangeEnabled) {
                        androidx.compose.material3.TextButton(onClick = { activeSheet = ProfileDetailSheet.PHONE_CHANGE }) {
                            androidx.compose.material3.Text(stringResource(Res.string.profile_details_change_phone))
                        }
                    }
                    // PLAN_V24 P4.4: corporate-email verification row (badge when verified, else CTA).
                    if (corporateVerificationEnabled) {
                        if (session.isCorporateVerified) {
                            androidx.compose.material3.AssistChip(
                                onClick = {},
                                enabled = false,
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                label = { androidx.compose.material3.Text(cvRes("corporate_verified_badge", "Corporate verified")) },
                            )
                        } else {
                            androidx.compose.material3.TextButton(onClick = { activeSheet = ProfileDetailSheet.CORPORATE }) {
                                androidx.compose.material3.Text(cvRes("corporate_verify_cta", "Verify corporate email"))
                            }
                        }
                    }
                }
            }

            // PLAN_V24 P8.2: payout identity — seeded read-only bank + editable UPI handle + QR.
            if (payoutDetailsEnabled) {
                item(span = { GridItemSpan(2) }) {
                    PayoutDetailsCard(
                        upiHandle = session.upiHandle,
                        onSaveUpi = { handle -> scope.launch { sessionRepository.setUpiHandle(handle) } },
                    )
                }
            }

            // P6.2: custom fields render on the Personal Info tile group as their own card,
            // only when the profile actually carries any (tenant-defined key/value pairs).
            if (profile.customFields.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    CollapsibleSectionCard(
                        title = stringResource(Res.string.profile_details_custom_fields),
                        initiallyExpanded = true,
                        leadingIcon = Icons.Default.Apps,
                    ) {
                        profile.customFields.forEach { (label, value) -> ContactRow(icon = Icons.Default.Badge, value = "$label: $value") }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                ProfileCompletionBanner(
                    completionPercentage = fieldCompletion.percent,
                    completedCount = fieldCompletion.completedCount,
                    totalCount = fieldCompletion.totalCount,
                    missingItems = missingItems,
                    categories = categoryDisplays,
                    onMissingItemClick = { fieldId ->
                        fieldCompletion.missingFields.find { it.fieldId == fieldId }?.let { onMissingFieldClick(it.route) }
                    },
                )
            }

            grouped.forEach { (category, entries) ->
                item(span = { GridItemSpan(2) }) {
                    ProfileSectionHeader(
                        title = category,
                        itemCount = entries.size,
                        icon = entries.first().leadingIcon,
                    )
                }
                items(entries, key = { it.item.id }) { detail ->
                    GridProfileTile(item = detail.item.copy(animatePulse = detail.item.id == highlightedFieldId))
                }
            }

            // Keep the last row above the system navigation bar (no bubble bar here).
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    when (activeSheet) {
        ProfileDetailSheet.VEHICLE ->
            VehicleDetailsSheet(
                initial = personalDetailsState.vehicle,
                onSave = personalDetailsViewModel::saveVehicle,
                onDismiss = { activeSheet = null },
            )
        ProfileDetailSheet.PASSPORT ->
            PassportDetailsSheet(
                initial = personalDetailsState.passport,
                onSave = personalDetailsViewModel::savePassport,
                onDismiss = { activeSheet = null },
            )
        ProfileDetailSheet.PHONE_CHANGE ->
            PhoneChangeSheet(onDismiss = { activeSheet = null })
        ProfileDetailSheet.CORPORATE ->
            CorporateVerificationSheet(onDismiss = { activeSheet = null })
        null -> Unit
    }
}

/** Dynamic string resolver for P4.4 corporate keys (avoids per-key generated imports). */
@Composable
private fun cvRes(
    key: String,
    fallback: String,
): String {
    val resource = Res.allStringResources[key] ?: return fallback
    return stringResource(resource)
}

/** Number of full-width rows rendered before the first per-field tile (identity, contact, banner). */
private const val HEADER_ITEM_COUNT = 3

/** Flat tile order [buildDetailItems] renders in — kept in sync with its own `entry(...)` calls so [scrollToField] can index into it. */
private val DETAIL_FIELD_ORDER =
    listOf("d_name", "d_gender", "d_home", "d_org", "d_manager", "d_role", "d_phone", "d_code", "d_vehicle", "d_passport")

/** Required categories drive the "X required items remaining" copy in the category pills. */
private val REQUIRED_CATEGORIES = setOf("Personal Info", "Organization", "Policy & Compliance")

/** P6.1: required *fields* (a finer grain than [REQUIRED_CATEGORIES]) drive the checklist's Required/Optional split. */
private val REQUIRED_FIELD_IDS = setOf("d_name", "d_org", "d_manager", "d_role")

private fun CompletionCategory.toDisplay(): CategoryCompletionDisplay =
    CategoryCompletionDisplay(
        categoryLabel = name,
        completedCount = done,
        totalCount = total,
        percentage = if (total > 0) done * 100 / total else 0,
        isRequiredCategory = name in REQUIRED_CATEGORIES,
    )

@Composable
private fun ProfileIdentityBlock(profile: EmployeeProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(DesignTokens.Shape.button)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(profile.name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            if (profile.role.isNotBlank()) DetailChip(profile.role)
            if (profile.gender.isNotBlank()) DetailChip(profile.gender)
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
        )
    }
}

/**
 * PLAN_V24 P3.2: the email row with a Verified/Unverified chip. When unverified and enabled, a
 * "Verify" action sends a demo link whose "Simulate clicking the link" affordance flips the status.
 */
@Composable
private fun EmailVerificationRow(
    email: String,
    enabled: Boolean,
    viewModel: com.mileway.feature.profile.viewmodel.EmailVerificationViewModel = org.koin.compose.viewmodel.koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize.navigation),
            )
            Text(
                text = email.ifBlank { stringResource(Res.string.profile_details_not_set) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            val chipColor = if (state.isVerified) DesignTokens.StatusColors.success else DesignTokens.StatusColors.warning
            Surface(color = chipColor.copy(alpha = 0.12f), shape = DesignTokens.Shape.chip) {
                Text(
                    text =
                        if (state.isVerified) {
                            stringResource(Res.string.profile_email_verified)
                        } else {
                            stringResource(Res.string.profile_email_unverified)
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = chipColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        if (enabled && !state.isVerified && email.isNotBlank()) {
            if (state.linkSent) {
                Text(
                    text = stringResource(Res.string.profile_email_link_sent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.TextButton(onClick = viewModel::confirmClicked) {
                    Text(stringResource(Res.string.profile_email_simulate_click))
                }
            } else {
                androidx.compose.material3.TextButton(onClick = viewModel::sendLink) {
                    Text(stringResource(Res.string.profile_email_verify))
                }
            }
        }
    }
}

/**
 * PLAN_V24 P8.2: payout identity block — a seeded, display-only bank account (masked) plus an
 * editable UPI handle (validated `\w+@\w+`) rendered as an illustrative QR. Bank fields are mock
 * constants; only the UPI handle persists (session). Restyled with Mileway's own card idiom — not
 * a port of the reference app's payout screen.
 */
@Composable
private fun PayoutDetailsCard(
    upiHandle: String?,
    onSaveUpi: (String) -> Unit,
) {
    var upiText by remember(upiHandle) { mutableStateOf(upiHandle.orEmpty()) }
    val trimmed = upiText.trim()
    val isValid = PayoutDetails.isValidUpiHandle(trimmed)
    val showError = trimmed.isNotEmpty() && !isValid

    CollapsibleSectionCard(
        title = cvRes("payout_section_title", "Payout details"),
        initiallyExpanded = true,
        leadingIcon = Icons.Default.AccountBalance,
    ) {
        ContactRow(icon = Icons.Default.AccountBalance, value = PayoutDetails.maskedAccount)
        Text(
            text = "${PayoutDetails.BANK_NAME} · IFSC ${PayoutDetails.IFSC}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(DesignTokens.Spacing.m))

        OutlinedTextField(
            value = upiText,
            onValueChange = { upiText = it },
            label = { Text(cvRes("payout_upi_label", "UPI handle")) },
            placeholder = { Text(cvRes("payout_upi_hint", "name@bank")) },
            singleLine = true,
            isError = showError,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        if (showError) {
            Text(
                cvRes("payout_upi_invalid", "Enter a valid UPI handle (name@bank)."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { onSaveUpi(trimmed) },
            enabled = isValid,
            shape = DesignTokens.Shape.button,
        ) {
            Text(cvRes("payout_upi_save", "Save"))
        }

        // Illustrative UPI QR (derived from the handle) — shown once a valid handle is saved.
        if (!upiHandle.isNullOrBlank() && PayoutDetails.isValidUpiHandle(upiHandle)) {
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                UpiQrCanvas(data = PayoutDetails.upiPayString(upiHandle))
                Column {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        cvRes("payout_upi_qr_caption", "Scan to pay"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(upiHandle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * A compact, illustrative QR whose module pattern is derived deterministically from [data] (so a
 * different UPI handle draws a different code). Reuses `QrHomeScreen`'s Canvas idiom — ponytail:
 * this is a demo QR, not a spec-compliant encoder (upgrade path: a real QR library if scanning is
 * ever needed).
 */
@Composable
private fun UpiQrCanvas(
    data: String,
    modifier: Modifier = Modifier,
) {
    val seed = data.fold(0) { acc, c -> acc * 31 + c.code }
    val dark = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier.size(96.dp).clip(DesignTokens.Shape.roundedSm).background(Color.White).padding(6.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val n = 21
            val cell = size.width / n
            for (row in 0 until n) {
                for (col in 0 until n) {
                    if (payoutQrBit(row, col, n, seed)) {
                        drawPayoutQrModule(row, col, cell, dark)
                    }
                }
            }
        }
    }
}

private fun payoutQrBit(
    row: Int,
    col: Int,
    n: Int,
    seed: Int,
): Boolean {
    val last = n - 1
    val inFinder =
        (row in 0..6 && col in 0..6) ||
            (row in 0..6 && col in (last - 6)..last) ||
            (row in (last - 6)..last && col in 0..6)
    if (inFinder) {
        val r = if (row <= 6) row else row - (last - 6)
        val c = if (col <= 6) col else col - (last - 6)
        return r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
    }
    val h = (row * 73856093) xor (col * 19349663) xor seed
    return (h ushr 3) and 1 == 1
}

private fun DrawScope.drawPayoutQrModule(
    row: Int,
    col: Int,
    cell: Float,
    color: Color,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(col * cell + 0.5f, row * cell + 0.5f),
        size = Size(cell - 1f, cell - 1f),
        cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f),
    )
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize.navigation),
        )
        Text(
            text = value.ifBlank { stringResource(Res.string.profile_details_not_set) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

/** A detail tile plus its section icon, so the section header can reuse the same glyph. */
private data class DetailEntry(
    val category: String,
    val leadingIcon: ImageVector,
    val item: ProfileGridItem,
)

/**
 * Builds the per-category detail tiles from the [profile] plus [vehicle]/[passport] (P6.2's new
 * linked-record tiles). Values present on the profile render as COMPLETE; absent values (also
 * listed in [missingFieldIds], from [ProfileFieldCompletion.derive]) render as INCOMPLETE.
 * [onTileClick] fires on a plain field tap — used to re-highlight/re-scroll to the tapped tile
 * itself (P6.1: no tile is a no-op anymore); [onOpenOrgChart]/[onOpenVehicleSheet]/
 * [onOpenPassportSheet] override that default for the three tiles that now open a real
 * destination instead.
 */
@Composable
private fun buildDetailItems(
    profile: EmployeeProfile,
    vehicle: VehicleDetails?,
    passport: PassportDetails?,
    missingFieldIds: Set<String>,
    onTileClick: (String) -> Unit,
    onOpenOrgChart: () -> Unit,
    onOpenVehicleSheet: () -> Unit,
    onOpenPassportSheet: () -> Unit,
): List<DetailEntry> {
    val notSet = stringResource(Res.string.profile_details_not_set)

    fun entry(
        category: String,
        sectionIcon: ImageVector,
        id: String,
        title: String,
        value: String,
        tileIcon: ImageVector,
        isComplete: Boolean = id !in missingFieldIds,
        action: () -> Unit = { onTileClick(id) },
    ): DetailEntry =
        DetailEntry(
            category = category,
            leadingIcon = sectionIcon,
            item =
                ProfileGridItem(
                    id = id,
                    title = title,
                    subtitle = value.ifBlank { notSet },
                    icon = tileIcon,
                    category = category,
                    status = if (isComplete) ProfileItemStatus.COMPLETE else ProfileItemStatus.INCOMPLETE,
                    action = action,
                ),
        )

    val sectionPersonal = stringResource(Res.string.profile_details_section_personal)
    val sectionLocation = stringResource(Res.string.profile_details_section_location)
    val sectionOrganization = stringResource(Res.string.profile_details_section_organization)
    val sectionPolicy = stringResource(Res.string.profile_details_section_policy)
    val sectionTravel = stringResource(Res.string.profile_details_section_travel)
    val sectionApps = stringResource(Res.string.profile_details_section_apps)

    return listOf(
        entry(sectionPersonal, Icons.Default.Person, "d_name", stringResource(Res.string.profile_details_field_name), profile.name, Icons.Default.Person),
        entry(sectionPersonal, Icons.Default.Person, "d_gender", stringResource(Res.string.profile_details_field_gender), profile.gender, Icons.Default.Badge),
        entry(sectionLocation, Icons.Default.Home, "d_home", stringResource(Res.string.profile_details_field_home), profile.homeLocation, Icons.Default.Home),
        entry(
            sectionOrganization,
            Icons.Default.Business,
            "d_org",
            stringResource(Res.string.profile_details_field_organization),
            profile.organization,
            Icons.Default.Business,
        ),
        entry(
            category = sectionOrganization,
            sectionIcon = Icons.Default.Business,
            id = "d_manager",
            title = stringResource(Res.string.profile_details_field_manager),
            value = profile.manager?.name.orEmpty(),
            tileIcon = Icons.Default.SupervisorAccount,
            action = onOpenOrgChart,
        ),
        entry(sectionPolicy, Icons.Default.Gavel, "d_role", stringResource(Res.string.profile_details_field_role), profile.role, Icons.Default.Gavel),
        entry(sectionTravel, Icons.Default.CardTravel, "d_phone", stringResource(Res.string.profile_details_field_phone), profile.phone, Icons.Default.Phone),
        entry(sectionApps, Icons.Default.Apps, "d_code", stringResource(Res.string.profile_details_field_code), profile.employeeCode, Icons.Default.Apps),
        entry(
            category = sectionLocation,
            sectionIcon = Icons.Default.Home,
            id = "d_vehicle",
            title = stringResource(Res.string.profile_details_field_vehicle),
            value = if (vehicle?.isComplete == true) "${vehicle.make} ${vehicle.model}".trim() else "",
            tileIcon = Icons.Default.DirectionsCar,
            isComplete = vehicle?.isComplete == true,
            action = onOpenVehicleSheet,
        ),
        entry(
            category = sectionTravel,
            sectionIcon = Icons.Default.CardTravel,
            id = "d_passport",
            title = stringResource(Res.string.profile_details_field_passport),
            value = if (passport?.isComplete == true) passport.passportNumber else "",
            tileIcon = Icons.Default.Book,
            isComplete = passport?.isComplete == true,
            action = onOpenPassportSheet,
        ),
    )
}
