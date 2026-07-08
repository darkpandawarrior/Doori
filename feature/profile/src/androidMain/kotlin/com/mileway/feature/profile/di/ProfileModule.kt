package com.mileway.feature.profile.di

import com.mileway.feature.profile.repository.ActiveSessionsRepository
import com.mileway.feature.profile.repository.AdvanceRepository
import com.mileway.feature.profile.repository.ConnectedAccountsRepository
import com.mileway.feature.profile.repository.DelegationRepository
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.repository.NotificationRepository
import com.mileway.feature.profile.repository.PassportDetailsRepository
import com.mileway.feature.profile.repository.ProfileRepository
import com.mileway.feature.profile.repository.SupportTicketRepository
import com.mileway.feature.profile.repository.SyncDiagnosticsRepository
import com.mileway.feature.profile.repository.VehicleDetailsRepository
import com.mileway.feature.profile.viewmodel.ActiveSessionsViewModel
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import com.mileway.feature.profile.viewmodel.ChangePasswordViewModel
import com.mileway.feature.profile.viewmodel.ConnectedAccountsViewModel
import com.mileway.feature.profile.viewmodel.DelegationViewModel
import com.mileway.feature.profile.viewmodel.DemoSettingsViewModel
import com.mileway.feature.profile.viewmodel.NotificationViewModel
import com.mileway.feature.profile.viewmodel.PersonalDetailsViewModel
import com.mileway.feature.profile.viewmodel.PhoneChangeViewModel
import com.mileway.feature.profile.viewmodel.PluginManagerViewModel
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import com.mileway.feature.profile.viewmodel.StorageViewModel
import com.mileway.feature.profile.viewmodel.SupportTicketViewModel
import com.mileway.feature.profile.viewmodel.SwitchAccountViewModel
import com.mileway.feature.profile.viewmodel.SyncDiagnosticsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule =
    module {
        single { MockAccountRepository(get()) }
        single<ProfileRepository> { FakeProfileRepository(get()) }
        single { AdvanceRepository() }
        // P6.2: Vehicle/Passport tiles' Room-backed repositories.
        single { VehicleDetailsRepository(get()) }
        single { PassportDetailsRepository(get()) }
        // P6.3: approval-delegation Room-backed repository (see DelegationScreen).
        single { DelegationRepository(get()) }
        // P6.4: Active Sessions' Room-backed repository (see ActiveSessionsScreen).
        single { ActiveSessionsRepository(get()) }
        // P6.5: Notification Centre's Room-backed repository (see NotificationCentreScreen).
        single { NotificationRepository(get()) }
        // P6.6: Connected Accounts' Room-backed repository (see ConnectedAccountsScreen).
        single { ConnectedAccountsRepository(get()) }
        // P6.7: Settings' Sync Diagnostics card — in-memory local counter (see SyncDiagnosticsCard).
        single { SyncDiagnosticsRepository() }
        // P6.8: Help & Support's "Contact Support"/"My Tickets" Room-backed repository.
        single { SupportTicketRepository(get()) }
        viewModelOf(::ProfileViewModel)
        viewModel { AdvanceViewModel(get()) }
        viewModelOf(::DemoSettingsViewModel)
        viewModelOf(::SwitchAccountViewModel)
        viewModelOf(::PersonalDetailsViewModel)
        viewModelOf(::DelegationViewModel)
        viewModelOf(::ActiveSessionsViewModel)
        viewModelOf(::NotificationViewModel)
        viewModelOf(::ConnectedAccountsViewModel)
        // P6.6: Preferences' Storage tile/sheet (real cache-size readout + clear-cache action).
        viewModelOf(::StorageViewModel)
        viewModelOf(::SyncDiagnosticsViewModel)
        viewModelOf(::SupportTicketViewModel)
        // PLAN_V24 P0.3: the Master Plugin page (PluginRegistry + PersonaPresetProvider injected).
        viewModelOf(::PluginManagerViewModel)
        // PLAN_V24 P1.5: change-password sheet (CredentialSource-backed).
        viewModelOf(::ChangePasswordViewModel)
        // PLAN_V24 P3.1: phone change with OTP re-verify.
        viewModelOf(::PhoneChangeViewModel)
    }
