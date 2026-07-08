package com.mileway.core.data.campaign

/**
 * PLAN_V24 P5.4: seed set for the campaign-marketing hub — 5 campaigns across live/upcoming/ended.
 * In core:data so [CampaignRepository.seedIfEmpty] can use it (the repository is shared by the
 * profile hub and the HomeScreen marketing strip).
 */
internal object CampaignMockData {
    val campaigns: List<Campaign> =
        listOf(
            Campaign(
                id = "CMP-A",
                name = "Refer & Earn Fest",
                description = "Invite colleagues this month and unlock bonus travel credits for every successful referral.",
                badge = "New",
                status = CampaignStatus.LIVE,
                mobileExclusive = true,
                contactEmail = "growth@mileway.com",
                interestCaptured = false,
            ),
            Campaign(
                id = "CMP-B",
                name = "Fuel Savings Week",
                description = "Log fuel expenses this week for a chance to win reimbursement boosters.",
                badge = "Live",
                status = CampaignStatus.LIVE,
                mobileExclusive = false,
                contactEmail = "offers@mileway.com",
                interestCaptured = false,
            ),
            Campaign(
                id = "CMP-C",
                name = "Wellness Commute",
                description = "A new cycle-to-work programme is coming soon — register interest to be notified first.",
                badge = "Soon",
                status = CampaignStatus.UPCOMING,
                mobileExclusive = true,
                contactEmail = "wellness@mileway.com",
                interestCaptured = false,
            ),
            Campaign(
                id = "CMP-D",
                name = "Quarterly Rewards Draw",
                description = "Top expense-loggers each quarter enter a rewards draw. Next draw opens next month.",
                badge = "Soon",
                status = CampaignStatus.UPCOMING,
                mobileExclusive = false,
                contactEmail = "rewards@mileway.com",
                interestCaptured = false,
            ),
            Campaign(
                id = "CMP-E",
                name = "Summer Travel Bonus",
                description = "The summer travel bonus has ended — thanks to everyone who took part.",
                badge = "Ended",
                status = CampaignStatus.ENDED,
                mobileExclusive = false,
                contactEmail = "travel@mileway.com",
                interestCaptured = true,
            ),
        )
}
