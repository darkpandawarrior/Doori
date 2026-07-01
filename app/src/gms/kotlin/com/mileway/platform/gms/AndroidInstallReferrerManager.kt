package com.mileway.platform.gms

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.mileway.core.platform.LocalReferralManager
import com.mileway.core.platform.ReferralData
import com.mileway.core.platform.ReferralManager

/**
 * RF.2: Android Install Referrer (gms flavor only). Decorates the shared [LocalReferralManager] (code
 * generation + redemption are unchanged) and, once, reads the Play Install Referrer URL, parses
 * `client_code` / `utm_source` / `utm_campaign`, and pushes the attribution into the shared store.
 *
 * The connection is opened and closed safely; any failure (no Play services, feature-not-supported) is
 * swallowed so it never crashes a sideloaded build.
 */
class AndroidInstallReferrerManager(
    private val context: Context,
    private val delegate: LocalReferralManager,
) : ReferralManager by delegate {
    fun captureInstallReferrer() {
        val client = InstallReferrerClient.newBuilder(context).build()
        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        runCatching {
                            val url = client.installReferrer.installReferrer
                            parseReferrer(url)?.let { delegate.capture(it) }
                        }
                    }
                    runCatching { client.endConnection() }
                }

                override fun onInstallReferrerServiceDisconnected() = Unit
            },
        )
    }

    private companion object {
        val CODE = Regex("(?:client_code|code)=([^&]+)")
        val SOURCE = Regex("utm_source=([^&]+)")
        val CAMPAIGN = Regex("utm_campaign=([^&]+)")

        fun parseReferrer(url: String?): ReferralData? {
            if (url.isNullOrBlank()) return null
            val code = CODE.find(url)?.groupValues?.getOrNull(1) ?: return null
            return ReferralData(
                code = code,
                source = SOURCE.find(url)?.groupValues?.getOrNull(1) ?: "install_referrer",
                campaign = CAMPAIGN.find(url)?.groupValues?.getOrNull(1),
            )
        }
    }
}
