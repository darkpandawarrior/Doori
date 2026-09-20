package com.mileway.feature.media.di

import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.feature.media.repository.MediaLibraryRepository
import com.mileway.feature.media.viewmodel.CloudLibraryViewModel
import com.mileway.feature.media.viewmodel.MediaViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The platform-agnostic half of feature:media's graph. Everything here resolves from commonMain
 * types, so iOS can register it (see `MilewayAppViewController`) — which it could not while this
 * file lived in `src/androidMain`.
 *
 * The one binding that stayed behind is `MediaRepository`: its only real implementation,
 * `RealMediaRepository`, takes an `android.content.Context`. Each platform contributes it
 * separately — Android via [androidMediaModule], iOS via `iosMediaModule`.
 */
val mediaModule =
    module {
        single { MediaLibraryRepository(get<MediaLibraryDao>()) }
        viewModelOf(::MediaViewModel)
        viewModelOf(::CloudLibraryViewModel)
    }
