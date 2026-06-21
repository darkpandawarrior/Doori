package com.miletracker.core.platform

/**
 * CF.1: typed reader over the raw feature-flag map (from `ConfigProvider.getFeatureFlags()`, which is
 * env/BuildConfig-overridable). Injected via Koin so UI can gate optional surfaces without touching the
 * config layer directly.
 */
class FeatureFlags(private val flags: Map<String, Boolean> = emptyMap()) {
    fun isEnabled(
        key: String,
        default: Boolean = false,
    ): Boolean = flags[key] ?: default

    val referralEnabled: Boolean get() = isEnabled(REFERRAL, true)
    val inAppReviewEnabled: Boolean get() = isEnabled(IN_APP_REVIEW, true)
    val inAppUpdateEnabled: Boolean get() = isEnabled(IN_APP_UPDATE, false)
    val shareEnabled: Boolean get() = isEnabled(SHARE, true)

    companion object {
        const val REFERRAL = "referralEnabled"
        const val IN_APP_REVIEW = "inAppReviewEnabled"
        const val IN_APP_UPDATE = "inAppUpdateEnabled"
        const val SHARE = "shareEnabled"
    }
}
