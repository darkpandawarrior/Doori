package com.mileway.core.network.payout

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PayoutBeneficiaryStoreTest {
    @Test
    fun beneficiarySurvivesANewStoreAndClearRemovesIt() {
        val settings = MapSettings()
        PayoutBeneficiaryStore(settings).store("fixture.user@bank")
        val reloaded = PayoutBeneficiaryStore(settings)
        assertEquals("fixture.user@bank", reloaded.read())
        reloaded.clear()
        assertNull(PayoutBeneficiaryStore(settings).read())
    }

    @Test
    fun invalidAddressCannotOverwriteExistingBeneficiary() {
        val store = PayoutBeneficiaryStore(MapSettings())
        store.store("fixture@bank")
        for (value in listOf("", "no-handle", "@bank", "user@", "user name@bank", "name@bank@other")) {
            assertFailsWith<IllegalArgumentException> { store.store(value) }
            assertEquals("fixture@bank", store.read())
        }
    }
}
