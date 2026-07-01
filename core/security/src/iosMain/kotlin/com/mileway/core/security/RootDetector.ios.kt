package com.mileway.core.security

import platform.Foundation.NSFileManager

/**
 * iOS jailbreak heuristic: the presence of common jailbreak artefacts on the filesystem.
 * TODO(ios): a fuller check would also probe the `cydia://` URL scheme and a sandbox-escape write
 * to `/private/`. This path-based heuristic is representative for the demo.
 */
internal actual fun detectRootSignals(): List<String> {
    val fileManager = NSFileManager.defaultManager
    val jailbreakPaths =
        listOf(
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd",
            "/etc/apt",
            "/private/var/lib/apt/",
        )
    val signals = mutableListOf<String>()
    if (jailbreakPaths.any { fileManager.fileExistsAtPath(it) }) {
        signals += "jailbreak artefact found"
    }
    return signals
}
