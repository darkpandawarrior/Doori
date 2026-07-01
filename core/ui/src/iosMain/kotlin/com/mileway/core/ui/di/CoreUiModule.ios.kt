package com.mileway.core.ui.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSTemporaryDirectory

actual val coreUiPlatformModule: Module =
    module {
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = { (NSTemporaryDirectory() + "theme_prefs.preferences_pb").toPath() },
            )
        }
    }
