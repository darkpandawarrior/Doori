package com.mileway.core.data.banner

/**
 * PLAN_V24 P13.1 — the unified priority banner stack. A `sealed` hierarchy of top-of-screen status
 * banners, assembled priority-ordered by [BannerAssembler] and rendered by one `BannerHost` hoisted
 * at the composition root. Lower [priority] renders nearer the top.
 *
 * This is the reference app's banner-system shape rebuilt on Mileway's own foundation, with two
 * deliberate divergences (noted in .ralph/PROGRESS.md):
 *  - Mileway is offline, so the reference's network/auth (priority 0) and debug banners collapse —
 *    there is no connectivity state to surface, and the tracking HUD is already the home banner
 *    strip. The variants kept here are the ones with a real local source.
 *  - Dismissals persist per banner-id + account in Room (`banners_dismissed`) rather than living in
 *    a process-lifetime set, so a dismissed banner stays dismissed across app restarts.
 *
 * Mileway additions over the reference set: [DeletionRequested] (P7.1), [DocumentExpiry] (P4),
 * [SubscriptionExpiry] (P6.2).
 */
sealed interface Banner {
    /** Stable identity used for dismissal persistence. */
    val id: String

    /** Ascending render/priority order — lower wins the top slot. */
    val priority: Int

    /** Whether the user may dismiss it. [UpdateReady] and [Delegate] are never dismissible. */
    val isDismissible: Boolean

    /** An app-update is staged and ready to install. Never filtered, never dismissible. */
    data object UpdateReady : Banner {
        override val id = "banner_update_ready"
        override val priority = 10
        override val isDismissible = false
    }

    /** A per-persona operator message (maintenance window, policy change, …). Dismissible. */
    data class Custom(val text: String) : Banner {
        override val id = "banner_custom"
        override val priority = 20
        override val isDismissible = true
    }

    /** "Acting as <name>" while a session delegation is active. Non-dismissible (end it instead). */
    data class Delegate(val name: String) : Banner {
        override val id = "banner_delegate"
        override val priority = 30
        override val isDismissible = false
    }

    /** An account-deletion request is in flight (REQUESTED / PROCESSING). Dismissible. */
    data object DeletionRequested : Banner {
        override val id = "banner_deletion_requested"
        override val priority = 40
        override val isDismissible = true
    }

    /** [count] verification documents still need attention. Dismissible. */
    data class DocumentExpiry(val count: Int) : Banner {
        override val id = "banner_document_expiry"
        override val priority = 50
        override val isDismissible = true
    }

    /** The active subscription renews/expires in [daysLeft] days. Dismissible. */
    data class SubscriptionExpiry(val daysLeft: Int) : Banner {
        override val id = "banner_subscription_expiry"
        override val priority = 60
        override val isDismissible = true
    }
}

/**
 * The pure assembly step: drop dismissible banners the account has dismissed, then order the rest by
 * [Banner.priority]. Non-dismissible banners ([Banner.UpdateReady], [Banner.Delegate]) always
 * survive the filter. Kept side-effect-free so it is exhaustively unit-tested without Room/Compose.
 */
object BannerAssembler {
    fun assemble(
        banners: List<Banner>,
        dismissedIds: Set<String>,
    ): List<Banner> =
        banners
            .filterNot { it.isDismissible && it.id in dismissedIds }
            .sortedBy { it.priority }
}
