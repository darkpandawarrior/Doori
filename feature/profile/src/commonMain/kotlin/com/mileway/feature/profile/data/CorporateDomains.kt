package com.mileway.feature.profile.data

/**
 * PLAN_V24 P4.4: the demo allow-list of recognised company email domains (the `:stub` "company
 * list" the corporate-email flow validates against). Offline — a real deployment would resolve
 * this from the tenant config.
 */
object CorporateDomains {
    val allowed: List<String> =
        listOf(
            "acmecorp.com",
            "globex.com",
            "initech.io",
            "umbrella.co",
            "mileway.com",
        )

    /** True when [email]'s domain is one of the recognised corporate domains. */
    fun isRecognised(email: String): Boolean {
        val domain = email.substringAfter('@', missingDelimiterValue = "").trim().lowercase()
        return domain.isNotBlank() && domain in allowed
    }
}
