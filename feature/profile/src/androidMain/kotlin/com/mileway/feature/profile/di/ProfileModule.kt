package com.mileway.feature.profile.di

import com.mileway.feature.profile.repository.ActiveSessionsRepository
import com.mileway.feature.profile.repository.AdvanceRepository
import com.mileway.feature.profile.repository.ConnectedAccountsRepository
import com.mileway.feature.profile.repository.CouponsRepository
import com.mileway.feature.profile.repository.DelegationRepository
import com.mileway.feature.profile.repository.DocumentRepository
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.repository.NotificationRepository
import com.mileway.feature.profile.repository.PassportDetailsRepository
import com.mileway.feature.profile.repository.ProfileRepository
import com.mileway.feature.profile.repository.ReferralProgramRepository
import com.mileway.feature.profile.repository.RewardsRepository
import com.mileway.feature.profile.repository.SavedPlacesRepository
import com.mileway.feature.profile.repository.SupportTicketRepository
import com.mileway.feature.profile.repository.SyncDiagnosticsRepository
import com.mileway.feature.profile.repository.VehicleDetailsRepository
import com.mileway.feature.profile.repository.WalletRepository
import com.mileway.feature.profile.viewmodel.AccountDeletionViewModel
import com.mileway.feature.profile.viewmodel.ActiveSessionsViewModel
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import com.mileway.feature.profile.viewmodel.ChangePasswordViewModel
import com.mileway.feature.profile.viewmodel.ConnectedAccountsViewModel
import com.mileway.feature.profile.viewmodel.CorporateVerificationViewModel
import com.mileway.feature.profile.viewmodel.CouponsViewModel
import com.mileway.feature.profile.viewmodel.DelegateSessionViewModel
import com.mileway.feature.profile.viewmodel.DelegationViewModel
import com.mileway.feature.profile.viewmodel.DemoSettingsViewModel
import com.mileway.feature.profile.viewmodel.EmailVerificationViewModel
import com.mileway.feature.profile.viewmodel.EmergencyContactsViewModel
import com.mileway.feature.profile.viewmodel.IncentiveViewModel
import com.mileway.feature.profile.viewmodel.MarketingHubViewModel
import com.mileway.feature.profile.viewmodel.MembershipViewModel
import com.mileway.feature.profile.viewmodel.NotificationViewModel
import com.mileway.feature.profile.viewmodel.PersonalDetailsViewModel
import com.mileway.feature.profile.viewmodel.PhoneChangeViewModel
import com.mileway.feature.profile.viewmodel.PluginManagerViewModel
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import com.mileway.feature.profile.viewmodel.ReferralHubViewModel
import com.mileway.feature.profile.viewmodel.RewardsViewModel
import com.mileway.feature.profile.viewmodel.SavedPlacesViewModel
import com.mileway.feature.profile.viewmodel.StorageViewModel
import com.mileway.feature.profile.viewmodel.SubscriptionViewModel
import com.mileway.feature.profile.viewmodel.SupportTicketViewModel
import com.mileway.feature.profile.viewmodel.SwitchAccountViewModel
import com.mileway.feature.profile.viewmodel.SyncDiagnosticsViewModel
import com.mileway.feature.profile.viewmodel.VerificationCentreViewModel
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
        // PLAN_V24 P8.1: external payment wallets (Room-backed) linked via offline OTP.
        single { WalletRepository(get()) }
        // P6.7: Settings' Sync Diagnostics card — in-memory local counter (see SyncDiagnosticsCard).
        single { SyncDiagnosticsRepository() }
        // P6.8: Help & Support's "Contact Support"/"My Tickets" Room-backed repository.
        single { SupportTicketRepository(get()) }
        // PLAN_V24 P3.4: saved places (home/work/other) Room-backed repository.
        single { SavedPlacesRepository(get()) }
        // PLAN_V24 P4.1: verification-centre documents Room-backed repository (seeded by P4.2's VM).
        single { DocumentRepository(get()) }
        // PLAN_V24 P5.1: referral ledger (DAO + SimulatedReviewEngine for pending→success/failed).
        single { ReferralProgramRepository(get(), get()) }
        // PLAN_V24 P5.2: coupons Room-backed repository.
        single { CouponsRepository(get()) }
        // PLAN_V24 P5.3: scratch-card rewards Room-backed repository.
        single { RewardsRepository(get()) }
        viewModelOf(::ProfileViewModel)
        viewModel { AdvanceViewModel(get()) }
        viewModelOf(::DemoSettingsViewModel)
        viewModelOf(::SwitchAccountViewModel)
        viewModelOf(::PersonalDetailsViewModel)
        viewModelOf(::DelegationViewModel)
        // PLAN_V24 P7.3: session-delegation (act on behalf) — separate from approval-delegation above.
        viewModelOf(::DelegateSessionViewModel)
        viewModelOf(::ActiveSessionsViewModel)
        viewModelOf(::NotificationViewModel)
        viewModelOf(::ConnectedAccountsViewModel)
        // P6.6: Preferences' Storage tile/sheet (real cache-size readout + clear-cache action).
        viewModelOf(::StorageViewModel)
        // P10.2: now takes PluginRegistry (persisted sync defaults) + CurrentTrackDataSource
        // (current-journey override) — both are Koin singles, so viewModelOf resolves them too, but
        // spelled out for clarity alongside the new constructor shape.
        viewModel { SyncDiagnosticsViewModel(get(), get(), get()) }
        viewModelOf(::SupportTicketViewModel)
        // PLAN_V24 P0.3: the Master Plugin page (PluginRegistry + PersonaPresetProvider injected).
        viewModelOf(::PluginManagerViewModel)
        // PLAN_V24 P1.5: change-password sheet (CredentialSource-backed).
        viewModelOf(::ChangePasswordViewModel)
        // PLAN_V24 P3.1: phone change with OTP re-verify.
        viewModelOf(::PhoneChangeViewModel)
        // PLAN_V24 P3.2: email verification status.
        viewModelOf(::EmailVerificationViewModel)
        // PLAN_V24 P3.4: saved places (home/work/other).
        viewModelOf(::SavedPlacesViewModel)
        // PLAN_V24 P3.5: emergency contacts (repository provided by core:data — shared with tracking SOS).
        viewModelOf(::EmergencyContactsViewModel)
        // PLAN_V24 P4.2: verification centre (DocumentRepository + SimulatedReviewEngine).
        viewModelOf(::VerificationCentreViewModel)
        // PLAN_V24 P4.4: corporate email verification (SessionRepository + LocalOtpEngine).
        viewModelOf(::CorporateVerificationViewModel)
        // PLAN_V24 P5.1: referral hub (ReferralProgramRepository + ReferralManager).
        viewModelOf(::ReferralHubViewModel)
        // PLAN_V24 P5.2: coupons (explicit so the Clock default is honored; NotificationDao from graph).
        viewModel { CouponsViewModel(get(), get()) }
        // PLAN_V24 P5.3: scratch-card rewards.
        viewModelOf(::RewardsViewModel)
        // PLAN_V24 P5.4: campaign-marketing hub (CampaignRepository from core:data).
        viewModelOf(::MarketingHubViewModel)
        // PLAN_V24 P6.1: Mileway Club membership (session-backed).
        viewModelOf(::MembershipViewModel)
        // PLAN_V24 P6.2: subscription plans + active subscription (explicit so the Clock default holds).
        viewModel { SubscriptionViewModel(get()) }
        // PLAN_V24 P6.3: incentive programs (live progress from the shared core:data SavedTrackDao).
        viewModelOf(::IncentiveViewModel)
        // PLAN_V24 P7.1: account-deletion lifecycle (DeletionRequestRepository + persona wipe + sign-out).
        viewModelOf(::AccountDeletionViewModel)
    }
