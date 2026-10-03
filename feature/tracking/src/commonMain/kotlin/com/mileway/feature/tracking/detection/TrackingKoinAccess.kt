package com.mileway.feature.tracking.detection

import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools

/** Background callbacks may precede UI bootstrap; reading the context must never start it or throw. */
internal object TrackingKoinAccess {
    fun currentOrNull(): Koin? = KoinPlatformTools.defaultContext().getOrNull()
}
