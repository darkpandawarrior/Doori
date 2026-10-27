package com.mileway.core.platform

// iOS has no OEM battery-management skinning to warn about — always no hint.
actual fun currentDeviceManufacturer(): String? = null
