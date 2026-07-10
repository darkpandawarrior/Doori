package com.mileway.feature.logging.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.common.UiText
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.db.DraftExpenseEntity
import com.mileway.core.network.model.PolicyViolation
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.core.platform.ReviewTracker
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.catalog.ExpenseCategoryCatalog
import com.mileway.feature.logging.import.ExpenseCsvImporter
import com.mileway.feature.logging.model.DraftStatus
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseDraftRow
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.validation.ExpenseFormValidator
import com.mileway.stub.PolicyMockData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** P27.E.11: max rows a bulk submit sends to the repo at once (mirrors the reference app's throttle). */
private const val BULK_SUBMIT_CONCURRENCY = 4

enum class ExpenseFilter { ALL, DRAFTS, PENDING, SETTLED }

/** Sort key for the expense history list (SHEETS.B). */
enum class ExpenseSort { DATE, AMOUNT, MERCHANT }

data class ExpenseListData(
    val records: List<ExpenseRecord> = emptyList(),
    val activeFilter: ExpenseFilter = ExpenseFilter.ALL,
    val activeSort: ExpenseSort = ExpenseSort.DATE,
    /** Category drill-down filter (SHEETS.A), empty means no category constraint. */
    val selectedCategories: Set<ExpenseCategory> = emptySet(),
)

data class ExpenseFormState(
    val step: Int = 1,
    val category: ExpenseCategory? = null,
    val amountText: String = "",
    val merchantName: String = "",
    val note: String = "",
    /** Local URI/path of an optional attached receipt photo (P1.4); null when none was attached. */
    val receiptImagePath: String? = null,
    /** P1.7: selected project/cost-center office code, only meaningful for `requiresCostCenter` categories. */
    val officeCode: String? = null,
    /**
     * P1.8: true when this form was opened via [ExpenseAction.OpenEdit] to edit/resubmit an
     * existing record, rather than a fresh Add Expense flow. Drives [submitExpense] to update the
     * existing record ([editingId]) instead of minting a new one.
     */
    val isEditing: Boolean = false,
    /** P1.8: id of the record being edited when [isEditing] is true; null in the create flow. */
    val editingId: String? = null,
    /**
     * P27.E.4: how this form was opened (trip/advance/event/card/message/scanner/edit linkage, or
     * [ExpenseSourceContext.None]/[ExpenseSourceContext.Regular] for a bare "Add Expense"). Drives
     * [openWithContext]'s prefill, [com.mileway.feature.logging.validation.ExpenseFormValidator]'s
     * card-ceiling check, and `core:ui`'s `ExpenseContextSummaryCard`.
     */
    val sourceContext: ExpenseSourceContext = ExpenseSourceContext.None,
    /** P27.E.4: Scanner-context prefill of the OCR-detected date; null uses submit-time `now()`. */
    val dateMs: Long? = null,
    val errors: Map<String, UiText> = emptyMap(),
)

data class ExpenseUiState(
    val listState: ScreenState<ExpenseListData> = ScreenState.Loading,
    val form: ExpenseFormState = ExpenseFormState(),
    val lastSubmittedId: String = "",
    val lastSubmittedAmount: Double = 0.0,
    val detailState: ScreenState<ExpenseRecord> = ScreenState.Empty,
    /** P1.5: a persisted draft exists from a previous session — offer "Resume draft" on entry. */
    val resumableDraft: DraftExpenseEntity? = null,
    /** P1.6: [PolicyMockData]'s tiered outcome for the last submitted amount (SUCCESS by default). */
    val lastSubmissionStatus: SubmissionStatus = SubmissionStatus.SUCCESS,
    /** P1.6: violations attached to the last submission, mirroring the mileage submission shape. */
    val lastSubmissionViolations: List<PolicyViolation> = emptyList(),
    /** P2.1: rows of the multi-item bulk expense entry grid. Always at least one row. */
    val rows: List<ExpenseDraftRow> = listOf(ExpenseDraftRow(id = "row-1")),
    /**
     * P2.3: outcome of the most recent [ExpenseAction.SubmitAllDrafts]/[ExpenseAction
     * .RetryFailedDrafts] batch — (successRows, errorRows) as they stood right after that batch
     * ran. Null until a batch submit has been attempted at least once this session.
     */
    val submissionSummary: Pair<List<ExpenseDraftRow>, List<ExpenseDraftRow>>? = null,
)

sealed interface ExpenseAction {
    data object Refresh : ExpenseAction

    data class SetFilter(val filter: ExpenseFilter) : ExpenseAction

    data class SetSort(val sort: ExpenseSort) : ExpenseAction

    data class SetCategories(val categories: Set<ExpenseCategory>) : ExpenseAction

    data class SelectCategory(val category: ExpenseCategory) : ExpenseAction

    data class SetAmount(val text: String) : ExpenseAction

    data class SetMerchant(val name: String) : ExpenseAction

    data class SetNote(val note: String) : ExpenseAction

    /** Attaches (or clears, when [path] is null) an optional local receipt photo (P1.4). */
    data class SetReceiptImage(val path: String?) : ExpenseAction

    /** P1.7: sets (or clears, when [code] is null) the project/cost-center office for the form. */
    data class SetOfficeCode(val code: String?) : ExpenseAction

    data object SubmitExpense : ExpenseAction

    data object ResetForm : ExpenseAction

    data class OpenDetail(val id: String) : ExpenseAction

    /**
     * P1.8: loads the existing record [id] into the form for editing (e.g. resubmitting a
     * REJECTED expense). A no-op if [id] doesn't resolve to a known record.
     */
    data class OpenEdit(val id: String) : ExpenseAction

    /**
     * P27.E.4: opens the entry form pre-filled per [context] — Trip/TripAdvance/Event/Advance link
     * a merchant label, Card locks merchant/category and caps amount, Message carries the
     * clarification attachment into [ExpenseFormState.receiptImagePath], Scanner prefills
     * merchant/amount/category/date from its OCR result, and [ExpenseSourceContext.Edit] delegates
     * to the existing [OpenEdit] load-by-id path.
     */
    data class OpenWithContext(val context: ExpenseSourceContext) : ExpenseAction

    /** P1.5: persists the current form as a draft (Room-backed, survives kill/relaunch). */
    data object SaveDraft : ExpenseAction

    /** P1.5: loads the persisted draft (if any) back into the form for editing. */
    data object ResumeDraft : ExpenseAction

    /** P1.5: dismisses the "Resume draft" offer without loading it (draft stays persisted). */
    data object DismissResumeDraft : ExpenseAction

    /** P1.5: discards the persisted draft entirely (both Room and the resumable-draft offer). */
    data object DiscardDraft : ExpenseAction

    // ── P2.1: multi-row draft grid for bulk expense entry ──────────────────────

    /** Appends a new blank row to the bulk-entry grid. */
    data object AddDraftRow : ExpenseAction

    /** Appends a copy of row [id] (same field values, fresh row id, status reset to PENDING). */
    data class DuplicateDraftRow(val id: String) : ExpenseAction

    /** Removes row [id] from the grid; a no-op when it's the grid's last remaining row. */
    data class RemoveDraftRow(val id: String) : ExpenseAction

    /** Applies [transform] to row [id] only, leaving every other row untouched. */
    data class UpdateDraftRow(val id: String, val transform: (ExpenseDraftRow) -> ExpenseDraftRow) : ExpenseAction

    // ── P2.2: carry-over defaults + apply-category-to-all for bulk rows ────────

    /** Sets [category] on every row still [DraftStatus.PENDING], leaving submitted/error rows untouched. */
    data class ApplyCategoryToAll(val category: ExpenseCategory) : ExpenseAction

    // ── P2.3: local batch submit + per-row outcome + retry-failed ──────────────

    /**
     * Validates and submits every [DraftStatus.PENDING] row as its own [ExpenseRecord]. A local,
     * fast Room/repo write per row — unlike the reference app's throttled remote fan-out, no
     * semaphore/concurrency limit is needed. Rows already [DraftStatus.SUCCESS] or [DraftStatus
     * .ERROR] are left untouched (use [RetryFailedDrafts] to resubmit error rows).
     */
    data object SubmitAllDrafts : ExpenseAction

    /** Resubmits only rows currently [DraftStatus.ERROR], leaving PENDING/SUCCESS rows untouched. */
    data object RetryFailedDrafts : ExpenseAction

    // ── P2.4: local CSV/TSV bulk-import parser (no backend) ────────────────────

    /**
     * Parses [text] (raw CSV/TSV file contents, already read from the platform file-picker by the
     * caller) via [ExpenseCsvImporter] and appends the resulting rows to the bulk-entry grid
     * alongside whatever [ExpenseUiState.rows] already has.
     */
    data class ImportCsv(val text: String) : ExpenseAction
}

sealed interface ExpenseEffect {
    data class ShowToast(val message: UiText) : ExpenseEffect

    data class NavigateToSuccess(val id: String) : ExpenseEffect

    data object NavigateBack : ExpenseEffect
}

/** Local, offline round-trip between [ExpenseFormState] and its persisted Room shape (P1.5). */
private fun ExpenseFormState.toDraftEntity(updatedAt: Long): DraftExpenseEntity =
    DraftExpenseEntity(
        categoryName = category?.name,
        amountText = amountText,
        merchantName = merchantName,
        note = note,
        receiptImagePath = receiptImagePath,
        updatedAt = updatedAt,
    )

/** Round-trips a rupee amount to its plain-text form (no trailing `.0` for whole rupees). */
private fun Double.toAmountText(): String = if (this == toLong().toDouble()) toLong().toString() else toString()

private fun DraftExpenseEntity.toFormState(): ExpenseFormState =
    ExpenseFormState(
        step = if (categoryName != null) 2 else 1,
        category = categoryName?.let { name -> ExpenseCategory.entries.find { it.name == name } },
        amountText = amountText,
        merchantName = merchantName,
        note = note,
        receiptImagePath = receiptImagePath,
    )

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    // PLAN_V24 P12.3: creating an expense is a meaningful engagement signal for the review gate.
    // Nullable-defaulted so direct-construction tests need no change; Koin supplies the real single.
    private val reviewTracker: ReviewTracker? = null,
) : BaseViewModel<ExpenseUiState, ExpenseEffect, ExpenseAction>(ExpenseUiState()) {
    init {
        refresh(ExpenseFilter.ALL, ExpenseSort.DATE, emptySet())
        viewModelScope.launch {
            val draft = repository.loadDraft()
            if (draft != null) setState { copy(resumableDraft = draft) }
        }
    }

    override fun onAction(action: ExpenseAction) {
        when (action) {
            ExpenseAction.Refresh ->
                refresh(currentState.listState.activeFilter(), currentState.listState.activeSort(), currentState.listState.activeCategories())
            is ExpenseAction.SetFilter ->
                refresh(action.filter, currentState.listState.activeSort(), currentState.listState.activeCategories())
            is ExpenseAction.SetSort ->
                refresh(currentState.listState.activeFilter(), action.sort, currentState.listState.activeCategories())
            is ExpenseAction.SetCategories ->
                refresh(currentState.listState.activeFilter(), currentState.listState.activeSort(), action.categories)
            is ExpenseAction.SelectCategory ->
                setState { copy(form = form.copy(category = action.category, step = 2)) }
            is ExpenseAction.SetAmount -> setState { copy(form = form.copy(amountText = action.text)) }
            is ExpenseAction.SetMerchant -> setState { copy(form = form.copy(merchantName = action.name)) }
            is ExpenseAction.SetNote -> setState { copy(form = form.copy(note = action.note)) }
            is ExpenseAction.SetReceiptImage -> setState { copy(form = form.copy(receiptImagePath = action.path)) }
            is ExpenseAction.SetOfficeCode -> setState { copy(form = form.copy(officeCode = action.code)) }
            ExpenseAction.SubmitExpense -> submitExpense()
            ExpenseAction.ResetForm ->
                setState {
                    copy(
                        form = ExpenseFormState(),
                        lastSubmittedId = "",
                        lastSubmittedAmount = 0.0,
                        lastSubmissionStatus = SubmissionStatus.SUCCESS,
                        lastSubmissionViolations = emptyList(),
                    )
                }
            is ExpenseAction.OpenDetail -> openDetail(action.id)
            is ExpenseAction.OpenEdit -> openEdit(action.id)
            is ExpenseAction.OpenWithContext -> openWithContext(action.context)
            ExpenseAction.SaveDraft -> saveDraft()
            ExpenseAction.ResumeDraft -> resumeDraft()
            ExpenseAction.DismissResumeDraft -> setState { copy(resumableDraft = null) }
            ExpenseAction.DiscardDraft -> discardDraft()
            ExpenseAction.AddDraftRow -> addDraftRow()
            is ExpenseAction.DuplicateDraftRow -> duplicateDraftRow(action.id)
            is ExpenseAction.RemoveDraftRow -> removeDraftRow(action.id)
            is ExpenseAction.UpdateDraftRow -> updateDraftRow(action.id, action.transform)
            is ExpenseAction.ApplyCategoryToAll -> applyCategoryToAll(action.category)
            ExpenseAction.SubmitAllDrafts -> submitAllDrafts()
            ExpenseAction.RetryFailedDrafts -> retryFailedDrafts()
            is ExpenseAction.ImportCsv -> importCsv(action.text)
        }
    }

    private fun ScreenState<ExpenseListData>.activeFilter(): ExpenseFilter = (this as? ScreenState.Content)?.data?.activeFilter ?: ExpenseFilter.ALL

    private fun ScreenState<ExpenseListData>.activeSort(): ExpenseSort = (this as? ScreenState.Content)?.data?.activeSort ?: ExpenseSort.DATE

    private fun ScreenState<ExpenseListData>.activeCategories(): Set<ExpenseCategory> = (this as? ScreenState.Content)?.data?.selectedCategories ?: emptySet()

    private fun refresh(
        filter: ExpenseFilter,
        sort: ExpenseSort,
        categories: Set<ExpenseCategory>,
    ) {
        val byStatus =
            when (filter) {
                ExpenseFilter.ALL -> repository.getAll()
                ExpenseFilter.DRAFTS -> repository.filterByStatus(ExpenseStatus.DRAFT)
                ExpenseFilter.PENDING -> repository.filterByStatus(ExpenseStatus.PENDING)
                ExpenseFilter.SETTLED -> repository.filterByStatus(ExpenseStatus.APPROVED)
            }
        val filtered = if (categories.isEmpty()) byStatus else byStatus.filter { it.category in categories }
        val records =
            when (sort) {
                ExpenseSort.DATE -> filtered.sortedByDescending { it.dateMs }
                ExpenseSort.AMOUNT -> filtered.sortedByDescending { it.amountRupees }
                ExpenseSort.MERCHANT -> filtered.sortedBy { it.merchantName.lowercase() }
            }
        setState { copy(listState = ScreenState.Content(ExpenseListData(records, filter, sort, categories))) }
    }

    private fun submitExpense() {
        val form = currentState.form
        val catalogDef = ExpenseCategoryCatalog.default().firstOrNull { it.category == form.category }
        val errors = ExpenseFormValidator.validate(form, catalogDef)
        if (errors.isNotEmpty()) {
            setState { copy(form = form.copy(errors = errors)) }
            return
        }
        val amount = form.amountText.toDoubleOrNull() ?: 0.0
        val category = form.category ?: ExpenseCategory.OTHER
        // P1.8: editing an existing record keeps its id (resubmit), instead of minting a new one.
        val id = form.editingId ?: "EXP-NEW-${(form.merchantName.hashCode() and 0x7FFF_FFFF) % 9000 + 1000}"
        val record =
            ExpenseRecord(
                id = id,
                category = category,
                merchantName = form.merchantName,
                amountRupees = amount,
                status = ExpenseStatus.PENDING,
                // P27.E.4: a Scanner-context form carries the OCR-detected date; every other source
                // stamps the actual submit time, same as before this task.
                dateMs = form.dateMs ?: kotlin.time.Clock.System.now().toEpochMilliseconds(),
                note = form.note,
                receiptImagePath = form.receiptImagePath,
                officeCode = form.officeCode,
            )
        // P1.6: same tiered policy engine as Log Miles, keyed off the expense amount.
        val submissionStatus = PolicyMockData.outcomeForExpenseAmount(amount, category.name)
        val violations = PolicyMockData.violationsForExpenseAmount(amount, category.name)
        viewModelScope.launch {
            if (form.isEditing) repository.update(record) else repository.insert(record)
            reviewTracker?.recordInteraction()
            // A submitted expense is no longer "in-flight" — clear the draft it was saved from
            // (if any), so relaunching the app doesn't re-offer a resume for an already-submitted form.
            repository.clearDraft()
            setState {
                copy(
                    lastSubmittedId = id,
                    lastSubmittedAmount = amount,
                    lastSubmissionStatus = submissionStatus,
                    lastSubmissionViolations = violations,
                    resumableDraft = null,
                )
            }
            emitEffect(ExpenseEffect.NavigateToSuccess(id))
        }
    }

    private fun openDetail(id: String) {
        val record = repository.getById(id)
        setState { copy(detailState = record?.let { ScreenState.Content(it) } ?: ScreenState.Empty) }
    }

    /** P1.8: loads [id] into the form pre-filled for editing/resubmission; a no-op if unknown. */
    private fun openEdit(id: String) {
        val record = repository.getById(id) ?: return
        setState {
            copy(
                form =
                    ExpenseFormState(
                        step = 2,
                        category = record.category,
                        amountText = record.amountRupees.toAmountText(),
                        merchantName = record.merchantName,
                        note = record.note,
                        receiptImagePath = record.receiptImagePath,
                        officeCode = record.officeCode,
                        isEditing = true,
                        editingId = record.id,
                        sourceContext = ExpenseSourceContext.Edit(record.id),
                    ),
            )
        }
    }

    /**
     * P27.E.4: opens the form pre-filled per [context]'s variant. [ExpenseSourceContext.Edit]
     * delegates to [openEdit] (it already knows how to load-by-id from [repository]); every other
     * variant sets a fresh [ExpenseFormState] whose [ExpenseFormState.sourceContext] is [context]
     * itself, since the field/id data those variants need (trip label, card ceiling, etc.) is
     * carried directly on the context (see [ExpenseSourceContext] kdoc for why — no feature:logging
     * dependency on feature:tracking/profile/events/cards/approvals repositories is added here).
     */
    private fun openWithContext(context: ExpenseSourceContext) {
        when (context) {
            is ExpenseSourceContext.Edit -> openEdit(context.expenseId)
            ExpenseSourceContext.None, ExpenseSourceContext.Regular ->
                setState { copy(form = ExpenseFormState(sourceContext = context)) }
            is ExpenseSourceContext.Trip ->
                setState { copy(form = ExpenseFormState(sourceContext = context, merchantName = context.tripLabel.orEmpty())) }
            is ExpenseSourceContext.TripAdvance ->
                setState { copy(form = ExpenseFormState(sourceContext = context, merchantName = context.tripLabel.orEmpty())) }
            is ExpenseSourceContext.Event ->
                setState { copy(form = ExpenseFormState(sourceContext = context, merchantName = context.eventLabel.orEmpty())) }
            is ExpenseSourceContext.Advance ->
                setState { copy(form = ExpenseFormState(sourceContext = context, merchantName = context.advanceLabel.orEmpty())) }
            is ExpenseSourceContext.Card ->
                setState {
                    copy(
                        form =
                            ExpenseFormState(
                                sourceContext = context,
                                merchantName = context.merchantName.orEmpty(),
                                amountText = context.transactionAmountRupees?.toAmountText().orEmpty(),
                            ),
                    )
                }
            is ExpenseSourceContext.Message ->
                setState { copy(form = ExpenseFormState(sourceContext = context, receiptImagePath = context.attachmentUrl)) }
            is ExpenseSourceContext.Scanner -> {
                val prefill = context.prefill
                setState {
                    copy(
                        form =
                            ExpenseFormState(
                                sourceContext = context,
                                merchantName = prefill.merchant.orEmpty(),
                                amountText = prefill.amountText.orEmpty(),
                                category = prefill.category?.let { name -> ExpenseCategory.entries.find { it.name == name } },
                                dateMs = prefill.dateEpochMs,
                            ),
                    )
                }
            }
        }
    }

    private fun saveDraft() {
        val form = currentState.form
        viewModelScope.launch {
            val updatedAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
            repository.saveDraft(form.toDraftEntity(updatedAt))
            emitEffect(ExpenseEffect.ShowToast(UiText.of("Draft saved")))
        }
    }

    private fun resumeDraft() {
        val draft = currentState.resumableDraft ?: return
        setState { copy(form = draft.toFormState(), resumableDraft = null) }
    }

    private fun discardDraft() {
        viewModelScope.launch {
            repository.clearDraft()
            setState { copy(resumableDraft = null) }
        }
    }

    // ── P2.1: multi-row draft grid for bulk expense entry ──────────────────────

    private var nextRowSeq = currentState.rows.size + 1

    private fun newRowId(): String {
        val id = "row-$nextRowSeq"
        nextRowSeq++
        return id
    }

    /**
     * P2.2: a new row carries over [ExpenseDraftRow.category]/[ExpenseDraftRow.merchantName] from
     * the last existing row when present — most bulk-entry batches are the same merchant/category
     * repeated (e.g. a week of daily cab receipts), so this saves re-picking both on every row.
     */
    private fun addDraftRow() {
        val last = currentState.rows.lastOrNull()
        val newRow =
            ExpenseDraftRow(
                id = newRowId(),
                category = last?.category,
                merchantName = last?.merchantName.orEmpty(),
            )
        setState { copy(rows = rows + newRow) }
    }

    private fun duplicateDraftRow(id: String) {
        val source = currentState.rows.find { it.id == id } ?: return
        val copy = source.copy(id = newRowId(), status = DraftStatus.PENDING)
        setState {
            val index = rows.indexOfFirst { it.id == id }
            copy(rows = rows.toMutableList().apply { add(index + 1, copy) })
        }
    }

    /** Removing the last remaining row is a no-op — the grid always keeps at least one row. */
    private fun removeDraftRow(id: String) {
        if (currentState.rows.size <= 1) return
        setState { copy(rows = rows.filterNot { it.id == id }) }
    }

    private fun updateDraftRow(
        id: String,
        transform: (ExpenseDraftRow) -> ExpenseDraftRow,
    ) {
        setState { copy(rows = rows.map { if (it.id == id) transform(it) else it }) }
    }

    /** Sets [category] on every row still [DraftStatus.PENDING], leaving submitted/error rows untouched. */
    private fun applyCategoryToAll(category: ExpenseCategory) {
        setState {
            copy(
                rows = rows.map { if (it.status == DraftStatus.PENDING) it.copy(category = category) else it },
            )
        }
    }

    // ── P2.3: local batch submit + per-row outcome + retry-failed ──────────────

    /** Reuses [ExpenseFormValidator]/[ExpenseCategoryCatalog] (P1.2/P1.1) by shaping [row] as a form. */
    private fun ExpenseDraftRow.toFormState(): ExpenseFormState =
        ExpenseFormState(category = category, amountText = amountText, merchantName = merchantName, note = note)

    private fun ExpenseDraftRow.toRecord(): ExpenseRecord {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val category = category ?: ExpenseCategory.OTHER
        return ExpenseRecord(
            id = "EXP-BULK-${(id.hashCode() and 0x7FFF_FFFF)}-${(merchantName.hashCode() and 0x7FFF_FFFF) % 9000 + 1000}",
            category = category,
            merchantName = merchantName,
            amountRupees = amount,
            status = ExpenseStatus.PENDING,
            dateMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            note = note,
            // P2.5: this row's own receipt attachment (if any) carries through to the resulting
            // record, mirroring the single-entry form's receiptImagePath but scoped per row.
            receiptImagePath = receiptImagePath,
        )
    }

    /** Validates+inserts a single row, returning the [DraftStatus] it landed on. Never throws. */
    private suspend fun submitOneRow(row: ExpenseDraftRow): DraftStatus {
        val catalogDef = ExpenseCategoryCatalog.default().firstOrNull { it.category == row.category }
        val errors = ExpenseFormValidator.validate(row.toFormState(), catalogDef)
        if (errors.isNotEmpty()) return DraftStatus.ERROR
        return runCatching { repository.insert(row.toRecord()) }
            .fold(onSuccess = { DraftStatus.SUCCESS }, onFailure = { DraftStatus.ERROR })
    }

    /**
     * Validates and inserts every row in [rowIds] as its own [ExpenseRecord] concurrently, bounded
     * to [BULK_SUBMIT_CONCURRENCY] in-flight rows at a time via [Semaphore] (P27.E.11 un-defers the
     * reference app's throttled remote fan-out into a local, still-bounded concurrent submit — no
     * longer the fully sequential P2.3 loop). [supervisorScope] plus a per-row `runCatching` in
     * [submitOneRow] means one row's failure never cancels its siblings — every row's outcome is
     * collected independently via [kotlinx.coroutines.awaitAll], and cooperative cancellation of the
     * whole batch (e.g. the ViewModel being cleared mid-submit) still cancels every in-flight row.
     * Updates each row's [DraftStatus] to [DraftStatus.SUCCESS]/[DraftStatus.ERROR] and publishes
     * the resulting (successRows, errorRows) as [ExpenseUiState.submissionSummary] — same shape as
     * the sequential version.
     */
    private fun submitRows(rowIds: Set<String>) {
        viewModelScope.launch {
            val snapshot = currentState.rows
            val semaphore = Semaphore(BULK_SUBMIT_CONCURRENCY)
            val statusById =
                supervisorScope {
                    rowIds
                        .map { id ->
                            async {
                                semaphore.withPermit { id to submitOneRow(snapshot.first { it.id == id }) }
                            }
                        }.awaitAll()
                }.toMap()
            val rows = currentState.rows.map { row -> statusById[row.id]?.let { row.copy(status = it) } ?: row }
            val successRows = rows.filter { it.id in rowIds && it.status == DraftStatus.SUCCESS }
            val errorRows = rows.filter { it.id in rowIds && it.status == DraftStatus.ERROR }
            setState { copy(rows = rows, submissionSummary = successRows to errorRows) }
        }
    }

    /** Submits every row still [DraftStatus.PENDING]; a no-op batch (empty summary) when none are. */
    private fun submitAllDrafts() {
        val pendingIds = currentState.rows.filter { it.status == DraftStatus.PENDING }.map { it.id }.toSet()
        if (pendingIds.isEmpty()) {
            setState { copy(submissionSummary = emptyList<ExpenseDraftRow>() to emptyList()) }
            return
        }
        submitRows(pendingIds)
    }

    /** Resubmits only rows currently [DraftStatus.ERROR]; a no-op batch when there are none. */
    private fun retryFailedDrafts() {
        val errorIds = currentState.rows.filter { it.status == DraftStatus.ERROR }.map { it.id }.toSet()
        if (errorIds.isEmpty()) {
            setState { copy(submissionSummary = emptyList<ExpenseDraftRow>() to emptyList()) }
            return
        }
        submitRows(errorIds)
    }

    // ── P2.4: local CSV/TSV bulk-import parser (no backend) ────────────────────

    /**
     * Re-ids each parsed row through [newRowId] (rather than [ExpenseCsvImporter]'s own row-local
     * ids) so imported rows never collide with ids already in [ExpenseUiState.rows], then appends
     * them to the grid.
     */
    private fun importCsv(text: String) {
        val imported = ExpenseCsvImporter.parse(text).map { it.copy(id = newRowId()) }
        if (imported.isEmpty()) return
        setState { copy(rows = rows + imported) }
    }
}
