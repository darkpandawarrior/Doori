package com.miletracker.feature.profile.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.CategoryCompletionDisplay
import com.miletracker.core.ui.components.GridProfileTile
import com.miletracker.core.ui.components.MissingItemDisplay
import com.miletracker.core.ui.components.ProfileCompletionBanner
import com.miletracker.core.ui.components.ProfileGridItem
import com.miletracker.core.ui.components.ProfileItemStatus
import com.miletracker.core.ui.components.ProfileSectionHeader
import com.miletracker.core.ui.previews.PreviewLightDark
import com.miletracker.core.ui.previews.PreviewMatrix
import com.miletracker.core.ui.previews.PreviewSurface
import com.miletracker.core.ui.previews.SampleData
import com.miletracker.feature.profile.ui.screens.NotificationCentreScreen
import com.miletracker.feature.profile.ui.screens.RootGuardScreen

// ---------------------------------------------------------------------------
// ProfilePreviews.kt: Phase 9.1 preview functions for feature:profile
//
// Rules:
// - No DI, no ViewModel, no Koin
// - Uses @PreviewLightDark / @PreviewMatrix from :core:ui
// - RootGuardScreen and NotificationCentreScreen accept only plain data / lambdas
//   so they can be called directly.
// - Screens backed by a ViewModel (CardDetailScreen, AdvanceHistory, etc.) are
//   represented via their underlying shared components (ProfileCompletionBanner,
//   GridProfileTile, etc.) from :core:ui.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 1. RootGuardScreen, rooted / compromised device variant
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun RootGuardRootedPreview() {
    PreviewSurface {
        RootGuardScreen(
            onContinue = {},
            signals =
                listOf(
                    "su binary found at /system/xbin/su",
                    "test-keys build",
                    "Magisk detected",
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// 2. RootGuardScreen, clean (secure) device variant
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun RootGuardCleanPreview() {
    PreviewSurface {
        RootGuardScreen(
            onContinue = {},
            signals = emptyList(),
        )
    }
}

// ---------------------------------------------------------------------------
// 3. NotificationCentreScreen, takes only a lambda, previews directly
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun NotificationCentrePreview() {
    PreviewSurface {
        NotificationCentreScreen(onBack = {})
    }
}

// ---------------------------------------------------------------------------
// 4. ProfileCompletionBanner, in-progress state (mirrors what's shown at the
//    top of the profile hub; screen is ViewModel-backed)
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun ProfileCompletionBannerInProgressPreview() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileCompletionBanner(
                completionPercentage = 62,
                completedCount = 8,
                totalCount = 13,
                missingItems =
                    listOf(
                        MissingItemDisplay(id = "bank", title = "Bank Account", isRequired = true),
                        MissingItemDisplay(id = "pan", title = "PAN Card", isRequired = true),
                        MissingItemDisplay(id = "photo", title = "Profile Photo", isRequired = false),
                    ),
                categories =
                    listOf(
                        CategoryCompletionDisplay("Personal", 3, 4, 75, true),
                        CategoryCompletionDisplay("Finance", 2, 5, 40, true),
                        CategoryCompletionDisplay("Documents", 3, 4, 75, false),
                    ),
                expanded = false,
                onExpandToggle = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5. GridProfileTile, used inside the profile hub grid
// ---------------------------------------------------------------------------

@PreviewMatrix
@Composable
private fun GridProfileTilePreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileSectionHeader(
                title = "Account",
                itemCount = 3,
                icon = Icons.Filled.Person,
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GridProfileTile(
                    item =
                        ProfileGridItem(
                            id = "profile",
                            title = SampleData.Profile.name,
                            subtitle = SampleData.Profile.employeeId,
                            icon = Icons.Filled.Person,
                            status = ProfileItemStatus.COMPLETE,
                            action = {},
                        ),
                    modifier = Modifier.weight(1f),
                )
                GridProfileTile(
                    item =
                        ProfileGridItem(
                            id = "notifications",
                            title = "Notifications",
                            subtitle = "174 unread",
                            icon = Icons.Filled.Notifications,
                            status = ProfileItemStatus.NEEDS_ATTENTION,
                            badgeCount = 174,
                            action = {},
                        ),
                    modifier = Modifier.weight(1f),
                )
                GridProfileTile(
                    item =
                        ProfileGridItem(
                            id = "cards",
                            title = "Corporate Card",
                            subtitle = "2 active",
                            icon = Icons.Filled.CreditCard,
                            status = ProfileItemStatus.COMPLETE,
                            action = {},
                        ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
