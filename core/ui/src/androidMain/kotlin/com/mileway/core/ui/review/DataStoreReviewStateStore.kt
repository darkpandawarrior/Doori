package com.mileway.core.ui.review

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mileway.core.platform.ReviewState
import com.mileway.core.platform.ReviewStateStore
import kotlinx.coroutines.flow.firstOrNull

private val Context.reviewDataStore by preferencesDataStore(name = "review_state")

/**
 * PLAN_V24 P12.3: DataStore-backed [ReviewStateStore] so the review-gate counters survive cold start
 * (the demo previously used the in-memory default, which reset every launch). Keys mirror the plan's
 * sim-friendly store names: `first_open_time`, `review_interaction_count`, `review_last_prompt_time`.
 */
class DataStoreReviewStateStore(private val context: Context) : ReviewStateStore {
    private val firstOpenKey = longPreferencesKey("first_open_time")
    private val interactionKey = intPreferencesKey("review_interaction_count")
    private val lastPromptKey = longPreferencesKey("review_last_prompt_time")

    override suspend fun load(): ReviewState {
        val prefs = context.reviewDataStore.data.firstOrNull() ?: return ReviewState()
        return ReviewState(
            firstOpenMillis = prefs[firstOpenKey] ?: 0L,
            interactionCount = prefs[interactionKey] ?: 0,
            lastPromptMillis = prefs[lastPromptKey] ?: 0L,
        )
    }

    override suspend fun save(state: ReviewState) {
        context.reviewDataStore.edit { prefs ->
            prefs[firstOpenKey] = state.firstOpenMillis
            prefs[interactionKey] = state.interactionCount
            prefs[lastPromptKey] = state.lastPromptMillis
        }
    }
}
