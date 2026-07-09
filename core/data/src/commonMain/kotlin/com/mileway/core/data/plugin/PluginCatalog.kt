package com.mileway.core.data.plugin

/**
 * PLAN_V24 P0.1 — the enumerated set of every plugin the app knows about. Each later phase
 * *registers* by appending its descriptor(s) to a category list here; the Master Plugin page
 * (P0.3) renders whatever is in [all], so a feature surfaces in the registry the moment its
 * descriptor lands. This is the single "register in the registry" mechanism — a plain aggregated
 * list, not a runtime registration API (no init-order hazard).
 *
 * P0.1 seeds only [coreModulePlugins] — Mileway's existing feature modules as TILE plugins, so
 * personas can hide whole modules (Jugnoo-verticals style). P1–P13 append their categories.
 */
object PluginCatalog {
    /**
     * Mileway's shipped feature modules, each a TILE plugin. Disabled ⇒ its home tile and nav
     * destination simply don't render (the composition root reads [PluginRegistry.observe]).
     * All default ON — the Minimal Guest persona (P0.2) is what turns most of them off.
     */
    val coreModulePlugins: List<PluginDescriptor> =
        listOf(
            coreModule("tracking"),
            coreModule("logging"),
            coreModule("expenses"),
            coreModule("travel"),
            coreModule("approvals"),
            coreModule("payables"),
            coreModule("payments"),
            coreModule("events"),
            coreModule("cards"),
            coreModule("agent"),
            coreModule("notificationCentre"),
            coreModule("referralCard"),
            coreModule("marketingStrip"),
        )

    /**
     * Auth-depth plugins (P1). Each defaults OFF — the email/password flow is the baseline; a
     * persona preset (P0.2) turns these on (e.g. Super-App Consumer → phone login).
     */
    val authPlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "phoneLoginEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.AUTH,
                titleKey = "plugin_auth_phone_login_title",
                descriptionKey = "plugin_auth_phone_login_desc",
                defaultOn = false,
            ),
            PluginDescriptor(
                id = "otpViaCallEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.AUTH,
                titleKey = "plugin_auth_otp_via_call_title",
                descriptionKey = "plugin_auth_otp_via_call_desc",
                defaultOn = false,
            ),
            PluginDescriptor(
                id = "mfaRequired",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.AUTH,
                titleKey = "plugin_auth_mfa_required_title",
                descriptionKey = "plugin_auth_mfa_required_desc",
                defaultOn = false,
            ),
            PluginDescriptor(
                id = "showPasswordSettings",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.AUTH,
                titleKey = "plugin_auth_password_settings_title",
                descriptionKey = "plugin_auth_password_settings_desc",
                defaultOn = false,
            ),
        )

    /**
     * Signup-onboarding form config (P2.1) — the reference app `SignupOnboardingFragment` gating flags.
     * `signupOnboardingEnabled` is the master gate; the rest shape the field set.
     */
    val onboardingPlugins: List<PluginDescriptor> =
        listOf(
            onboardingFlag("signupOnboardingEnabled", defaultOn = false),
            onboardingFlag("signupLastNameOptional", defaultOn = true),
            onboardingFlag("signupEmailOptional", defaultOn = true),
            onboardingFlag("genderRequired", defaultOn = false),
            onboardingFlag("dobRequired", defaultOn = false),
            onboardingFlag("showPromoOnboarding", defaultOn = false),
            onboardingFlag("showSkipOnboarding", defaultOn = true),
        )

    /** Profile-depth plugins (P3). */
    val profilePlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "phoneChangeEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PROFILE,
                titleKey = "plugin_profile_phone_change_title",
                descriptionKey = "plugin_profile_phone_change_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "emailVerificationEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PROFILE,
                titleKey = "plugin_profile_email_verify_title",
                descriptionKey = "plugin_profile_email_verify_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "savedPlacesEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PROFILE,
                titleKey = "plugin_profile_saved_places_title",
                descriptionKey = "plugin_profile_saved_places_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "emergencyContactsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PROFILE,
                titleKey = "plugin_profile_emergency_contacts_title",
                descriptionKey = "plugin_profile_emergency_contacts_desc",
                defaultOn = true,
            ),
            // PLAN_V24 P10.1: SettingsScreen's notifications toggle. NOTED SKIP for behavior (the
            // tracking FGS notification is mandatory; no offline consumer), but registry-backed so
            // it survives VM recreation instead of being an in-memory MutableStateFlow.
            PluginDescriptor(
                id = "notificationsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PROFILE,
                titleKey = "plugin_profile_notifications_title",
                descriptionKey = "plugin_profile_notifications_desc",
                defaultOn = true,
            ),
        )

    /** Tracking-depth plugins (P3.5, P10, P11). */
    val trackingPlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "driverEmergencyModeEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_tracking_emergency_mode_title",
                descriptionKey = "plugin_tracking_emergency_mode_desc",
                defaultOn = true,
            ),
            // PLAN_V24 P10.1: force raw-GPS provider instead of Fused. Consumer =
            // FusedLocationSource provider/priority selection (androidMain). defaultOn=false keeps
            // the current Fused-high-accuracy behavior.
            PluginDescriptor(
                id = "track_force_gps_only",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_tracking_force_gps_title",
                descriptionKey = "plugin_tracking_force_gps_desc",
                defaultOn = false,
            ),
            // PLAN_V24 P10.1: NOTED SKIP — no offline consumer (no backend upload path exists).
            // Persisted only so the toggle is not remember-only; parity placeholder for a future
            // backend phase. defaultOn=true matches the screen's current default.
            PluginDescriptor(
                id = "track_upload_in_background",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_tracking_upload_background_title",
                descriptionKey = "plugin_tracking_upload_background_desc",
                defaultOn = true,
            ),
            // PLAN_V24 P10.1: NOTED SKIP — no trip auto-pause logic exists (only stationary
            // detection, not auto-pause). Persisted only. defaultOn=false matches the screen.
            PluginDescriptor(
                id = "track_auto_pause_detection",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_tracking_auto_pause_title",
                descriptionKey = "plugin_tracking_auto_pause_desc",
                defaultOn = false,
            ),
            // PLAN_V24 P10.6: manager-only reportee tracking view (the reference app enableTrackMileageManagerView /
            // ExpensePluginConfig.showTrackReportees). A whole new profile-hub destination (TILE).
            // defaultOn=false keeps the profile-hub gallery golden byte-identical; the id is already
            // declared true in SuperProfilePersonas.CorporateCommuter, so registering this descriptor
            // is what turns the tile on for that (manager) persona.
            PluginDescriptor(
                id = "trackMileageManagerView",
                kind = PluginKind.TILE,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_tracking_manager_view_title",
                descriptionKey = "plugin_tracking_manager_view_desc",
                defaultOn = false,
            ),
        )

    /**
     * Tracking-tuning VALUE plugins (P10.1) — the Track Miles settings sliders, now registry-backed
     * so they persist per-account AND drive the live location engine. NOTE (P10.3 overlap):
     * `track_min_displacement_m` and `track_location_interval_s` are thin single floors that P10.3's
     * full categorized key set will supersede — P10.3 must reconcile, not double-register.
     */
    val trackingTuningPlugins: List<PluginDescriptor> =
        listOf(
            // Consumer = LocationProcessor.maxAccuracyThreshold. Default 50 preserves the shipped
            // pipeline default (LocationTrackingConstants.MAX_ACCURACY_THRESHOLD_M) — NOT the
            // screen's dead 30 — so live tracking math is unchanged when unset.
            PluginDescriptor(
                id = "track_min_accuracy_m",
                kind = PluginKind.VALUE,
                category = PluginCategory.TRACKING_TUNING,
                titleKey = "plugin_tracking_tuning_min_accuracy_title",
                descriptionKey = "plugin_tracking_tuning_min_accuracy_desc",
                valueSpec = PluginValueSpec.IntSpec(defaultValue = 50, min = 10, max = 100, step = 1, unit = "m"),
            ),
            // Consumer = DynamicIntervalCalculator via a new IntervalInputs floor. Default 10 s.
            PluginDescriptor(
                id = "track_location_interval_s",
                kind = PluginKind.VALUE,
                category = PluginCategory.TRACKING_TUNING,
                titleKey = "plugin_tracking_tuning_location_interval_title",
                descriptionKey = "plugin_tracking_tuning_location_interval_desc",
                valueSpec = PluginValueSpec.IntSpec(defaultValue = 10, min = 5, max = 60, step = 1, unit = "s"),
            ),
            // Consumer = LocationProcessor.minDisplacementForSpeed floor. Default 0 = existing math
            // untouched when unset.
            PluginDescriptor(
                id = "track_min_displacement_m",
                kind = PluginKind.VALUE,
                category = PluginCategory.TRACKING_TUNING,
                titleKey = "plugin_tracking_tuning_min_displacement_title",
                descriptionKey = "plugin_tracking_tuning_min_displacement_desc",
                valueSpec = PluginValueSpec.IntSpec(defaultValue = 0, min = 0, max = 50, step = 1, unit = "m"),
            ),
        )

    /**
     * Mileage-sync settings (P10.2) — the reference app `MileageSyncSettingsCard`. Each toggle gates a real
     * local behavior in [SyncDiagnosticsRepository][com.mileway.feature.profile.repository.SyncDiagnosticsRepository]'s
     * force-sync drain (which local staging buckets get moved to the synced counters); the interval
     * VALUE drives the displayed next-auto-sync-due time. No backend — see CLAUDE.md "The backend".
     */
    val syncSettingsPlugins: List<PluginDescriptor> =
        listOf(
            // Gates the location staging bucket in force-sync. defaultOn=true = today's behavior
            // (all buckets drain).
            PluginDescriptor(
                id = "sync_location_enabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_sync_location_title",
                descriptionKey = "plugin_sync_location_desc",
                defaultOn = true,
            ),
            // Gates the event staging bucket in force-sync. defaultOn=true.
            PluginDescriptor(
                id = "sync_events_enabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_sync_events_title",
                descriptionKey = "plugin_sync_events_desc",
                defaultOn = true,
            ),
            // Gates an extra debug-events staging bucket in force-sync. defaultOn=false (diagnostic
            // opt-in), matching the reference app where debug-event upload is off by default.
            PluginDescriptor(
                id = "sync_debug_events_enabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_sync_debug_events_title",
                descriptionKey = "plugin_sync_debug_events_desc",
                defaultOn = false,
            ),
            // NOTED SKIP for behavior — picks the backend sync protocol version; no offline consumer
            // (Mileway has no backend yet). Persisted for parity so the toggle isn't remember-only.
            // defaultOn=false (v1 baseline).
            PluginDescriptor(
                id = "sync_v2_api_enabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_sync_v2_api_title",
                descriptionKey = "plugin_sync_v2_api_desc",
                defaultOn = false,
            ),
            // Consumer = SyncDiagnosticsRepository.nextSyncDueMs (lastSync + interval), shown on the
            // card as the next auto-sync-due time. Default 15 min.
            PluginDescriptor(
                id = "sync_interval_minutes",
                kind = PluginKind.VALUE,
                category = PluginCategory.TRACKING_TUNING,
                titleKey = "plugin_sync_interval_title",
                descriptionKey = "plugin_sync_interval_desc",
                valueSpec = PluginValueSpec.IntSpec(defaultValue = 15, min = 5, max = 60, step = 5, unit = "min"),
            ),
        )

    /** Verification-centre plugins (P4). */
    val verificationPlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "verificationCentreEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.VERIFICATION,
                titleKey = "plugin_verification_centre_title",
                descriptionKey = "plugin_verification_centre_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "corporateVerificationEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.VERIFICATION,
                titleKey = "plugin_corporate_verify_title",
                descriptionKey = "plugin_corporate_verify_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "cardKycEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PAYMENTS,
                titleKey = "plugin_cards_kyc_title",
                descriptionKey = "plugin_cards_kyc_desc",
                defaultOn = true,
            ),
            // PLAN_V24 P8.1: external payment-wallet linking (Paytm/Mobikwik-style) via offline OTP.
            // Defaults OFF — a wallet-carrying persona preset (or the Master Plugin page) enables it.
            PluginDescriptor(
                id = "walletLinkingEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PAYMENTS,
                titleKey = "plugin_payments_wallet_linking_title",
                descriptionKey = "plugin_payments_wallet_linking_desc",
                defaultOn = false,
            ),
            // PLAN_V24 P8.2: payout identity (bank display + editable UPI handle + QR). Defaults OFF —
            // a driver-ish persona preset (or the Master Plugin page) enables it.
            PluginDescriptor(
                id = "payoutDetailsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.PAYMENTS,
                titleKey = "plugin_payments_payout_details_title",
                descriptionKey = "plugin_payments_payout_details_desc",
                defaultOn = false,
            ),
        )

    /** Growth plugins (P5). */
    val growthPlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "referralProgramEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.GROWTH,
                titleKey = "plugin_growth_referral_program_title",
                descriptionKey = "plugin_growth_referral_program_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "couponsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.GROWTH,
                titleKey = "plugin_growth_coupons_title",
                descriptionKey = "plugin_growth_coupons_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "scratchRewardsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.GROWTH,
                titleKey = "plugin_growth_rewards_title",
                descriptionKey = "plugin_growth_rewards_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "campaignMarketingEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.GROWTH,
                titleKey = "plugin_growth_campaigns_title",
                descriptionKey = "plugin_growth_campaigns_desc",
                defaultOn = true,
            ),
        )

    /** Membership plugins (P6). */
    val membershipPlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "clubEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.MEMBERSHIP,
                titleKey = "plugin_membership_club_title",
                descriptionKey = "plugin_membership_club_desc",
                defaultOn = true,
            ),
            PluginDescriptor(
                id = "subscriptionsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.MEMBERSHIP,
                titleKey = "plugin_membership_subscriptions_title",
                descriptionKey = "plugin_membership_subscriptions_desc",
                defaultOn = true,
            ),
            // PLAN_V24 P7.3: manager-only "Act on behalf" (session delegation). Defaults OFF — a
            // privileged capability a manager persona preset (or the Master Plugin page) enables.
            PluginDescriptor(
                id = "superDelegateMode",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.MEMBERSHIP,
                titleKey = "plugin_membership_super_delegate_title",
                descriptionKey = "plugin_membership_super_delegate_desc",
                defaultOn = false,
            ),
        )

    /** Incentive-program plugins (P6.3). */
    val incentivePlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "incentiveProgramsEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.ENGAGEMENT,
                titleKey = "plugin_engagement_incentives_title",
                descriptionKey = "plugin_engagement_incentives_desc",
                defaultOn = true,
            ),
        )

    /** Every registered descriptor across all categories. */
    val all: List<PluginDescriptor> =
        coreModulePlugins + authPlugins + onboardingPlugins + profilePlugins + trackingPlugins +
            trackingTuningPlugins + syncSettingsPlugins + verificationPlugins + growthPlugins +
            membershipPlugins + incentivePlugins

    private fun onboardingFlag(
        id: String,
        defaultOn: Boolean,
    ): PluginDescriptor =
        PluginDescriptor(
            id = id,
            kind = PluginKind.CAPABILITY,
            category = PluginCategory.ONBOARDING,
            titleKey = "plugin_onboarding_${id.lowercase()}_title",
            descriptionKey = "plugin_onboarding_${id.lowercase()}_desc",
            defaultOn = defaultOn,
        )

    private val byId: Map<String, PluginDescriptor> = all.associateBy { it.id }

    fun byId(id: String): PluginDescriptor? = byId[id]

    fun inCategory(category: PluginCategory): List<PluginDescriptor> = all.filter { it.category == category }

    private fun coreModule(id: String): PluginDescriptor =
        PluginDescriptor(
            id = id,
            kind = PluginKind.TILE,
            category = PluginCategory.CORE_MODULES,
            // Resource names must be lowercase; the id itself keeps its camelCase for gating.
            titleKey = "plugin_core_${id.lowercase()}_title",
            descriptionKey = "plugin_core_${id.lowercase()}_desc",
            defaultOn = true,
        )
}
