package com.mileway.feature.profile.viewmodel

/**
 * PLAN_V24 P6.3: an incentive program (source: the reference app `fetch_incentive_data` → `ProgramModel`,
 * grouped active vs expired by `programType`, each with target / progress / reward). String content
 * is carried as resource keys ([titleKey]/[descKey]/[rewardKey]) resolved in the screen, so the model
 * stays i18n-clean and the [IncentiveCatalog.build] logic stays pure/testable.
 */
data class IncentiveProgram(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val rewardKey: String,
    val target: Int,
    val progress: Int,
    val expired: Boolean,
)

/**
 * PLAN_V24 P6.3: the (mostly seeded) incentive catalogue. The one live program — "complete N tracked
 * trips" — has its progress fed by the real completed-trip count (from `SavedTrackDao`); the rest are
 * seeded. Pure so it unit-tests without a DAO.
 */
object IncentiveCatalog {
    const val WEEKLY_TRIPS_TARGET = 3

    fun build(completedTrips: Int): List<IncentiveProgram> =
        listOf(
            IncentiveProgram(
                id = "weekly_trips",
                titleKey = "incentive_weekly_trips_title",
                descKey = "incentive_weekly_trips_desc",
                rewardKey = "incentive_weekly_trips_reward",
                target = WEEKLY_TRIPS_TARGET,
                progress = completedTrips.coerceIn(0, WEEKLY_TRIPS_TARGET),
                expired = false,
            ),
            IncentiveProgram(
                id = "log_expenses",
                titleKey = "incentive_log_expenses_title",
                descKey = "incentive_log_expenses_desc",
                rewardKey = "incentive_log_expenses_reward",
                target = 5,
                progress = 2,
                expired = false,
            ),
            IncentiveProgram(
                id = "monsoon_saver",
                titleKey = "incentive_monsoon_title",
                descKey = "incentive_monsoon_desc",
                rewardKey = "incentive_monsoon_reward",
                target = 10,
                progress = 10,
                expired = true,
            ),
        )
}
