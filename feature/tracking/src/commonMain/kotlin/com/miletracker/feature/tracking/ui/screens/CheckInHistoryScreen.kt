package com.miletracker.feature.tracking.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * One row in the check-in history timeline.
 *
 * A flat, UI-friendly model the integrator maps its domain events onto. Each item is a
 * single point on the journey timeline: either a geo (automatic) or a manual check-in.
 *
 * @param id             stable identity for list keys
 * @param title          primary label (vendor/centre/place name)
 * @param subtitle       optional secondary line (purpose, note, reference)
 * @param timestampMillis epoch-millis of the event; drives date grouping and time filters
 * @param lat            latitude of the check-in
 * @param lng            longitude of the check-in
 * @param type           free-form type label rendered in a chip (e.g. "Client visit")
 * @param isManual       true = manual check-in (amber), false = geo check-in (green)
 */
data class CheckInHistoryItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val timestampMillis: Long,
    val lat: Double,
    val lng: Double,
    val type: String,
    val isManual: Boolean,
)

/** Time-window filter applied to the timestamp of each [CheckInHistoryItem]. */
private enum class TimeFilter(val label: String) {
    All("All"),
    Today("Today"),
    ThisWeek("This Week"),
    ThisMonth("This Month"),
}

/**
 * Full-screen, date-grouped check-in history timeline.
 *
 * Stateless: the integrator supplies [events] and wires navigation via [onBack]. All
 * interaction state (search query, active filter, which row is expanded) is local to the
 * screen since it is purely presentational.
 *
 * Layout, top to bottom:
 * - [DepthAwareTopBar] titled "Check-In History" at [NavigationDepth.LEVEL_2] with a back arrow.
 * - A search field filtering on title/subtitle/type.
 * - A row of time-filter chips (All / Today / This Week / This Month).
 * - A [LazyColumn] grouped by friendly date header, each group rendering timeline rows: a
 *   vertical connector + colored dot, then a card with title, a type chip (amber for manual,
 *   green for geo), the coordinates line and the time. Tapping a row expands an inline detail
 *   panel (via animateContentSize) with a small Canvas map placeholder and a
 *   "Copy coordinates" tonal button.
 * - An empty state when nothing matches.
 *
 * This is a pushed full-screen flow (no bubble bottom bar), so the content owns its bottom
 * inset via [navigationBarsPadding] on the scaffold body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInHistoryScreen(
    events: List<CheckInHistoryItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var timeFilter by remember { mutableStateOf(TimeFilter.All) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    // Filter pipeline: time window first, then free-text query, newest first.
    val filtered =
        remember(events, searchQuery, timeFilter) {
            events
                .asSequence()
                .filter { matchesTimeFilter(it.timestampMillis, timeFilter) }
                .filter { matchesQuery(it, searchQuery) }
                .sortedByDescending { it.timestampMillis }
                .toList()
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = "Check-In History",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
        ) {
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
            )

            TimeFilterChips(
                selected = timeFilter,
                onSelect = { timeFilter = it },
            )

            if (filtered.isEmpty()) {
                EmptyHistoryState(hasQuery = searchQuery.isNotBlank())
            } else {
                TimelineList(
                    items = filtered,
                    expandedId = expandedId,
                    onToggleExpand = { id ->
                        expandedId = if (expandedId == id) null else id
                    },
                )
            }
        }
    }
}

/** Search field filtering the timeline on title, subtitle and type. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.Spacing.screenHorizontal,
                    vertical = DesignTokens.Spacing.s,
                ),
        placeholder = { Text("Search history") },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        singleLine = true,
        shape = DesignTokens.Shape.roundedMd,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(),
    )
}

/** Horizontally scrollable single-select time-window chips. */
@Composable
private fun TimeFilterChips(
    selected: TimeFilter,
    onSelect: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.Spacing.xs),
        contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        items(TimeFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
                shape = DesignTokens.Shape.chip,
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        }
    }
}

/** Date-grouped timeline. Each group emits a sticky-style header then its rows. */
@Composable
private fun TimelineList(
    items: List<CheckInHistoryItem>,
    expandedId: String?,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Preserve the newest-first ordering already applied to [items] when grouping.
    val grouped =
        remember(items) {
            items.groupBy { friendlyDateHeader(it.timestampMillis) }
        }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = DesignTokens.Spacing.screenHorizontal,
                end = DesignTokens.Spacing.screenHorizontal,
                top = DesignTokens.Spacing.s,
                // Pushed flow: lift the last row clear of the gesture nav bar.
                bottom = DesignTokens.Spacing.xxl,
            ),
    ) {
        grouped.forEach { (header, groupItems) ->
            item(key = "header_$header") {
                DateHeader(text = header)
            }
            itemsIndexed(
                items = groupItems,
                key = { _, item -> item.id },
            ) { index, item ->
                TimelineEventRow(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == groupItems.lastIndex,
                    expanded = expandedId == item.id,
                    onClick = { onToggleExpand(item.id) },
                )
            }
        }
    }
}

/** Friendly date group header (e.g. "Today", "Yesterday", "Mon, Jun 9"). */
@Composable
private fun DateHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier.padding(
                top = DesignTokens.Spacing.l,
                bottom = DesignTokens.Spacing.s,
            ),
    )
}

/**
 * A single timeline entry: the connector/dot rail on the left and the event card on the right.
 * The card and inline detail panel expand together via [animateContentSize].
 */
@Composable
private fun TimelineEventRow(
    item: CheckInHistoryItem,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor =
        if (item.isManual) {
            DesignTokens.StatusColors.warning
        } else {
            DesignTokens.StatusColors.success
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // Let the rail's connector line stretch to the full height of the row.
                .height(IntrinsicSize.Min),
    ) {
        TimelineRail(
            isFirst = isFirst,
            isLast = isLast,
            dotColor = dotColor,
        )
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        EventCard(
            item = item,
            dotColor = dotColor,
            expanded = expanded,
            onClick = onClick,
            modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs),
        )
    }
}

/**
 * The vertical connector rail with a colored node. The line runs full-height of the row so
 * consecutive entries appear linked; it is suppressed above the first and below the last node
 * within a date group.
 */
@Composable
private fun TimelineRail(
    isFirst: Boolean,
    isLast: Boolean,
    dotColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val connectorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Column(
        modifier =
            modifier
                .width(28.dp)
                .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Connector above the node.
        Box(modifier = Modifier.weight(1f)) {
            if (!isFirst) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(connectorColor),
                )
            }
        }

        // Node: a tinted halo around a solid colored dot.
        Box(
            modifier =
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
            )
        }

        // Connector below the node.
        Box(modifier = Modifier.weight(1f)) {
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(connectorColor),
                )
            }
        }
    }
}

/** The clickable event card: title, type chip, coordinates, time and the expandable detail panel. */
@Composable
private fun EventCard(
    item: CheckInHistoryItem,
    dotColor: androidx.compose.ui.graphics.Color,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(DesignTokens.Shape.roundedMd)
                .clickable(onClick = onClick)
                .animateContentSize(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            // Title + type chip on one line.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                TypeChip(type = item.type, isManual = item.isManual, accent = dotColor)
            }

            if (!item.subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            // Coordinates line with a small location glyph.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
                Spacer(Modifier.width(DesignTokens.Spacing.xs))
                Text(
                    text = formatCoordinates(item.lat, item.lng),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = formatTime(item.timestampMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Inline detail panel.
            if (expanded) {
                ExpandedDetails(item = item, accent = dotColor)
            }
        }
    }
}

/** Type chip: amber for manual check-ins, green for geo. */
@Composable
private fun TypeChip(
    type: String,
    isManual: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val label = type.ifBlank { if (isManual) "Manual" else "Geo" }
    Row(
        modifier =
            modifier
                .clip(DesignTokens.Shape.chip)
                .background(accent.copy(alpha = 0.14f))
                .padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (isManual) Icons.Filled.EditLocationAlt else Icons.Filled.MyLocation,
            contentDescription = if (isManual) "Manual check-in" else "Geo check-in",
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Inline expanded panel: a small Canvas map placeholder (dot grid + pin) and a tonal
 * "Copy coordinates" button that writes the formatted coordinates to the clipboard.
 */
@Composable
private fun ExpandedDetails(
    item: CheckInHistoryItem,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val coordinates = formatCoordinates(item.lat, item.lng)

    Column(modifier = modifier.padding(top = DesignTokens.Spacing.m)) {
        MapPlaceholder(
            pinColor = accent,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(DesignTokens.Shape.roundedSm),
        )

        Spacer(Modifier.height(DesignTokens.Spacing.m))

        FilledTonalButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(coordinates))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSize.inline),
            )
            Spacer(Modifier.width(DesignTokens.Spacing.s))
            Text("Copy coordinates")
        }
    }
}

/**
 * Lightweight map stand-in drawn with [Canvas]: a faint dot grid suggesting a map surface
 * with a single drop-pin marker at the centre. Purely decorative, no real map dependency.
 */
@Composable
private fun MapPlaceholder(
    pinColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val surfaceTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)

    Canvas(modifier = modifier.background(surfaceTint)) {
        // Dot grid.
        val step = 22f
        val dotRadius = 1.6f
        var y = step
        while (y < size.height) {
            var x = step
            while (x < size.width) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
        }

        // Centre pin: a teardrop body with a hollow centre dot, plus a ground shadow.
        val center = Offset(size.width / 2f, size.height / 2f)
        val pinHeight = 34f
        val pinRadius = 11f
        val tipY = center.y + pinHeight / 2f
        val bulbCenter = Offset(center.x, center.y - pinHeight / 2f + pinRadius)

        // Ground shadow ellipse under the tip.
        drawCircle(
            color = pinColor.copy(alpha = 0.18f),
            radius = 5f,
            center = Offset(center.x, tipY + 2f),
        )

        // Teardrop: circular bulb + a triangle tapering to the tip.
        drawCircle(color = pinColor, radius = pinRadius, center = bulbCenter)
        val tail =
            Path().apply {
                moveTo(bulbCenter.x - pinRadius * 0.78f, bulbCenter.y + pinRadius * 0.5f)
                lineTo(center.x, tipY)
                lineTo(bulbCenter.x + pinRadius * 0.78f, bulbCenter.y + pinRadius * 0.5f)
                close()
            }
        drawPath(path = tail, color = pinColor)

        // Hollow centre.
        drawCircle(color = surfaceTint, radius = pinRadius * 0.42f, center = bulbCenter)
    }
}

/** Centered empty state shown when no events match the current filters. */
@Composable
private fun EmptyHistoryState(
    hasQuery: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(DesignTokens.Spacing.xl),
        ) {
            Icon(
                imageVector = Icons.Filled.HistoryToggleOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Text(
                text = if (hasQuery) "No matching check-ins" else "No check-in history yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text =
                    if (hasQuery) {
                        "Try a different search or time range."
                    } else {
                        "Your geo and manual check-ins will appear here."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pure helpers
// ---------------------------------------------------------------------------

/** Returns whether [timestampMillis] falls within the window described by [filter]. */
private fun matchesTimeFilter(
    timestampMillis: Long,
    filter: TimeFilter,
): Boolean {
    if (filter == TimeFilter.All) return true
    if (timestampMillis <= 0L) return false

    val tz = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(tz).date
    val today = kotlin.time.Clock.System.now().toLocalDateTime(tz).date

    return when (filter) {
        TimeFilter.All -> true
        TimeFilter.Today -> date == today
        TimeFilter.ThisWeek -> {
            // Calendar week starting Monday
            val daysFromMonday = today.dayOfWeek.ordinal.toLong()
            val weekStart = today.minus(daysFromMonday, DateTimeUnit.DAY)
            date >= weekStart && date <= today
        }
        TimeFilter.ThisMonth ->
            date.year == today.year && date.month == today.month
    }
}

/** Case-insensitive match of [query] against the item's title, subtitle and type. */
private fun matchesQuery(
    item: CheckInHistoryItem,
    query: String,
): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val needle = trimmed.lowercase()
    return item.title.lowercase().contains(needle) ||
        item.type.lowercase().contains(needle) ||
        (item.subtitle?.lowercase()?.contains(needle) == true)
}

/** Date group header: "Today", "Yesterday", otherwise "Mon, Jun 9". */
private fun friendlyDateHeader(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Unknown"
    val tz = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(tz).date
    val today = kotlin.time.Clock.System.now().toLocalDateTime(tz).date
    return when (date) {
        today -> "Today"
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> {
            val dow = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
            val mon = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
            "$dow, $mon ${date.dayOfMonth}"
        }
    }
}

/** Coordinates formatted to four decimals, e.g. "18.5204, 73.8567". */
private fun formatCoordinates(
    lat: Double,
    lng: Double,
): String {
    val latStr = ((lat * 10_000).toLong() / 10_000.0).toString()
    val lngStr = ((lng * 10_000).toLong() / 10_000.0).toString()
    return "$latStr, $lngStr"
}

/** Event time formatted as "Jun 9, 4:30 PM". */
private fun formatTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    val tz = TimeZone.currentSystemDefault()
    val ldt = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(tz)
    val mon = ldt.month.name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
    val h24 = ldt.hour
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    val ampm = if (h24 < 12) "AM" else "PM"
    val mm = ldt.minute.toString().padStart(2, '0')
    return "$mon ${ldt.dayOfMonth}, $h12:$mm $ampm"
}
