package com.mileway.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Rich employee profile ─────────────────────────────────────────────────────

/**
 * Lightweight reference to another employee — used for [EmployeeProfile.manager] so the profile
 * can link to a real org-chart node (P6.2) instead of only carrying a free-text name.
 */
@Serializable
data class EmployeeSummary(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("code") val code: String = "",
)

/**
 * Full employee profile shown on the profile detail screen.
 * Superset of the lightweight header identity (name / email / code).
 *
 * P6.2: [manager] becomes a linked [EmployeeSummary] (was a bare `String`) so "Reporting Manager"
 * can push a real org-chart node instead of only rendering a name; [customFields] is a small
 * ordered map of tenant-defined key/value pairs (mirrors the reference app's custom-field concept)
 * rendered on the Personal Info tile.
 */
@Serializable
data class EmployeeProfile(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("employeeCode") val employeeCode: String,
    @SerialName("phone") val phone: String = "",
    @SerialName("gender") val gender: String = "",
    @SerialName("role") val role: String = "",
    @SerialName("organization") val organization: String = "",
    @SerialName("manager") val manager: EmployeeSummary? = null,
    @SerialName("homeLocation") val homeLocation: String = "",
    @SerialName("customFields") val customFields: Map<String, String> = emptyMap(),
)

// ── Profile completion ────────────────────────────────────────────────────────

/**
 * One section of the profile-completion checklist, e.g. "Personal Info 2/2".
 */
@Serializable
data class CompletionCategory(
    @SerialName("name") val name: String,
    @SerialName("done") val done: Int = 0,
    @SerialName("total") val total: Int = 0,
)

/**
 * Aggregate profile completion. [percent] is the integer percentage of all
 * [CompletionCategory.done] items over all [CompletionCategory.total] items.
 */
@Serializable
data class ProfileCompletion(
    @SerialName("percent") val percent: Int = 0,
    @SerialName("categories") val categories: List<CompletionCategory> = emptyList(),
)

// ── Sessions & accounts ───────────────────────────────────────────────────────

/**
 * A device session the account is signed in on.
 */
@Serializable
data class UserSession(
    @SerialName("deviceName") val deviceName: String,
    @SerialName("platform") val platform: String,
    @SerialName("lastActiveMillis") val lastActiveMillis: Long = 0L,
    @SerialName("isCurrent") val isCurrent: Boolean = false,
    // PLAN_V24 P7.2: device-detail enrichment surfaced in the session-details sheet.
    @SerialName("os") val os: String = "",
    @SerialName("appVersion") val appVersion: String = "",
    @SerialName("ip") val ip: String = "",
)

/**
 * An account the user can switch to from the profile screen.
 *
 * P1.3: [isActive]/[lastLoginAtMs]/[createdAtMs] mirror `MockAccountEntity` (core/data) so
 * `AccountDetailsSheet` can render them without reaching past this screen-facing model. Defaulted
 * so every existing call site (including `stub.ProfileMockData`) keeps compiling unchanged.
 */
@Serializable
data class DemoAccount(
    @SerialName("id") val id: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("employeeCode") val employeeCode: String,
    @SerialName("organization") val organization: String = "",
    @SerialName("isActive") val isActive: Boolean = false,
    @SerialName("lastLoginAtMs") val lastLoginAtMs: Long = 0L,
    @SerialName("createdAtMs") val createdAtMs: Long = 0L,
    // PLAN_V24 P1.6: the persona's registered phone (E.164-ish), for duplicate-account resolution
    // on phone-OTP login. Two seeded personas deliberately share a number.
    @SerialName("phone") val phone: String = "",
)
