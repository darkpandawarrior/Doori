package com.mileway.feature.profile.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.mileway.core.network.model.DemoAccount
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.network.model.ProfileCompletion
import com.mileway.core.network.model.UserSession

/** Header shown at the top of the profile screen. */
data class ProfileHeader(
    val name: String,
    val email: String,
    val code: String,
    val tenant: String,
    val initials: String,
)

/** A single tappable settings entry rendered as a card on the profile screen. */
data class SettingsTile(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * On/off preference toggles owned by the ViewModel and surfaced on the Preferences screen.
 * These are local-only demo flags (no server, no network).
 */
data class PreferenceToggles(
    val pushNotifications: Boolean = true,
    val usageAnalytics: Boolean = true,
)

/**
 * Lightweight analytics snapshot shown in the "Analytics / Live" card on the Account hub.
 * [sparkline] is a list of normalised points (0f..1f) plotted as a tiny trend line.
 */
data class AccountAnalyticsSnapshot(
    val totalSpend: String,
    val transactions: Int,
    val window: String,
    val updatedAt: String,
    val sparkline: List<Float>,
) {
    companion object {
        /** Deterministic demo snapshot mirroring the reference "last 7 days" widget. */
        fun demo(): AccountAnalyticsSnapshot =
            AccountAnalyticsSnapshot(
                totalSpend = "₹22,629.12",
                transactions = 1,
                window = "last 7 days",
                updatedAt = "02:54 AM",
                sparkline = listOf(0.30f, 0.42f, 0.38f, 0.55f, 0.48f, 0.70f, 0.62f),
            )
    }
}

/**
 * Single immutable state for the whole account suite.
 * Profile/completion/sessions/accounts come straight from the offline repository; the
 * remaining fields are transient UI flags driven by ViewModel intents.
 */
data class ProfileUiState(
    val header: ProfileHeader,
    val profile: EmployeeProfile,
    val completion: ProfileCompletion,
    val sessions: List<UserSession>,
    val accounts: List<DemoAccount>,
    val selectedAccountId: String = "",
    val analytics: AccountAnalyticsSnapshot,
    val preferences: PreferenceToggles = PreferenceToggles(),
    val showSessionsDialog: Boolean = false,
    val preferenceMessage: String? = null,
)

data class SettingsUiState(
    val darkThemeOverride: Boolean?,
    val useMiles: Boolean = true,
    val appVersion: String = "1.0.0 (demo)",
)
