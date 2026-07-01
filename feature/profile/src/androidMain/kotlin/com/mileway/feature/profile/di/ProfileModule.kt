package com.mileway.feature.profile.di

import com.mileway.feature.profile.repository.AdvanceRepository
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.ProfileRepository
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import com.mileway.feature.profile.viewmodel.DemoSettingsViewModel
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule =
    module {
        single<ProfileRepository> { FakeProfileRepository() }
        single { AdvanceRepository() }
        viewModelOf(::ProfileViewModel)
        viewModel { AdvanceViewModel(get()) }
        viewModelOf(::DemoSettingsViewModel)
    }
