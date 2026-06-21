package com.miletracker.stub

import com.miletracker.core.network.model.CompletionCategory
import com.miletracker.core.network.model.DemoAccount
import com.miletracker.core.network.model.EmployeeProfile
import com.miletracker.core.network.model.ProfileCompletion
import com.miletracker.core.network.model.UserSession

/**
 * Static rich-profile data for the offline demo: the primary employee profile,
 * a profile-completion checklist, signed-in device sessions and switchable accounts.
 *
 * All values are deterministic constants. The identity (name / code / email) matches
 * the profile inside [DemoMockData.userConfig] so every surface shows the same user.
 */
object ProfileMockData {
    /** The signed-in demo employee. Identity fields mirror [DemoMockData.userConfig]. */
    fun primaryProfile(): EmployeeProfile =
        EmployeeProfile(
            name = "Demo User",
            email = "demo@mileway.app",
            employeeCode = "EMP001",
            phone = "+91 98765 43210",
            gender = "Male",
            role = "Field Sales Executive",
            organization = "Demo Logistics Pvt Ltd",
            manager = "Asha Verma",
            homeLocation = "Baner, Pune",
        )

    /**
     * Profile completion checklist. The percent is derived from the category counts
     * (8 done out of 11 total -> 72%), so the headline number always matches the rows.
     */
    fun completion(): ProfileCompletion {
        val categories =
            listOf(
                CompletionCategory(name = "Personal Info", done = 2, total = 2),
                CompletionCategory(name = "Location & Assets", done = 0, total = 1),
                CompletionCategory(name = "Organization", done = 1, total = 1),
                CompletionCategory(name = "Policy & Compliance", done = 1, total = 1),
                CompletionCategory(name = "Travel", done = 1, total = 3),
                CompletionCategory(name = "Apps & Activity", done = 3, total = 3),
            )
        val done = categories.sumOf { it.done }
        val total = categories.sumOf { it.total }
        return ProfileCompletion(
            percent = if (total > 0) done * 100 / total else 0,
            categories = categories,
        )
    }

    /**
     * Devices this account is signed in on. Timestamps are fixed demo epochs
     * (mid-2026); the current session carries the most recent one.
     */
    fun sessions(): List<UserSession> =
        listOf(
            UserSession(
                deviceName = "Pixel 8 Pro",
                platform = "Android 15",
                lastActiveMillis = 1_780_000_000_000L,
                isCurrent = true,
            ),
            UserSession(
                deviceName = "Chrome on Windows",
                platform = "Web",
                lastActiveMillis = 1_779_400_000_000L,
                isCurrent = false,
            ),
            UserSession(
                deviceName = "iPad Air",
                platform = "iPadOS 18",
                lastActiveMillis = 1_778_100_000_000L,
                isCurrent = false,
            ),
        )

    /** Accounts the user can switch between from the profile screen. */
    fun accounts(): List<DemoAccount> =
        listOf(
            DemoAccount(
                id = "ACC-001",
                displayName = "Demo User",
                employeeCode = "EMP001",
                organization = "Demo Logistics Pvt Ltd",
            ),
            DemoAccount(
                id = "ACC-002",
                displayName = "Demo User (Sandbox)",
                employeeCode = "EMP001-SBX",
                organization = "Demo Sandbox Workspace",
            ),
            DemoAccount(
                id = "ACC-003",
                displayName = "QA Tester",
                employeeCode = "QA042",
                organization = "Demo QA Workspace",
            ),
        )
}
