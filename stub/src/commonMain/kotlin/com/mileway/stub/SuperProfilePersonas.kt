package com.mileway.stub

import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.data.plugin.PersonaSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/*
 * PLAN_V24 P0.2 — persona presets: the PRESET resolution layer of the Plugin Registry. Four seeded
 * personas each declare a different plugin mix so the same app renders four visibly different
 * stories (the super-profile demo). Generalized from the reference app's named option→Boolean
 * profile bundles to product plugins, with its per-tenant flag sets as the content model.
 *
 * A preset override only takes effect once its plugin's descriptor is registered in
 * [com.mileway.core.data.plugin.PluginCatalog]. P0.1 registered CORE_MODULES, so the module
 * toggles below are live TODAY; the forward-referenced feature ids (mfaRequired, vehicleGarage, …)
 * are inert until their owning P1–P13 phase registers them — at which point that phase inherits
 * persona differentiation for free. Ids are strings by design: presets are declarative content,
 * gated by descriptor existence, not compile-time coupled to every future phase.
 */

/** One persona's plugin mix. [overrides] flattens to the uniform raw-string map the registry reads. */
data class PersonaPreset(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val toggles: Map<String, Boolean> = emptyMap(),
    val values: Map<String, String> = emptyMap(),
) {
    fun overrides(): Map<String, String> = toggles.mapValues { it.value.toString() } + values
}

/**
 * The four seeded personas + the account→persona binding. Bound to the existing demo accounts by
 * id (display names stay as-is — differentiation is the plugin mix, not the label). Any unmapped
 * or null account (guest login) resolves to [MinimalGuest].
 */
object SuperProfilePersonas {
    // Core-module plugin ids (registered by P0.1). Toggling these hides whole modules.
    private const val TRACKING = "tracking"
    private const val LOGGING = "logging"
    private const val EXPENSES = "expenses"
    private const val TRAVEL = "travel"
    private const val APPROVALS = "approvals"
    private const val PAYABLES = "payables"
    private const val PAYMENTS = "payments"
    private const val EVENTS = "events"
    private const val CARDS = "cards"
    private const val AGENT = "agent"
    private const val NOTIFICATION_CENTRE = "notificationCentre"
    private const val REFERRAL_CARD = "referralCard"
    private const val MARKETING_STRIP = "marketingStrip"

    /** Enterprise expense identity — MFA, delegation, club, campaigns, manager view. */
    val CorporateCommuter =
        PersonaPreset(
            id = "corporate_commuter",
            nameKey = "persona_corporate_commuter_name",
            descriptionKey = "persona_corporate_commuter_desc",
            toggles =
                mapOf(
                    // Modules the corporate persona keeps; referralCard/marketingStrip are consumer-flavoured.
                    REFERRAL_CARD to false,
                    // Forward feature ids (inert until their phase registers them):
                    "mfaRequired" to true,
                    "showPasswordSettings" to true,
                    "clubEnabled" to true,
                    "campaignMarketingEnabled" to true,
                    "superDelegateMode" to true,
                    "trackMileageManagerView" to true,
                    // P11.1: enterprise personas get per-km policy rates; the consumer persona gets none.
                    "perKmRatesEnabled" to true,
                    "verificationCentreEnabled" to false,
                    "subscriptionsEnabled" to false,
                    "destinationMode" to false,
                    "walletLinkingEnabled" to false,
                ),
        )

    /** Consumer super-app identity — phone-OTP, referrals, coupons, wallets, ecometer. */
    val SuperAppConsumer =
        PersonaPreset(
            id = "super_app_consumer",
            nameKey = "persona_super_app_consumer_name",
            descriptionKey = "persona_super_app_consumer_desc",
            toggles =
                mapOf(
                    EXPENSES to false,
                    TRAVEL to false,
                    APPROVALS to false,
                    PAYABLES to false,
                    "phoneLoginEnabled" to true,
                    // P2.1: signup onboarding form (last name + email optional, skip on).
                    "signupOnboardingEnabled" to true,
                    "genderRequired" to true,
                    "showPromoOnboarding" to true,
                    "referralProgramEnabled" to true,
                    "couponsEnabled" to true,
                    "scratchRewardsEnabled" to true,
                    "offersHubEnabled" to true,
                    "walletLinkingEnabled" to true,
                    "emergencyContactsEnabled" to true,
                    "savedPlacesEnabled" to true,
                    "ecometerEnabled" to true,
                    "mfaRequired" to false,
                    "verificationCentreEnabled" to false,
                ),
        )

    /** Gig/KYC identity — verification centre, garage, per-km rates, plans, tour. */
    val GigDriver =
        PersonaPreset(
            id = "gig_driver",
            nameKey = "persona_gig_driver_name",
            descriptionKey = "persona_gig_driver_desc",
            toggles =
                mapOf(
                    TRAVEL to false,
                    EXPENSES to false,
                    APPROVALS to false,
                    EVENTS to false,
                    "verificationCentreEnabled" to true,
                    "vehicleGarage" to true,
                    "multipleVehiclesEnabled" to true,
                    "perKmRatesEnabled" to true,
                    "subscriptionsEnabled" to true,
                    "incentiveProgramsEnabled" to true,
                    "destinationMode" to true,
                    "payoutDetailsEnabled" to true,
                    "trainingTour" to true,
                    "selfAudit" to true,
                    "signature" to true,
                    "showRating" to true,
                    "ecometerEnabled" to true,
                    "clubEnabled" to false,
                    "campaignMarketingEnabled" to false,
                ),
        )

    /** Tracking + logging only; everything else off. Also the guest-login preset. */
    val MinimalGuest =
        PersonaPreset(
            id = "minimal_guest",
            nameKey = "persona_minimal_guest_name",
            descriptionKey = "persona_minimal_guest_desc",
            toggles =
                mapOf(
                    EXPENSES to false,
                    TRAVEL to false,
                    APPROVALS to false,
                    PAYABLES to false,
                    PAYMENTS to false,
                    EVENTS to false,
                    CARDS to false,
                    AGENT to false,
                    NOTIFICATION_CENTRE to false,
                    REFERRAL_CARD to false,
                    MARKETING_STRIP to false,
                    // TRACKING + LOGGING intentionally omitted → resolve to their DEFAULT (on).
                ),
        )

    val all: List<PersonaPreset> = listOf(CorporateCommuter, SuperAppConsumer, GigDriver, MinimalGuest)

    private val byAccount: Map<String, PersonaPreset> =
        mapOf(
            "ACC-001" to CorporateCommuter,
            "ACC-002" to SuperAppConsumer,
            "ACC-003" to GigDriver,
        )

    /** The persona bound to [accountId]; unmapped/null (guest) → [MinimalGuest]. */
    fun forAccount(accountId: String?): PersonaPreset = accountId?.let { byAccount[it] } ?: MinimalGuest
}

/**
 * PLAN_V24 P0.2 — the `:stub` implementation of the registry's PRESET layer, replacing
 * `EmptyPersonaPresetProvider`. Pure/offline: maps the active account to its persona's raw
 * overrides. Bound in `stubModule` (overrides the core:data Empty default via Koin override).
 */
class StubPersonaPresetProvider : PersonaPresetProvider {
    override fun presetOverrides(accountId: String?): Flow<Map<String, String>> =
        flowOf(SuperProfilePersonas.forAccount(accountId).overrides())

    override fun availablePersonas(): List<PersonaSummary> =
        SuperProfilePersonas.all.map { preset ->
            PersonaSummary(
                id = preset.id,
                nameKey = preset.nameKey,
                descriptionKey = preset.descriptionKey,
                overrides = preset.overrides(),
            )
        }
}
