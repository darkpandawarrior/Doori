package com.mileway.core.ui.review

import com.siddharth.kmp.appshell.ReviewState
import com.siddharth.kmp.appshell.ReviewStateStore
import platform.Foundation.NSUserDefaults

/**
 * PLAN_V24 P12.3: NSUserDefaults-backed [ReviewStateStore] so the iOS review-gate counters survive
 * cold start (the demo previously used the in-memory default). Same three keys as the Android store.
 */
class IosReviewStateStore : ReviewStateStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun load(): ReviewState =
        ReviewState(
            firstOpenMillis = defaults.integerForKey("first_open_time"),
            interactionCount = defaults.integerForKey("review_interaction_count").toInt(),
            lastPromptMillis = defaults.integerForKey("review_last_prompt_time"),
        )

    override suspend fun save(state: ReviewState) {
        defaults.setInteger(state.firstOpenMillis, forKey = "first_open_time")
        defaults.setInteger(state.interactionCount.toLong(), forKey = "review_interaction_count")
        defaults.setInteger(state.lastPromptMillis, forKey = "review_last_prompt_time")
    }
}
