package com.mileway.feature.tracking.manager

import android.app.ActivityManager
import android.content.Context

/** Android actual: total physical RAM via [ActivityManager.MemoryInfo], mirrors the iOS actual. */
class AndroidDeviceRamSource(
    private val context: Context,
) : DeviceRamSource {
    override fun totalRamBytes(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return Runtime.getRuntime().maxMemory()
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }
}
