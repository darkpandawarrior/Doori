package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.DesignTokens

@Composable
fun TogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    if (outlined) {
        val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        val containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
        val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        OutlinedButton(
            shape = DesignTokens.Shape.button,
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor, containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
        ) { Text(label) }
    } else {
        Button(
            shape = DesignTokens.Shape.button,
            onClick = onClick,
            modifier = Modifier.height(36.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (selected) MilewayColors.info else Color(0xFFE0E0E0),
                    contentColor = if (selected) Color.White else Color(0xFF212121),
                ),
        ) { Text(label, style = MaterialTheme.typography.bodySmall) }
    }
}
