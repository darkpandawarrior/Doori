package com.mileway.feature.tracking

import com.mileway.core.data.domain.policy.AutoClassificationRules
import com.mileway.core.data.domain.policy.ClassificationHours
import com.mileway.core.data.domain.policy.ClassificationSource
import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.domain.policy.TripClassification
import com.mileway.core.data.domain.policy.classifyTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutoClassificationRulesTest {
    @Test
    fun precedenceIsVehicleThenPlaceThenHoursThenLastTrip() {
        val rules =
            AutoClassificationRules(
                vehicles = mapOf("car" to TripClassification.PERSONAL),
                places = mapOf("office" to TripClassification.BUSINESS),
                hours = ClassificationHours(9, 18, TripClassification.PERSONAL),
            )
        val engine = PolicyEngine(emptyList())
        assertEquals(ClassificationSource.VEHICLE, engine.classifyTrip(rules, "car", listOf("office"), 10, TripClassification.BUSINESS)?.source)
        assertEquals(ClassificationSource.PLACE, engine.classifyTrip(rules, null, listOf("office"), 10, TripClassification.PERSONAL)?.source)
        assertEquals(ClassificationSource.HOURS, engine.classifyTrip(rules, null, emptyList(), 10, TripClassification.BUSINESS)?.source)
        assertEquals(ClassificationSource.LAST_TRIP, engine.classifyTrip(rules, null, emptyList(), 18, TripClassification.BUSINESS)?.source)
        assertNull(engine.classifyTrip(rules, null, emptyList(), 18, null))
    }
}
