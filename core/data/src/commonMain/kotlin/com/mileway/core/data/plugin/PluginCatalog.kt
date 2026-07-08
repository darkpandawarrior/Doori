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
        )

    /** Every registered descriptor across all categories. */
    val all: List<PluginDescriptor> =
        coreModulePlugins + authPlugins + onboardingPlugins + profilePlugins + trackingPlugins +
            verificationPlugins + growthPlugins

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
