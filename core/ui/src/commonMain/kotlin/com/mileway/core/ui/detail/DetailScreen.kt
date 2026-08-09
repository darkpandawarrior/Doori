package com.mileway.core.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.sheet.DetailInfoRow
import com.mileway.core.ui.text.text
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.dataStyle

/**
 * Renders any [DetailSpec] — the one details-screen renderer every feature routes through
 * (P28.DETAIL.1). Knows only the ten [DetailField] shapes, never a domain type: everything it draws
 * came in pre-resolved as [com.siddharth.kmp.common.UiText] or a caller-supplied composable slot.
 */
@Composable
fun DetailScreen(
    spec: DetailSpec,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(DesignTokens.Spacing.l),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        spec.sections.filter { it.visible }.forEach { section -> detailSection(section) }
    }
}

/**
 * Emits one [DetailSectionSpec] into the enclosing LazyColumn.
 *
 * A section with no [DetailField.PagedSlot] field renders as a single eager item — unchanged from
 * before this screen was a LazyColumn, and still correct for every bounded field type (a metrics
 * grid, an attachment strip). A section that does contain a [DetailField.PagedSlot] can't be one
 * eager item: that field owns lazy items of its own (e.g. a route-points list), so its sibling
 * fields are emitted as their own items around it instead of being pre-built inside one Card.
 */
private fun LazyListScope.detailSection(section: DetailSectionSpec) {
    val visibleFields = section.fields.filter { it.visible }
    if (visibleFields.isEmpty()) return

    if (visibleFields.none { it is DetailField.PagedSlot }) {
        item(key = section.id) { DetailSectionContent(section) }
        return
    }

    section.title?.let { title ->
        item(key = "${section.id}_title") {
            Text(
                title.text(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    visibleFields.forEach { field ->
        if (field is DetailField.PagedSlot) {
            field.content(this)
        } else {
            item(key = field.id) { DetailFieldContent(field) }
        }
    }
}

@Composable
private fun DetailSectionContent(section: DetailSectionSpec) {
    val visibleFields = section.fields.filter { it.visible }
    if (visibleFields.isEmpty()) return

    if (section.title != null) {
        SectionCard(title = section.title.text()) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                visibleFields.forEach { DetailFieldContent(it) }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            visibleFields.forEach { DetailFieldContent(it) }
        }
    }
}

@Composable
private fun DetailFieldContent(field: DetailField) {
    when (field) {
        is DetailField.KeyValue -> DetailInfoRow(field.label.text(), field.value.text())
        is DetailField.MetricGrid -> DetailMetricGridRow(field)
        is DetailField.Status -> DetailStatusRow(field)
        is DetailField.Amount -> DetailAmountRow(field)
        is DetailField.DateField -> DetailInfoRow(field.label.text(), field.value.text())
        is DetailField.AttachmentList -> DetailAttachmentsRow(field)
        is DetailField.Slot -> field.content()
        // ponytail: no LazyListScope reaches this eager path (detailSection routes every
        // top-level PagedSlot around it); a nested one has nothing valid to render.
        is DetailField.PagedSlot -> Unit
        is DetailField.Expandable -> DetailExpandableRow(field)
        is DetailField.Divider -> HorizontalDivider()
        is DetailField.ActionRow -> DetailActionRowContent(field)
    }
}

@Composable
private fun DetailMetricGridRow(field: DetailField.MetricGrid) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        field.metrics.chunked(field.columns).forEach { rowMetrics ->
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                rowMetrics.forEach { metric -> DetailMetricTile(metric, Modifier.weight(1f)) }
                repeat(field.columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DetailMetricTile(
    metric: DetailField.MetricGrid.Metric,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            metric.icon?.let { icon ->
                Icon(
                    icon,
                    contentDescription = null,
                    tint = metric.tone?.color ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTile),
                )
                Spacer(Modifier.height(DesignTokens.Spacing.s))
            }
            Text(
                metric.value.text(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = metric.tone?.color ?: Color.Unspecified,
            )
            Text(metric.label.text(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailStatusRow(field: DetailField.Status) {
    if (field.label != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(field.label.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusChip(field.value.text(), field.tone)
        }
    } else {
        StatusChip(field.value.text(), field.tone)
    }
}

@Composable
private fun DetailAmountRow(field: DetailField.Amount) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(field.label.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(field.value.text(), style = MaterialTheme.typography.titleMedium.dataStyle(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailAttachmentsRow(field: DetailField.AttachmentList) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        field.title?.let {
            Text(it.text(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            field.attachments.forEach { attachment ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                ) {
                    attachment.label?.let {
                        Text(it.text(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        attachment.thumbnail()
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailExpandableRow(field: DetailField.Expandable) {
    CollapsibleSectionCard(title = field.title.text(), initiallyExpanded = field.initiallyExpanded) {
        field.fields.filter { it.visible }.forEach { DetailFieldContent(it) }
    }
}

@Composable
private fun DetailActionRowContent(field: DetailField.ActionRow) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        field.actions.filter { it.visible }.forEach { action ->
            FilledTonalButton(
                onClick = action.onClick,
                enabled = action.enabled,
                shape = DesignTokens.Shape.button,
                modifier = Modifier.fillMaxWidth(),
            ) {
                action.icon?.let { icon ->
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                }
                Text(action.label.text())
            }
        }
    }
}
