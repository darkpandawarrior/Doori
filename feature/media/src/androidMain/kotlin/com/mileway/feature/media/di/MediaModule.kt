package com.mileway.feature.media.di

import android.content.Context
import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.feature.media.repository.MediaLibraryRepository
import com.mileway.feature.media.repository.MediaRepository
import com.mileway.feature.media.repository.RealMediaRepository
import com.mileway.feature.media.viewmodel.CloudLibraryViewModel
import com.mileway.feature.media.viewmodel.MediaViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mediaModule =
    module {
        single<MediaRepository> { RealMediaRepository(get<Context>()) }
        single { MediaLibraryRepository(get<MediaLibraryDao>()) }
        viewModelOf(::MediaViewModel)
        viewModelOf(::CloudLibraryViewModel)
    }
