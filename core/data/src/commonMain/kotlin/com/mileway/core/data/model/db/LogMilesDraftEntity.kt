package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_miles_drafts")
data class LogMilesDraftEntity(
    @PrimaryKey
    val draftId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val journeyTimestamp: Long?,
    val selectedVehicleKey: String?,
    val selectedVehicleDisplayName: String?,
    val isRoundTrip: Boolean,
    val totalDistance: Double,
    val originalDistance: Double,
    val locationsJson: String,
    val odometerStateJson: String?,
    val pettyId: Long?,
    val pettyTitle: String?,
    val tripId: String?,
    val tripV2Id: String? = null,
    val tripTitle: String?,
    val itineraryId: String?,
    val itineraryName: String?,
    val cityId: Long? = null,
    val eventId: Long,
    val isFromCard: Boolean,
    val selectedServiceId: Long? = null,
    val selectedServiceName: String? = null,
    val selectedServiceGlCode: String? = null,
    val processorFormDataJson: String? = null,
    val expenseFormDataJson: String? = null,
    val employeesJson: String? = null,
    val notes: String? = null,
    val violationRemarks: String? = null,
    val attachmentsJson: String? = null,
    val officeId: Long? = null,
    val entityId: Long? = null,
    val mjpId: String? = null,
    val mjpItemId: Long? = null,
    val force: Long? = null,
    val provider: String? = null,
)
