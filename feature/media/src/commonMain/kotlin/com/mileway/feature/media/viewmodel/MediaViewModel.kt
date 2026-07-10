package com.mileway.feature.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.media.model.UploadState
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.model.AttachmentSource
import com.mileway.feature.media.model.FlashMode
import com.mileway.feature.media.repository.MediaLibraryRepository
import com.mileway.feature.media.repository.MediaRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Single immutable UI state for the whole media-capture flow. Shared across the
 * selection -> camera/gallery -> preview -> OCR-sheet journey via a nav-graph
 * scoped ViewModel.
 */
data class MediaUiState(
    val attachments: List<AttachmentItem> = emptyList(),
    val selectedSource: AttachmentSource? = null,
    val pendingItem: AttachmentItem? = null,
    val pendingBatch: List<AttachmentItem> = emptyList(),
    val flashMode: FlashMode = FlashMode.AUTO,
    val ocrSheetVisible: Boolean = false,
    val isProcessing: Boolean = false,
)

sealed interface MediaAction {
    data class SelectSource(val source: AttachmentSource) : MediaAction

    data object CycleFlashMode : MediaAction

    data class Captured(val uri: String) : MediaAction

    data class PickedFromGallery(val uri: String) : MediaAction

    data class RemoveFromBatch(val id: String) : MediaAction

    data object RunOcr : MediaAction

    data object ConfirmPending : MediaAction

    data object Retake : MediaAction

    data object DismissSheet : MediaAction
}

/** No one-shot effects; OCR-sheet visibility is state. Present to satisfy the MVI contract. */
sealed interface MediaEffect

class MediaViewModel(
    private val repository: MediaRepository,
    private val libraryRepository: MediaLibraryRepository,
) : BaseViewModel<MediaUiState, MediaEffect, MediaAction>(MediaUiState()) {
    /** Backwards-compatible alias; screens read [state]. */
    val uiState: StateFlow<MediaUiState> = state

    override fun onAction(action: MediaAction) {
        when (action) {
            is MediaAction.SelectSource -> setState { copy(selectedSource = action.source) }
            MediaAction.CycleFlashMode -> cycleFlashMode()
            is MediaAction.Captured -> onCaptured(action.uri)
            is MediaAction.PickedFromGallery -> onPickedFromGallery(action.uri)
            is MediaAction.RemoveFromBatch -> removeFromBatch(action.id)
            MediaAction.RunOcr -> runOcr()
            MediaAction.ConfirmPending -> confirmPending()
            MediaAction.Retake ->
                setState { copy(pendingItem = null, pendingBatch = emptyList(), ocrSheetVisible = false) }
            MediaAction.DismissSheet -> setState { copy(ocrSheetVisible = false) }
        }
    }

    private fun newId(): String = Clock.System.now().toEpochMilliseconds().toString(36) + "_" + Random.nextLong().toString(36)

    /** Cycle the camera flash mode AUTO -> ON -> OFF -> AUTO. */
    private fun cycleFlashMode() {
        setState {
            val next =
                when (flashMode) {
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.OFF
                    FlashMode.OFF -> FlashMode.AUTO
                }
            copy(flashMode = next)
        }
    }

    private fun onCaptured(uri: String) {
        val item =
            AttachmentItem(
                id = newId(),
                uri = uri,
                source = AttachmentSource.CAMERA,
                capturedAtMillis = Clock.System.now().toEpochMilliseconds(),
            )
        setState { copy(pendingItem = item, pendingBatch = pendingBatch + item, ocrSheetVisible = false) }
    }

    private fun onPickedFromGallery(uri: String) {
        setState {
            val item =
                AttachmentItem(
                    id = newId(),
                    uri = uri,
                    source = selectedSource ?: AttachmentSource.GALLERY,
                    capturedAtMillis = Clock.System.now().toEpochMilliseconds(),
                )
            copy(pendingItem = item, pendingBatch = pendingBatch + item, ocrSheetVisible = false)
        }
    }

    private fun removeFromBatch(id: String) {
        setState {
            val batch = pendingBatch.filterNot { it.id == id }
            copy(
                pendingBatch = batch,
                pendingItem =
                    when {
                        batch.isEmpty() -> null
                        pendingItem?.id == id -> batch.last()
                        else -> pendingItem
                    },
                ocrSheetVisible = ocrSheetVisible && batch.isNotEmpty(),
            )
        }
    }

    /** Run the (mocked) OCR pass over the pending item and surface the result sheet. */
    private fun runOcr() {
        val pending = currentState.pendingItem ?: return
        setState { copy(isProcessing = true) }
        viewModelScope.launch {
            val result = repository.runOcr(pending.uri)
            setState {
                val updated = pendingItem?.copy(ocr = result)
                copy(
                    pendingItem = updated,
                    pendingBatch = pendingBatch.map { if (updated != null && it.id == updated.id) updated else it },
                    ocrSheetVisible = true,
                    isProcessing = false,
                )
            }
        }
    }

    /**
     * Confirm the pending capture(s): run the mocked upload over every item in the
     * multi-capture batch, persist them to the library, then clear the pending slot.
     */
    private fun confirmPending() {
        val s = currentState
        val toUpload = s.pendingBatch.ifEmpty { listOfNotNull(s.pendingItem) }
        if (toUpload.isEmpty()) return
        setState { copy(isProcessing = true) }
        viewModelScope.launch {
            val finished =
                toUpload.map { item ->
                    val uploading = item.copy(uploadState = UploadState.Uploading)
                    val done: UploadState = repository.upload(uploading)
                    uploading.copy(uploadState = done)
                }
            finished.forEach { libraryRepository.save(it) }
            setState {
                copy(
                    attachments = attachments + finished,
                    pendingItem = null,
                    pendingBatch = emptyList(),
                    ocrSheetVisible = false,
                    selectedSource = null,
                    isProcessing = false,
                )
            }
        }
    }
}
