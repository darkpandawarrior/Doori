package com.mileway.feature.tracking.service

import com.siddharth.kmp.offlineoutbox.SubmitOutbox
import kotlinx.serialization.Serializable

// Journey telemetry (PLAN_V33 A4/B3 shape, applied to HardwareEvent): durable outbox payload for the
// hardware-event sync path — mirrors core/data/outbox's LocationBatch, but kept in this module
// instead of core/data/outbox because only :feature:tracking drains it. Carries only row ids (refs
// into the `hardware_events` table, same idea as LocationBatch.pointIds); the actual rows never
// leave Room except through the network call itself.
@Serializable
data class HardwareEventBatch(
    val token: String,
    val eventIds: List<Long>,
)

typealias HardwareEventBatchOutbox = SubmitOutbox<HardwareEventBatch>

/**
 * Koin qualifier for the [HardwareEventBatchOutbox] binding. Required, not cosmetic: `SubmitOutbox<T>`
 * resolves by its erased type here, so an unqualified binding shadows `SubmitOutbox<TripDraft>` and
 * `MilesSubmitSyncer` ends up with the hardware-event outbox instead of the trip-draft one.
 */
const val HARDWARE_EVENT_OUTBOX = "hardwareEventOutbox"
