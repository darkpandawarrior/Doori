package com.mileway

import com.mileway.core.platform.MotionSensorProvider
import com.mileway.core.platform.MotionState
import com.mileway.core.platform.toMotionState
import com.mileway.feature.tracking.detection.DetectedDriveRecorder
import com.mileway.feature.tracking.detection.DriveDeparture
import com.mileway.feature.tracking.detection.DriveStartSource
import com.mileway.feature.tracking.detection.PendingDrive
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** noGms heuristic wait, while this process is alive. Motion is not proof of a vehicle. */
class HeuristicDriveStartSource(
    private val sensors: MotionSensorProvider,
    private val recorder: DetectedDriveRecorder,
) : DriveStartSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private val mutableWaiting = MutableStateFlow(false)
    override val waiting = mutableWaiting.asStateFlow()
    override val waitLabel = "Wait for movement (motion heuristic)"

    override suspend fun arm(
        drive: PendingDrive,
        departure: DriveDeparture?,
    ): Boolean {
        // noGms has no OS geofencing source; never pretend an explicit departure guard was met.
        if (departure != null) return false
        disarm()
        mutableWaiting.value = true
        job =
            scope.launch {
                sensors.start()
                try {
                    sensors.readings
                        .toMotionState()
                        .drop(1)
                        .first { it == MotionState.MOVING }
                } finally {
                    sensors.stop()
                    mutableWaiting.value = false
                }
                runCatching { recorder.start(drive) }.onFailure {
                    Napier.w("Motion recording start failed", it, tag = "DriveWake")
                }
            }
        return true
    }

    override suspend fun disarm() {
        job?.cancelAndJoin()
        job = null
        mutableWaiting.value = false
    }
}
