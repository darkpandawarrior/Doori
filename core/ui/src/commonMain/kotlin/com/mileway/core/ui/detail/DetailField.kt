package com.mileway.core.ui.detail

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.mileway.core.ui.components.StatusTone
import com.siddharth.kmp.common.UiText

/**
 * The row/field type set every real details screen in the app needs (P28.DETAIL.1 — proved against
 * TrackDetailScreen, today's only details screen). [id] is stable and is what [DetailConfig] targets
 * to hide/reorder/relabel a field from a server/tenant config; [visible] is the caller's own
 * data-driven condition (e.g. "only show the amount row when reimbursableAmount > 0"), evaluated
 * once when the spec is built — [DetailScreen] never re-derives it.
 *
 * [Slot] is the escape hatch for anything the other nine cases can't express (a map/route preview,
 * a bespoke gradient hero card) — see `sampleTrackDetailSpec` for how a real screen uses it.
 */
sealed interface DetailField {
    val id: String
    val visible: Boolean

    /** A single labelled value, e.g. "Vehicle — Four Wheeler Petrol". */
    data class KeyValue(
        override val id: String,
        val label: UiText,
        val value: UiText,
        override val visible: Boolean = true,
    ) : DetailField

    /** A grid of [metrics], [columns] wide (2, matching every metrics grid shipped so far). */
    data class MetricGrid(
        override val id: String,
        val metrics: List<Metric>,
        val columns: Int = 2,
        override val visible: Boolean = true,
    ) : DetailField {
        data class Metric(
            val label: UiText,
            val value: UiText,
            val icon: ImageVector? = null,
            /** Optional status tint for the value/icon — e.g. an over-budget metric rendered in [StatusTone.Danger]. */
            val tone: StatusTone? = null,
        )
    }

    /** A tinted status pill, optionally preceded by a label — renders via [StatusTone]. */
    data class Status(
        override val id: String,
        val value: UiText,
        val tone: StatusTone,
        val label: UiText? = null,
        override val visible: Boolean = true,
    ) : DetailField

    /**
     * A currency/amount row. [value] is pre-formatted by the caller (e.g. "₹640") — formatting
     * stays with the display-model layer, same as `TrackDisplayData.getFormattedDistance()`;
     * this field only owns the emphasised presentation.
     */
    data class Amount(
        override val id: String,
        val label: UiText,
        val value: UiText,
        override val visible: Boolean = true,
    ) : DetailField

    /** A date row. [value] is pre-formatted by the caller, same reasoning as [Amount]. */
    data class DateField(
        override val id: String,
        val label: UiText,
        val value: UiText,
        override val visible: Boolean = true,
    ) : DetailField

    /**
     * A horizontally-scrolling strip of attachment thumbnails (receipts, odometer proofs, ...).
     * Each [Attachment] owns its own thumbnail composable rather than an image URL — core:ui's
     * commonMain has no image-loading dependency (coil3 is androidMain-only here), so callers that
     * do have one (e.g. :feature:tracking, which has coil3 in commonMain) supply the loaded image.
     */
    data class AttachmentList(
        override val id: String,
        val attachments: List<Attachment>,
        val title: UiText? = null,
        override val visible: Boolean = true,
    ) : DetailField {
        data class Attachment(
            val label: UiText? = null,
            val contentDescription: UiText? = null,
            val thumbnail: @Composable () -> Unit,
        )
    }

    /**
     * Arbitrary composable content — the map/route preview slot, and the general escape hatch for
     * anything the other nine field types can't express (e.g. a bespoke gradient hero card).
     */
    data class Slot(
        override val id: String,
        override val visible: Boolean = true,
        val content: @Composable () -> Unit,
    ) : DetailField

    /**
     * Lazy/paged content escape hatch — like [Slot], but for content whose size can't be bounded
     * up front (e.g. a route-points list backed by paging) and so must not be built eagerly inside
     * one Card. [content] emits its own `item`/`items` calls directly into the enclosing
     * [DetailScreen]'s LazyColumn — it is only valid as a top-level field of a [DetailSectionSpec],
     * never nested inside an [Expandable] (there is no LazyListScope to emit into there).
     */
    data class PagedSlot(
        override val id: String,
        override val visible: Boolean = true,
        val content: LazyListScope.() -> Unit,
    ) : DetailField

    /** A collapsible group of nested [fields] — renders via `CollapsibleSectionCard`. */
    data class Expandable(
        override val id: String,
        val title: UiText,
        val fields: List<DetailField>,
        val initiallyExpanded: Boolean = false,
        override val visible: Boolean = true,
    ) : DetailField

    data class Divider(
        override val id: String,
        override val visible: Boolean = true,
    ) : DetailField

    /** A stack of full-width buttons — [DetailAction.enabled]/[DetailAction.visible] are per-button. */
    data class ActionRow(
        override val id: String,
        val actions: List<DetailAction>,
        override val visible: Boolean = true,
    ) : DetailField
}

/** One button inside a [DetailField.ActionRow]. */
data class DetailAction(
    val id: String,
    val label: UiText,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val visible: Boolean = true,
)
