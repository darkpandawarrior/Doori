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
     * Signup-onboarding form config (P2.1) — the reference app's signup-onboarding gating flags.
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
            // PLAN_V24 P10.6: manager-only reportee tracking view (per the reference app's manager
            // reportee-tracking flags). A whole new profile-hub destination (TILE).
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
            // PLAN_V24 P10.5: floating-bubble toggle. Consumer = LocationTrackingService, which starts
            // FloatingBubbleService for the running trip when this + the overlay master are on.
            // defaultOn=false = today's behavior (the bubble was never started before this task).
            PluginDescriptor(
                id = "show_tracking_bubble",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_show_tracking_bubble_title",
                descriptionKey = "plugin_show_tracking_bubble_desc",
                defaultOn = false,
            ),
            // PLAN_V24 P10.5: overlay master (reference showTrackingOverlay). Consumer =
            // LocationTrackingService — when off, no tracking overlay/bubble shows even if the bubble
            // toggle is on. defaultOn=true matches the reference default; gates only the opt-in bubble.
            PluginDescriptor(
                id = "track_show_overlay",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_track_show_overlay_title",
                descriptionKey = "plugin_track_show_overlay_desc",
                defaultOn = true,
            ),
            // PLAN_V24 P10.5: reverse-geocode source. Consumer = OfflineLocationNameResolver (Android):
            // off = the local gazetteer label; on = a simulated-remote lookup returning a richer
            // address. defaultOn=false keeps the local-table label (unchanged names/goldens).
            PluginDescriptor(
                id = "reverse_geocode_remote",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_reverse_geocode_remote_title",
                descriptionKey = "plugin_reverse_geocode_remote_desc",
                defaultOn = false,
            ),
            // PLAN_V24 P11.3: the "Head home" destination panel on the tracking screen. defaultOn=false
            // keeps the tracking golden byte-identical; only the Gig Driver persona turns it on.
            PluginDescriptor(
                id = "destinationMode",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.TRACKING,
                titleKey = "plugin_tracking_destination_mode_title",
                descriptionKey = "plugin_tracking_destination_mode_desc",
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
     * PLAN_V24 P10.3 — the full location fine-tuning key set. Every knob here is a field of
     * `AbnormalDetectionConfig` that `LocationProcessor` actually consumes on the live pipeline
     * (jitter gates, speed bands, spike/gap-recovery tiers, movement window). Each defaults to
     * exactly the shipped `AbnormalDetectionConfig.DEFAULT` value, so an untouched knob leaves the
     * tracking math byte-for-byte unchanged; moving one on the Master Plugin page (which renders the
     * categorized VALUE editor) flows through `RegistryAbnormalDetectionSource` →
     * `TrackingConfigManager.abnormalDetectionConfig` → `LocationProcessor`. No dead knobs.
     *
     * Reference keys with no Mileway consumer (Kalman params, battery thresholds, path simplification,
     * activity detection, event aggregation, min/max accuracy pair) are NOT registered here — see
     * PROGRESS for the skip list. The boolean experimental optimisation toggles live in P10.7.
     */
    val abnormalTuningPlugins: List<PluginDescriptor> =
        listOf(
            // Speed-classification bands (m/s).
            tuningDouble("track_walking_max_mps", 2.5, 0.5, 5.0, 0.1, "m/s"),
            tuningDouble("track_cycling_max_mps", 7.0, 3.0, 15.0, 0.5, "m/s"),
            tuningDouble("track_stationary_speed_mps", 1.2, 0.2, 3.0, 0.1, "m/s"),
            tuningDouble("track_movement_history_mps", 1.5, 0.2, 5.0, 0.1, "m/s"),
            // Per-band min-displacement jitter gates (m).
            tuningDouble("track_walking_jitter_m", 2.0, 0.5, 10.0, 0.5, "m"),
            tuningDouble("track_cycling_jitter_m", 3.0, 0.5, 15.0, 0.5, "m"),
            tuningDouble("track_driving_jitter_m", 5.0, 1.0, 25.0, 0.5, "m"),
            tuningDouble("track_stationary_jitter_m", 1.2, 0.2, 5.0, 0.1, "m"),
            // Movement-history window (samples).
            tuningInt("track_speed_history_size", 5, 2, 20, 1, null),
            // Spike / abnormal distance gates (m).
            tuningDouble("track_spike_hard_gate_m", 5_000.0, 500.0, 20_000.0, 100.0, "m"),
            tuningDouble("track_gap_max_distance_m", 10_000.0, 1_000.0, 50_000.0, 500.0, "m"),
            // Gap-recovery windows (s).
            tuningInt("track_gap_min_s", 30, 5, 120, 5, "s"),
            tuningInt("track_gap_5m_s", 300, 60, 900, 30, "s"),
            tuningInt("track_gap_1h_s", 3_600, 600, 7_200, 60, "s"),
            tuningInt("track_gap_6h_s", 21_600, 7_200, 43_200, 600, "s"),
            // Gap-recovery relaxed speed caps (m/s).
            tuningDouble("track_gap_tier_5m_mps", 150.0, 30.0, 300.0, 5.0, "m/s"),
            tuningDouble("track_gap_tier_1h_mps", 100.0, 20.0, 250.0, 5.0, "m/s"),
            tuningDouble("track_gap_tier_6h_mps", 60.0, 10.0, 200.0, 5.0, "m/s"),
        )

    /**
     * PLAN_V24 P10.7 — the experimental-optimizations toggle set (7 toggles) matching the reference
     * "Experimental optimizations" card. Registry-backed CAPABILITY plugins (experimental=true so
     * they only show on the Master Plugin page behind the 7-tap unlock; the settings card always
     * shows them). All default OFF so a fresh account renders unchanged. These replace the former
     * in-memory `ExperimentalFlags` (they now persist per-account instead of being remember-only).
     * Behavioural wiring into the live pipeline is sequenced under the live-pipeline phase; see
     * PROGRESS for the per-toggle consumer status.
     */
    val experimentalPlugins: List<PluginDescriptor> =
        listOf(
            experimentalFlag("exp_battery_aware"),
            experimentalFlag("exp_low_end_tuning"),
            experimentalFlag("exp_aggressive_gps"),
            experimentalFlag("exp_capture_kalman"),
            experimentalFlag("exp_path_simplification"),
            experimentalFlag("exp_gap_telemetry"),
            experimentalFlag("exp_imu_logging"),
        )

    /**
     * Mileage-sync settings (P10.2) — the reference app's mileage-sync settings card. Each toggle gates a real
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
            // PLAN_V24 P11.4: the Ecometer dashboard — a profile-hub TILE. defaultOn=false keeps the
            // hub gallery byte-identical; the Consumer + Gig Driver presets turn it on.
            PluginDescriptor(
                id = "ecometerEnabled",
                kind = PluginKind.TILE,
                category = PluginCategory.GROWTH,
                titleKey = "plugin_growth_ecometer_title",
                descriptionKey = "plugin_growth_ecometer_desc",
                defaultOn = false,
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

    /**
     * Vehicle plugins (P11). [perKmRatesEnabled] is the per-km policy-rate gate consumed by
     * [com.mileway.core.data.vehicle.VehicleRateRepository]: on ⇒ vehicle keys resolve to their
     * catalog policy rate (₹/km chips + reimbursable amounts); off ⇒ no rate at all. Defaults OFF so
     * the baseline persona shows no rates; the Corporate/Gig presets turn it on.
     */
    val vehiclePlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "perKmRatesEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.VEHICLES,
                titleKey = "plugin_vehicles_per_km_rates_title",
                descriptionKey = "plugin_vehicles_per_km_rates_desc",
                defaultOn = false,
            ),
            // P11.2: the multi-vehicle garage — a profile-hub TILE. defaultOn=false keeps the hub
            // gallery byte-identical; the Gig preset turns it on.
            PluginDescriptor(
                id = "vehicleGarage",
                kind = PluginKind.TILE,
                category = PluginCategory.VEHICLES,
                titleKey = "plugin_vehicles_garage_title",
                descriptionKey = "plugin_vehicles_garage_desc",
                defaultOn = false,
            ),
            // P11.2: multi-vehicle capability. off ⇒ single-vehicle mode (the add affordance hides
            // once one vehicle exists); on ⇒ the garage keeps many. defaultOn=false = single-vehicle.
            PluginDescriptor(
                id = "multipleVehiclesEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.VEHICLES,
                titleKey = "plugin_vehicles_multiple_title",
                descriptionKey = "plugin_vehicles_multiple_desc",
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

    /**
     * Engagement/trust plugins (P12). [badgesEnabled] gates the whole earned-badges + compliments
     * section on the profile hub; [showRating] gates just the aggregate rating chip inside it. Both
     * default OFF so the baseline hub golden stays byte-identical; the Gig Driver persona turns them
     * on (Driver-shaped rating + achievements layout).
     */
    val badgePlugins: List<PluginDescriptor> =
        listOf(
            PluginDescriptor(
                id = "badgesEnabled",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.ENGAGEMENT,
                titleKey = "plugin_engagement_badges_title",
                descriptionKey = "plugin_engagement_badges_desc",
                defaultOn = false,
            ),
            PluginDescriptor(
                id = "showRating",
                kind = PluginKind.CAPABILITY,
                category = PluginCategory.ENGAGEMENT,
                titleKey = "plugin_engagement_show_rating_title",
                descriptionKey = "plugin_engagement_show_rating_desc",
                defaultOn = false,
            ),
        )

    /** Every registered descriptor across all categories. */
    val all: List<PluginDescriptor> =
        coreModulePlugins + authPlugins + onboardingPlugins + profilePlugins + trackingPlugins +
            trackingTuningPlugins + abnormalTuningPlugins + experimentalPlugins + syncSettingsPlugins +
            verificationPlugins + growthPlugins + membershipPlugins + incentivePlugins + vehiclePlugins +
            badgePlugins

    // PLAN_V24 P10.3: VALUE-plugin builders for the fine-tuning key set. Title/description keys are
    // derived from the id (plugin_<id>_title / _desc) so a new knob needs only its strings entry.
    private fun tuningDouble(
        id: String,
        default: Double,
        min: Double,
        max: Double,
        step: Double,
        unit: String?,
    ): PluginDescriptor =
        PluginDescriptor(
            id = id,
            kind = PluginKind.VALUE,
            category = PluginCategory.TRACKING_TUNING,
            titleKey = "plugin_${id}_title",
            descriptionKey = "plugin_${id}_desc",
            valueSpec = PluginValueSpec.DoubleSpec(defaultValue = default, min = min, max = max, step = step, unit = unit),
        )

    private fun tuningInt(
        id: String,
        default: Int,
        min: Int,
        max: Int,
        step: Int,
        unit: String?,
    ): PluginDescriptor =
        PluginDescriptor(
            id = id,
            kind = PluginKind.VALUE,
            category = PluginCategory.TRACKING_TUNING,
            titleKey = "plugin_${id}_title",
            descriptionKey = "plugin_${id}_desc",
            valueSpec = PluginValueSpec.IntSpec(defaultValue = default, min = min, max = max, step = step, unit = unit),
        )

    // PLAN_V24 P10.7: experimental CAPABILITY-plugin builder. TRACKING category, experimental=true,
    // defaultOn=false. Title/description keys derived from the id (plugin_<id>_title / _desc).
    private fun experimentalFlag(id: String): PluginDescriptor =
        PluginDescriptor(
            id = id,
            kind = PluginKind.CAPABILITY,
            category = PluginCategory.TRACKING,
            titleKey = "plugin_${id}_title",
            descriptionKey = "plugin_${id}_desc",
            defaultOn = false,
            experimental = true,
        )

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
