package com.mileway.feature.logging.repository

import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.model.db.LogMilesDraftEntity
import com.mileway.feature.logging.ui.model.LocationStop
import com.mileway.feature.logging.viewmodel.LogMilesUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * P5.1: wires the already-existing [LogMilesDraftDao]/[LogMilesDraftEntity] (previously written
 * to by nothing — `LogMilesViewModel.saveDraft()` only appended an in-memory `LogMilesDraftUi`)
 * into real Room persistence, plus the [LogMilesUiState] <-> [LogMilesDraftEntity] mapper the
 * viewmodel needs to save/load a draft's full form state (not just the summary shown in history).
 */
class LogMilesDraftRepository(
    private val dao: LogMilesDraftDao,
) {
    fun allDrafts(): Flow<List<LogMilesDraftEntity>> = dao.getAllDrafts()

    suspend fun save(draft: LogMilesDraftEntity) = dao.upsertDraft(draft)

    suspend fun delete(draftId: String) = dao.deleteDraftById(draftId)

    suspend fun deleteAll() = dao.deleteAllDrafts()

    /**
     * Loads a draft's persisted form state, or `null` if not found. [LoadedLogMilesDraft.vehicleKey]
     * is returned alongside the [LogMilesUiState] so the caller can re-resolve the live
     * [com.mileway.core.data.model.network.ApprovedVehicle] (with current pricing) from its
     * currently loaded vehicle list, since only the key/display name are persisted.
     */
    suspend fun loadDraft(draftId: String): LoadedLogMilesDraft? =
        dao.getDraftById(draftId)?.let { entity -> LoadedLogMilesDraft(entity.toDraftUiState(), entity.selectedVehicleKey) }

    companion object {
        private val stopListSerializer = ListSerializer(LocationStop.serializer())
        private val stringListSerializer = ListSerializer(String.serializer())

        /** Encodes the current itinerary's stops into [LogMilesDraftEntity.locationsJson]'s stored form. */
        fun encodeStops(stops: List<LocationStop>): String = Json.encodeToString(stopListSerializer, stops)

        /** Decodes [LogMilesDraftEntity.locationsJson] back into the ordered stop list. */
        fun decodeStops(json: String): List<LocationStop> = runCatching { Json.decodeFromString(stopListSerializer, json) }.getOrDefault(emptyList())

        /** Encodes tagged-employee names into [LogMilesDraftEntity.employeesJson]'s stored form. */
        fun encodeEmployees(names: List<String>): String? = names.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(stringListSerializer, it) }

        /** Decodes [LogMilesDraftEntity.employeesJson] back into the tagged-employee list. */
        fun decodeEmployees(json: String?): List<String> =
            if (json == null) {
                emptyList()
            } else {
                runCatching { Json.decodeFromString(stringListSerializer, json) }.getOrDefault(emptyList())
            }

        /** Encodes [invoiceDateMillis] into [LogMilesDraftEntity.processorFormDataJson]'s stored form. */
        fun encodeInvoiceDate(invoiceDateMillis: Long?): String? =
            invoiceDateMillis?.let { Json.encodeToString(InvoiceDatePayload.serializer(), InvoiceDatePayload(it)) }

        /** Decodes [LogMilesDraftEntity.processorFormDataJson] back into the invoice date, if present. */
        fun decodeInvoiceDate(json: String?): Long? =
            json?.let { runCatching { Json.decodeFromString(InvoiceDatePayload.serializer(), it) }.getOrNull()?.invoiceDateMillis }

        /**
         * Serializes the fields the P5.1 Acceptance clause requires — stops, [LogMilesUiState.isRoundTrip],
         * the selected vehicle, [LogMilesUiState.journeyDateMillis]/`invoiceDateMillis`, the note and tagged
         * employees — into a [LogMilesDraftEntity] ready for [save]. [draftId]/[createdAtMillis] are
         * preserved by the caller when updating an existing draft.
         */
        fun LogMilesUiState.toDraftEntity(
            draftId: String,
            createdAtMillis: Long,
            nowMillis: Long,
        ): LogMilesDraftEntity =
            LogMilesDraftEntity(
                draftId = draftId,
                createdAt = createdAtMillis,
                updatedAt = nowMillis,
                journeyTimestamp = journeyDateMillis,
                selectedVehicleKey = selectedVehicle?.vehicleKey,
                selectedVehicleDisplayName = selectedVehicle?.vehicleName,
                isRoundTrip = isRoundTrip,
                totalDistance = distanceKm,
                originalDistance = calculatedDistanceKm,
                locationsJson = encodeStops(stops),
                odometerStateJson = null,
                pettyId = null,
                pettyTitle = null,
                tripId = null,
                tripTitle = null,
                itineraryId = null,
                itineraryName = null,
                eventId = 0L,
                isFromCard = false,
                selectedServiceId = selectedService?.id,
                selectedServiceName = selectedService?.name,
                processorFormDataJson = encodeInvoiceDate(invoiceDateMillis),
                employeesJson = encodeEmployees(taggedEmployees),
                notes = logMilesNote.ifBlank { null },
            )

        /** Rehydrates the [LogMilesUiState] fields this draft carries; distance/pricing is recomputed by the caller. */
        private fun LogMilesDraftEntity.toDraftUiState(): LogMilesUiState =
            LogMilesUiState(
                stops = decodeStops(locationsJson),
                isRoundTrip = isRoundTrip,
                calculatedDistanceKm = originalDistance,
                distanceKm = totalDistance,
                journeyDateMillis = journeyTimestamp,
                invoiceDateMillis = decodeInvoiceDate(processorFormDataJson),
                logMilesNote = notes.orEmpty(),
                taggedEmployees = decodeEmployees(employeesJson),
            )
    }
}

/** Minimal JSON payload for [LogMilesDraftEntity.processorFormDataJson] (P5.1: invoice date only, so far). */
@Serializable
private data class InvoiceDatePayload(
    val invoiceDateMillis: Long,
)

/** Result of [LogMilesDraftRepository.loadDraft]: the rehydrated form state plus the persisted vehicle key. */
data class LoadedLogMilesDraft(
    val uiState: LogMilesUiState,
    val vehicleKey: String?,
)
