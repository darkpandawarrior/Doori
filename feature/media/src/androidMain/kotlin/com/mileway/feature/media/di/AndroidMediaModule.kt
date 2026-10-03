package com.mileway.feature.media.di

import com.mileway.feature.media.repository.MediaRepository
import com.mileway.feature.media.repository.UnavailableOcrMediaRepository
import org.koin.dsl.module

/** Default manual OCR fallback; the app GMS module overrides it with the real recognizer. */
val androidMediaModule =
    module {
        single<MediaRepository> { UnavailableOcrMediaRepository() }
    }
