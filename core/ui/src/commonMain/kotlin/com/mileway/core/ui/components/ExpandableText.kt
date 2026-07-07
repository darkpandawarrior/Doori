package com.mileway.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_show_less
import com.mileway.core.ui.resources.core_show_more
import org.jetbrains.compose.resources.stringResource

/**
 * Text that never hides content behind an ellipsis. It renders collapsed to [collapsedMaxLines];
 * if (and only if) the text actually overflows that cap, a "More"/"Less" toggle is shown so the
 * full value is always reachable — the product-wide no-truncation rule (PLAN_V22 R3).
 *
 * Use this instead of `Text(maxLines = N, overflow = TextOverflow.Ellipsis)` for any free-text the
 * user might need to read in full (descriptions, notes, addresses, reasons, comments).
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3,
    style: TextStyle = LocalTextStyle.current,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // Only surface the toggle when the collapsed render truly clipped something.
                if (!expanded) hasOverflow = result.hasVisualOverflow
            },
        )
        if (hasOverflow || expanded) {
            Text(
                text = stringResource(if (expanded) Res.string.core_show_less else Res.string.core_show_more),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
    }
}
