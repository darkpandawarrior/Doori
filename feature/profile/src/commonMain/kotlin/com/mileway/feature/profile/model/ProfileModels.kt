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

/**
 * P1.4: initials derivation shared by the active-account badge ([DemoAccount.displayName]).
 * commonMain (not androidMain) since it's pure string logic with no platform dependency — kept
 * separate from `ProfileScreen.kt`'s androidMain-only `initialsOf`/`FakeProfileRepository`'s
 * `initialsFrom` (both file-private, so not directly reusable here) rather than widening either.
 */
object AccountBadge {
    fun initialsFor(name: String): String =
        name
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }
}

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
 * PLAN_V22 P6.5: Preferences' Notification Center channel gates — Mileway's local/offline
 * equivalent of the reference app's WhatsApp/Slack connect-disconnect switches on the same screen.
 * Backed by [com.mileway.core.data.settings.DemoSettingsRepository] (DataStore), so these coexist
 * with [PreferenceToggles] rather than replacing it.
 */
data class NotificationChannels(
    val pushEnabled: Boolean = true,
    val whatsappEnabled: Boolean = false,
    val slackEnabled: Boolean = false,
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
        /**
         * One normalised point per day of the demo week, hoisted out of [demo] so the seven values
         * are a named series rather than seven unexplained floats in an argument list.
         */
        private val DemoWeekSparkline = listOf(0.30f, 0.42f, 0.38f, 0.55f, 0.48f, 0.70f, 0.62f)

        /** Deterministic demo snapshot mirroring the reference "last 7 days" widget. */
        fun demo(): AccountAnalyticsSnapshot =
            AccountAnalyticsSnapshot(
                totalSpend = "₹22,629.12",
                transactions = 1,
                window = "last 7 days",
                updatedAt = "02:54 AM",
                sparkline = DemoWeekSparkline,
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
    /** P6.5: Preferences' Notification Center channel gates, live-synced from DataStore. */
    val notificationChannels: NotificationChannels = NotificationChannels(),
    val preferenceMessage: String? = null,
    /** P1.3: the persona currently shown in [AccountDetailsSheet][com.mileway.feature.profile.ui.screens.AccountDetailsSheet], or null when dismissed. */
    val accountDetailsSheet: DemoAccount? = null,
    /**
     * P2.3: the persona a switch was requested for but not yet confirmed by
     * [SwitchAccountPinSheet][com.mileway.feature.profile.ui.screens.SwitchAccountPinSheet] — non-null
     * shows the PIN sheet; null (the default) means no switch is pending. Only used on the local-PIN
     * path; the biometric path never sets this (see `ProfileEffect.RequestBiometricGate`).
     */
    val pendingSwitchAccountId: String? = null,
    /**
     * P3.4: the persona just switched *to*, when [com.mileway.core.data.session.MockAccountSessionCoordinator]
     * (via `ProfileViewModel.CommitAccountSwitch`) found and paused a trip the outgoing persona had
     * running — non-null shows the "Trip in progress — pause and switch?" notice sheet; null (the
     * default, and the state after dismissal) means no notice is pending. A switch with no active
     * trip never sets this, so the notice is silent in the common case.
     */
    val pausedTripNotice: String? = null,
    /**
     * P3.2: true when [com.mileway.core.data.session.isSessionFresh] reports the session as stale
     * and [com.mileway.feature.profile.ui.screens.ReconfirmIdentitySheet] should show. Dismissing
     * it resets the staleness window rather than requiring a real credential (see `ProfileViewModel`).
     */
    val showReconfirmIdentity: Boolean = false,
)

data class SettingsUiState(
    val darkThemeOverride: Boolean?,
    val useMiles: Boolean = true,
    val appVersion: String = "1.0.0 (demo)",
)

// ── P6.1: profile completion engine ────────────────────────────────────────────

/**
 * A destination a [MissingFieldSpec] can navigate to. One variant per screen/section that
 * actually owns a field today. New owning screens/sections (e.g. P6.2's Vehicle/Passport tiles)
 * should add their own variant here rather than overloading [ProfileDetails].
 */
sealed interface ProfileRoute {
    /**
     * The field lives on [com.mileway.feature.profile.ui.screens.ProfileDetailsScreen] itself —
     * this is every field's current home, since Personal Info/Organization/Policy &
     * Compliance/Travel/Apps & Activity/Location & Assets are all rendered as tiles on that one
     * screen. [fieldId] matches [DetailEntry][com.mileway.feature.profile.ui.screens.ProfileDetailsScreen]'s
     * `ProfileGridItem.id] so the screen can scroll to and highlight the exact tile.
     */
    data class ProfileDetails(
        val fieldId: String,
    ) : ProfileRoute
}

/**
 * A single missing (blank) profile field with its own stable identity, human label, a priority
 * (lower = shown first in the checklist; required fields sort ahead of optional ones) and a real
 * navigation target — replaces the old category-level `done/total` pair, which could not say
 * *which* field was missing or send a tap anywhere.
 */
data class MissingFieldSpec(
    val fieldId: String,
    val label: String,
    val priority: Int,
    val route: ProfileRoute,
)

/**
 * Per-field profile completion, derived straight from an [EmployeeProfile] snapshot's actual
 * blank/non-blank state rather than a static, hand-maintained count.
 */
data class ProfileFieldCompletion(
    val percent: Int,
    val completedCount: Int,
    val totalCount: Int,
    val missingFields: List<MissingFieldSpec>,
) {
    companion object {
        /** Ordered lower-priority-first; required fields (see [isRequiredField]) sort ahead of optional ones. */
        private data class FieldDef(
            val fieldId: String,
            val label: String,
            val required: Boolean,
            val value: (EmployeeProfile) -> String,
        )

        private val FIELD_DEFS =
            listOf(
                FieldDef("d_name", "Full Name", required = true) { it.name },
                FieldDef("d_gender", "Gender", required = false) { it.gender },
                FieldDef("d_home", "Home Location", required = false) { it.homeLocation },
                FieldDef("d_org", "Organization", required = true) { it.organization },
                FieldDef("d_manager", "Reporting Manager", required = true) { it.manager?.name.orEmpty() },
                FieldDef("d_role", "Role", required = true) { it.role },
                FieldDef("d_phone", "Phone", required = false) { it.phone },
                FieldDef("d_code", "Employee Code", required = false) { it.employeeCode },
            )

        /**
         * Derives completion straight from [profile]'s actual field presence: a field counts as
         * done iff its string value is non-blank. Missing fields are sorted required-first, then
         * by declaration order, and each carries a real [ProfileRoute] to navigate to.
         */
        fun derive(profile: EmployeeProfile): ProfileFieldCompletion {
            val missing =
                FIELD_DEFS
                    .withIndex()
                    .filter { (_, def) -> def.value(profile).isBlank() }
                    .sortedWith(compareBy({ !it.value.required }, { it.index }))
                    .mapIndexed { priority, (_, def) ->
                        MissingFieldSpec(
                            fieldId = def.fieldId,
                            label = def.label,
                            priority = priority,
                            route = ProfileRoute.ProfileDetails(def.fieldId),
                        )
                    }
            val total = FIELD_DEFS.size
            val done = total - missing.size
            return ProfileFieldCompletion(
                percent = if (total > 0) done * 100 / total else 0,
                completedCount = done,
                totalCount = total,
                missingFields = missing,
            )
        }
    }
}
