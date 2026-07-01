package com.mileway.core.security

import android.os.Build
import java.io.File

internal actual fun detectRootSignals(): List<String> {
    val signals = mutableListOf<String>()
    if (isSuBinaryPresent()) signals += "su binary found"
    if (isTestKeysBuild()) signals += "test-keys build"
    if (isMagiskPresent()) signals += "Magisk detected"
    return signals
}

private fun isSuBinaryPresent(): Boolean {
    val paths =
        listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
        )
    return paths.any { File(it).exists() }
}

private fun isTestKeysBuild(): Boolean = Build.TAGS?.contains("test-keys") == true

private fun isMagiskPresent(): Boolean = File("/sbin/.magisk").exists() || File("/data/adb/magisk").exists()
