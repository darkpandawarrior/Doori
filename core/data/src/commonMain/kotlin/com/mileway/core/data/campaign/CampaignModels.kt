package com.mileway.core.data.campaign

/*
 * PLAN_V24 P5.4: campaign-marketing models (reference app campaign marketing).
 * Offline/mock — in core:data (the base module the Room entity, the profile hub AND the shared
 * HomeScreen marketing strip all reach). Closes MASTER_GAP:70.
 */

/** A campaign's lifecycle badge. */
enum class CampaignStatus { LIVE, UPCOMING, ENDED }

/**
 * A marketing campaign / feature announcement. [interestCaptured] is the one-shot "Get in touch"
 * flag (the CTA disables after capture, mirroring the source's capture-interest).
 */
data class Campaign(
    val id: String,
    val name: String,
    val description: String,
    val badge: String,
    val status: CampaignStatus,
    val mobileExclusive: Boolean,
    val contactEmail: String,
    val interestCaptured: Boolean,
)
