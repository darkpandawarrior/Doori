package com.mileway.core.platform.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin bindings for the [com.mileway.core.platform] service interfaces.
 *
 * The `actual` implementations bind the concrete services:
 * - Android (`androidMain`): wired in Phase 2.2.
 * - iOS (`iosMain`): wired in Phase 4.
 */
expect fun platformModule(): Module
