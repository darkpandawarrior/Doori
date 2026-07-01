package com.mileway

import com.mileway.feature.tracking.service.location.ActivityTypeMapper
import com.mileway.feature.tracking.service.location.RecognizedActivity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O.2: the pure DetectedActivity-code → [RecognizedActivity] mapping (ON_FOOT/WALKING/RUNNING collapse to
 * ON_FOOT; unknown codes fall back to UNKNOWN).
 */
class ActivityTypeMapperTest {
    @Test
    fun `maps detected activity codes to recognized activities`() {
        assertEquals(RecognizedActivity.IN_VEHICLE, ActivityTypeMapper.fromDetectedType(0))
        assertEquals(RecognizedActivity.ON_BICYCLE, ActivityTypeMapper.fromDetectedType(1))
        assertEquals(RecognizedActivity.ON_FOOT, ActivityTypeMapper.fromDetectedType(2)) // ON_FOOT
        assertEquals(RecognizedActivity.ON_FOOT, ActivityTypeMapper.fromDetectedType(7)) // WALKING
        assertEquals(RecognizedActivity.ON_FOOT, ActivityTypeMapper.fromDetectedType(8)) // RUNNING
        assertEquals(RecognizedActivity.STILL, ActivityTypeMapper.fromDetectedType(3))
        assertEquals(RecognizedActivity.UNKNOWN, ActivityTypeMapper.fromDetectedType(4)) // UNKNOWN
        assertEquals(RecognizedActivity.UNKNOWN, ActivityTypeMapper.fromDetectedType(99))
    }
}
