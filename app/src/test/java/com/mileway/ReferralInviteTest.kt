package com.mileway

import com.mileway.core.ui.components.buildReferralInvite
import org.junit.Test
import kotlin.test.assertTrue

/** RF.4: invite message embeds the code + App-Links URL. */
class ReferralInviteTest {
    @Test
    fun `invite embeds code and app-link url`() {
        val msg = buildReferralInvite("MTABC123")
        assertTrue(msg.contains("MTABC123"))
        assertTrue(msg.contains("https://mileway.example.com/referral?code=MTABC123"))
    }
}
