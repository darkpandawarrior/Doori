package com.mileway.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * The one button hierarchy for Mileway. Every screen and shared sheet/dialog reaches for one of
 * these four instead of a raw Material3 `Button`/`OutlinedButton`/`TextButton` with ad-hoc
 * `ButtonDefaults.buttonColors(containerColor = ...)`, so colour and weight mean the same thing
 * everywhere a user is asked to act:
 *
 * - [MilewayPrimaryButton]     — ONE per surface: the thing the user came here to do. Filled,
 *   full-emphasis brand colour.
 * - [MilewaySecondaryButton]   — a supporting action, visibly lighter than primary. Outlined only,
 *   never filled — that outline-vs-fill gap IS the hierarchy signal.
 * - [MilewayDestructiveButton] — reserved for actions that lose data: delete, discard, revoke,
 *   sign out everywhere, reset. Filled in the theme's error colour. **Nothing else may use this
 *   colour.** If the action doesn't destroy something the user can't get back, it is not this
 *   button — reach for [MilewayPrimaryButton] instead, even when the situation being described is
 *   alarming (a failed reading, a policy violation). The button's colour answers "does tapping
 *   this lose data?", not "is the news bad?".
 * - [MilewayTextButton]        — tertiary escape: Cancel, Later, Close, Ignore. No container, no
 *   border; the lowest-emphasis affordance on the surface.
 *
 * All four enforce the 48dp a11y touch-target floor and the shared [DesignTokens.Shape.button]
 * corner radius, so switching between them never changes shape or reachability, only emphasis.
 */
@Composable
fun MilewayPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DesignTokens.IconSize.minTouchTarget),
        shape = DesignTokens.Shape.button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(DesignTokens.Spacing.s))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MilewaySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DesignTokens.IconSize.minTouchTarget),
        shape = DesignTokens.Shape.button,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        // Medium, not Bold: a filled primary button typically sits beside this one, and it already
        // carries the visual weight via its fill — a bold label here would read as near-equal
        // weight against it, undoing the outline-vs-fill hierarchy signal.
        Text(text, fontWeight = FontWeight.Medium)
    }
}

/**
 * Reserved for actions that destroy data. See the file-level doc for the rule: colour answers
 * "does this lose data?", never "is this situation bad?".
 */
@Composable
fun MilewayDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DesignTokens.IconSize.minTouchTarget),
        shape = DesignTokens.Shape.button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MilewayTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = DesignTokens.IconSize.minTouchTarget),
        shape = DesignTokens.Shape.button,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Text(text, fontWeight = FontWeight.Medium)
    }
}
