package com.mileway.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Rich employee profile ─────────────────────────────────────────────────────

/**
 * Full employee profile shown on the profile detail screen.
 * Superset of the lightweight header identity (name / email / code).
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
    @SerialName("manager") val manager: String = "",
    @SerialName("homeLocation") val homeLocation: String = "",
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
)

/**
 * An account the user can switch to from the profile screen.
 */
@Serializable
data class DemoAccount(
    @SerialName("id") val id: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("employeeCode") val employeeCode: String,
    @SerialName("organization") val organization: String = "",
)
