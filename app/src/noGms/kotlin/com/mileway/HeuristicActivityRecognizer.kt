package com.mileway

import com.mileway.feature.tracking.service.location.ActivityRecognizer
import com.mileway.feature.tracking.service.location.RecognizedActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * noGms/F-Droid flavor [ActivityRecognizer]: no Play Services on this flavor, so there is no
 * classifier here yet — a single UNKNOWN emission, never updated. The IMU MotionState fusion
 * (`core:platform`) remains the real offline STILL/MOVING signal on this flavor; this stub only
 * exists so `noGms` has *a* binding instead of the tracking service failing to resolve one.
 *
 * ponytail: this is a placeholder, not the noGms activity classifier — PLAN_V37 Phase 2/3 replaces
 * it with an on-device TFLite model (`HeuristicActivityRecognizer`/`TfliteActivityRecognizer`
 * naming per the plan); upgrade there, not here.
 */
class HeuristicActivityRecognizer : ActivityRecognizer {
    override val activity: Flow<RecognizedActivity> = flowOf(RecognizedActivity.UNKNOWN)
}
