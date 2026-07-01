package com.mileway.core.ui.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val coreUiPlatformModule: Module =
    module {
        single<DataStore<Preferences>> {
            // Resolve the context eagerly: produceFile runs lazily on first read, which may be
            // after the Koin scope that created this single has closed (e.g. in tests).
            val context = androidContext().applicationContext
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("theme_prefs") },
            )
        }
    }
