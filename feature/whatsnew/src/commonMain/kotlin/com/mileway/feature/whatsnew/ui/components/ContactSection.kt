package com.mileway.feature.whatsnew.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whatsnew_detail_contact_body
import com.mileway.core.ui.resources.whatsnew_detail_contact_button
import com.mileway.core.ui.resources.whatsnew_detail_contact_email
import com.mileway.core.ui.resources.whatsnew_detail_contact_title
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V36 P4 — spec §5.2's contact block, rendered only when `WhatsNewEntry.contactEmail` is
 * non-null: divider → "Need help?" → explainer → a filled-tonal "Get in touch" button, plus an
 * underlined "or email <address>" fallback — both invoke [onContact] (the screen wires it to
 * `UrlOpener.open("mailto:…")`).
 */
@Composable
fun ContactSection(
    email: String,
    onContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        HorizontalDivider()
        Text(text = stringResource(Res.string.whatsnew_detail_contact_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(Res.string.whatsnew_detail_contact_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            shape = DesignTokens.Shape.button,
            onClick = onContact,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.whatsnew_detail_contact_button))
        }
        Text(
            text = stringResource(Res.string.whatsnew_detail_contact_email, email),
            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onContact),
        )
    }
}
