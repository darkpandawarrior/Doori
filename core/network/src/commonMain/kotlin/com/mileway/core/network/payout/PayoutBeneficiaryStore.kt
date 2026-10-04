package com.mileway.core.network.payout

import com.russhwolf.settings.Settings

/** Stores only a VPA in SecureSettingsFactory output, using AuthTokenStore's Settings seam. */
class PayoutBeneficiaryStore(
    private val settings: Settings,
) {
    fun read(): String? = settings.getStringOrNull(Key)

    fun store(vpa: String) {
        val normalized = vpa.trim()
        require(isValid(normalized)) { "Enter a payout address as name@handle" }
        settings.putString(Key, normalized)
    }

    fun clear() = settings.remove(Key)

    companion object {
        private const val MaxVpaLength = 255
        private const val Key = "payout_beneficiary_vpa"

        fun isValid(vpa: String): Boolean = vpa.length <= MaxVpaLength && vpa.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*@[A-Za-z0-9][A-Za-z0-9.-]*"))
    }
}
