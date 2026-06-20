package com.miletracker.core.ui.di

import com.miletracker.core.ui.theme.LocaleController
import com.miletracker.core.ui.theme.ThemeController
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform-provided definitions for core UI infrastructure — currently the
 * `DataStore<Preferences>` that backs theme persistence.
 */
expect val coreUiPlatformModule: Module

/**
 * Koin module for shared UI infrastructure that must outlive any single screen,
 * e.g. the app-wide [ThemeController] read by the shell and written by Settings.
 */
val coreUiModule =
    module {
        includes(coreUiPlatformModule)
        single { ThemeController(prefs = get()) }
        // UX.6: shared in-app locale state (Settings writes it; features observe it).
        single { LocaleController() }
    }
