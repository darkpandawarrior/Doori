package com.mileway.feature.media.di

import android.content.Context
import com.mileway.feature.media.repository.MediaRepository
import com.mileway.feature.media.repository.RealMediaRepository
import org.koin.dsl.module

/**
 * The Android-only remainder of [mediaModule]: `RealMediaRepository` does EXIF-corrected bitmap
 * work and ML Kit OCR, so it takes an `android.content.Context`. Register it alongside
 * [mediaModule] wherever [mediaModule] is registered on Android.
 */
val androidMediaModule =
    module {
        single<MediaRepository> { RealMediaRepository(get<Context>()) }
    }
