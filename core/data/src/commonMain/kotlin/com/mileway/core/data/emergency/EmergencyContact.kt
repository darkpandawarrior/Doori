package com.mileway.core.data.emergency

/**
 * PLAN_V24 P3.5: an emergency contact as consumed by the profile management screen and the
 * tracking SOS sheet. [countryCode] is a dial code (e.g. "+91"); [displayNumber] joins it with the
 * national number for banners/share text.
 */
data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNo: String,
    val countryCode: String,
) {
    val displayNumber: String get() = "$countryCode $phoneNo"
}
