package com.miletracker.core.ui

import com.miletracker.core.platform.IosReferralCapture
import com.miletracker.core.platform.LocalReferralManager
import com.miletracker.core.platform.ReferralData
import com.miletracker.core.platform.ReferralManager

/**
 * RF.3: Swift → KMP referral bridge, exported in the Mileway framework.
 *
 * `iOSApp.swift` calls [captureDeferred] once at first launch (reads the pasteboard token), and
 * [captureCode] when a Universal-Link `?code=` arrives. Shared screens read [manager] for the user's own
 * code + the pending captured referral.
 */
object ReferralBridge {
    val manager: ReferralManager = LocalReferralManager()
    private val local get() = manager as LocalReferralManager

    fun captureDeferred() {
        IosReferralCapture.pasteboardReferral()?.let { local.capture(it) }
    }

    fun captureCode(code: String) {
        local.capture(ReferralData(code = code, source = "universal_link"))
    }
}
