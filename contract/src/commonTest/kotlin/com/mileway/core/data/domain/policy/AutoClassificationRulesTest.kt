package com.mileway.core.data.domain.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AutoClassificationRulesTest {
    @Test
    fun hoursIncludeStartExcludeEndAndSupportOvernightShifts() {
        val rules = AutoClassificationRules(hours = ClassificationHours(22, 6, TripClassification.BUSINESS))
        for (hour in listOf(22, 23, 0, 5)) assertEquals(TripClassification.BUSINESS, rules.resolve(null, emptyList(), hour, null)?.classification)
        for (hour in listOf(6, 21, -1, 24)) assertNull(rules.resolve(null, emptyList(), hour, null))
        assertFailsWith<IllegalArgumentException> { ClassificationHours(9, 9, TripClassification.BUSINESS) }
        assertFailsWith<IllegalArgumentException> { ClassificationHours(-1, 18, TripClassification.BUSINESS) }
    }

    @Test
    fun placeOrderIsExplicitAndMissingRulesDoNotInventBusinessUse() {
        val rules = AutoClassificationRules(places = mapOf("home" to TripClassification.PERSONAL, "office" to TripClassification.BUSINESS))
        assertEquals(TripClassification.PERSONAL, rules.resolve(null, listOf("home", "office"), 10, TripClassification.BUSINESS)?.classification)
        assertNull(AutoClassificationRules().resolve("car", listOf("office"), 10, null))
    }
}
