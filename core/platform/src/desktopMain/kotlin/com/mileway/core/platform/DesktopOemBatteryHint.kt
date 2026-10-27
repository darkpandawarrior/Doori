package com.mileway.core.platform

// desktop target has no OEM battery-management concept — always no hint.
actual fun currentDeviceManufacturer(): String? = null
