package com.mileway.feature.tracking.detection

import kotlin.test.Test
import kotlin.test.assertNull

class TrackingKoinAccessTest {
    @Test
    fun coldBackgroundCallbackCanReadUninitializedKoinWithoutThrowing() {
        assertNull(TrackingKoinAccess.currentOrNull())
    }
}
