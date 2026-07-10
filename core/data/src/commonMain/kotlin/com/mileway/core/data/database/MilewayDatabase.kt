package com.mileway.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.dao.BannerDismissalDao
import com.mileway.core.data.dao.CampaignDao
import com.mileway.core.data.dao.ConnectedAccountDao
import com.mileway.core.data.dao.CouponDao
import com.mileway.core.data.dao.DelegationDao
import com.mileway.core.data.dao.DeletionRequestDao
import com.mileway.core.data.dao.DestinationModeDao
import com.mileway.core.data.dao.DocumentDao
import com.mileway.core.data.dao.DraftExpenseDao
import com.mileway.core.data.dao.EmergencyContactDao
import com.mileway.core.data.dao.FavouriteRouteDao
import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.dao.LogMilesFrequentRouteDao
import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.dao.PassportDetailsDao
import com.mileway.core.data.dao.PaymentWalletDao
import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.dao.ReferralTxnDao
import com.mileway.core.data.dao.RewardCardDao
import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.dao.SessionDao
import com.mileway.core.data.dao.SignatureDao
import com.mileway.core.data.dao.SubmitDraftDao
import com.mileway.core.data.dao.SubscriptionDao
import com.mileway.core.data.dao.SupportTicketDao
import com.mileway.core.data.dao.TourProgressDao
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.dao.VehicleAuditDao
import com.mileway.core.data.dao.VehicleDao
import com.mileway.core.data.dao.VehicleDetailsDao
import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.data.model.db.ActiveSubscriptionEntity
import com.mileway.core.data.model.db.AgentConversationEntity
import com.mileway.core.data.model.db.AgentMessageEntity
import com.mileway.core.data.model.db.BannerDismissedEntity
import com.mileway.core.data.model.db.CampaignEntity
import com.mileway.core.data.model.db.ConnectedAccountEntity
import com.mileway.core.data.model.db.CouponEntity
import com.mileway.core.data.model.db.DelegationEntity
import com.mileway.core.data.model.db.DeletionRequestEntity
import com.mileway.core.data.model.db.DestinationModeEntity
import com.mileway.core.data.model.db.DocumentEntity
import com.mileway.core.data.model.db.DraftExpenseEntity
import com.mileway.core.data.model.db.EmergencyContactEntity
import com.mileway.core.data.model.db.FavouriteRouteEntity
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.LogMilesDraftEntity
import com.mileway.core.data.model.db.LogMilesFrequentRouteEntity
import com.mileway.core.data.model.db.MockAccountEntity
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.core.data.model.db.PassportDetailsEntity
import com.mileway.core.data.model.db.PaymentWalletEntity
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.model.db.ReferralTxnEntity
import com.mileway.core.data.model.db.RewardCardEntity
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.SessionEntity
import com.mileway.core.data.model.db.SignatureEntity
import com.mileway.core.data.model.db.SubmitDraftEntity
import com.mileway.core.data.model.db.SubscriptionPlanEntity
import com.mileway.core.data.model.db.SupportTicketEntity
import com.mileway.core.data.model.db.TourProgressEntity
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.db.VehicleAuditEntity
import com.mileway.core.data.model.db.VehicleDetailsEntity
import com.mileway.core.data.model.db.VehicleEntity
import com.mileway.core.data.model.db.VoucherEntity

@Database(
    entities = [
        LocationData::class,
        SavedTrack::class,
        HardwareEvent::class,
        LogMilesDraftEntity::class,
        LogMilesFrequentRouteEntity::class,
        TripAttachmentEntity::class,
        MediaLibraryEntry::class,
        SubmitDraftEntity::class,
        AgentConversationEntity::class,
        AgentMessageEntity::class,
        DraftExpenseEntity::class,
        VoucherEntity::class,
        MockAccountEntity::class,
        VehicleDetailsEntity::class,
        PassportDetailsEntity::class,
        DelegationEntity::class,
        SessionEntity::class,
        NotificationEntity::class,
        ConnectedAccountEntity::class,
        SupportTicketEntity::class,
        PluginOverrideEntity::class,
        SavedPlaceEntity::class,
        EmergencyContactEntity::class,
        DocumentEntity::class,
        ReferralTxnEntity::class,
        CouponEntity::class,
        RewardCardEntity::class,
        CampaignEntity::class,
        SubscriptionPlanEntity::class,
        ActiveSubscriptionEntity::class,
        DeletionRequestEntity::class,
        PaymentWalletEntity::class,
        VehicleEntity::class,
        DestinationModeEntity::class,
        VehicleAuditEntity::class,
        SignatureEntity::class,
        FavouriteRouteEntity::class,
        TourProgressEntity::class,
        BannerDismissedEntity::class,
    ],
    version = 38,
    exportSchema = false,
)
@ConstructedBy(MilewayDatabaseConstructor::class)
abstract class MilewayDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    abstract fun savedTrackDao(): SavedTrackDao

    abstract fun hardwareEventDao(): HardwareEventDao

    abstract fun logMilesDraftDao(): LogMilesDraftDao

    abstract fun logMilesFrequentRouteDao(): LogMilesFrequentRouteDao

    abstract fun tripAttachmentDao(): TripAttachmentDao

    abstract fun mediaLibraryDao(): MediaLibraryDao

    abstract fun submitDraftDao(): SubmitDraftDao

    abstract fun agentDao(): AgentDao

    abstract fun draftExpenseDao(): DraftExpenseDao

    abstract fun voucherDao(): VoucherDao

    abstract fun mockAccountDao(): MockAccountDao

    abstract fun vehicleDetailsDao(): VehicleDetailsDao

    abstract fun vehicleDao(): VehicleDao

    abstract fun vehicleAuditDao(): VehicleAuditDao

    abstract fun signatureDao(): SignatureDao

    abstract fun favouriteRouteDao(): FavouriteRouteDao

    abstract fun tourProgressDao(): TourProgressDao

    abstract fun passportDetailsDao(): PassportDetailsDao

    abstract fun delegationDao(): DelegationDao

    abstract fun sessionDao(): SessionDao

    abstract fun notificationDao(): NotificationDao

    abstract fun connectedAccountDao(): ConnectedAccountDao

    abstract fun supportTicketDao(): SupportTicketDao

    abstract fun pluginOverrideDao(): PluginOverrideDao

    abstract fun savedPlaceDao(): SavedPlaceDao

    abstract fun emergencyContactDao(): EmergencyContactDao

    abstract fun documentDao(): DocumentDao

    abstract fun referralTxnDao(): ReferralTxnDao

    abstract fun couponDao(): CouponDao

    abstract fun rewardCardDao(): RewardCardDao

    abstract fun campaignDao(): CampaignDao

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun deletionRequestDao(): DeletionRequestDao

    abstract fun paymentWalletDao(): PaymentWalletDao

    abstract fun destinationModeDao(): DestinationModeDao

    abstract fun bannerDismissalDao(): BannerDismissalDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MilewayDatabaseConstructor : RoomDatabaseConstructor<MilewayDatabase>
