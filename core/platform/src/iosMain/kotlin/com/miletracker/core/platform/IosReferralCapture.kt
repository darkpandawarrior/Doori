package com.miletracker.core.platform

import platform.UIKit.UIPasteboard

/**
 * RF.3 — iOS deferred-referral capture (no third-party SDK). Reads a referral token left on the general
 * pasteboard by a "share" link (format `miletracker-ref:<CODE>`), the offline-friendly iOS equivalent of
 * the Android Install Referrer. The Universal-Link `?code=` path is handled by the DeepLinkRouter (DL.3).
 */
object IosReferralCapture {
    private val PASTEBOARD_TOKEN = Regex("miletracker-ref:([A-Za-z0-9]+)")

    fun pasteboardReferral(): ReferralData? {
        val text = UIPasteboard.generalPasteboard.string ?: return null
        val match = PASTEBOARD_TOKEN.find(text) ?: return null
        return ReferralData(code = match.groupValues[1], source = "pasteboard")
    }
}
