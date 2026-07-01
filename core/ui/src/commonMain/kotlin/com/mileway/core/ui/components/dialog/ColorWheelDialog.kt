package com.mileway.core.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

/**
 * A reusable colour picker dialog with an HSV colour wheel.
 *
 * @param selectedColor The currently selected colour
 * @param onColorSelected Callback when a colour is confirmed (provides Color and hex string)
 * @param onDismiss Callback when the dialog is dismissed
 * @param showHexcode Whether to display the hex code alongside the colour preview
 * @param title Optional title for the dialog (defaults to "Choose Color")
 */
@Composable
fun ColorWheelDialog(
    selectedColor: Color,
    onColorSelected: (Color, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showHexcode: Boolean = false,
    title: String = "Choose Color",
) {
    val controller = rememberColorPickerController()
    var currentColor by remember { mutableStateOf(selectedColor) }
    var currentHexCode by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // HSV colour picker wheel
                HsvColorPicker(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(10.dp),
                    controller = controller,
                    initialColor = selectedColor,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        currentColor = colorEnvelope.color
                        currentHexCode = colorEnvelope.hexCode
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Colour preview and hex code display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(60.dp, 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentColor)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                    )

                    if (showHexcode) {
                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text =
                                if (currentHexCode.length >= 2) {
                                    "#${currentHexCode.substring(2)}"
                                } else {
                                    currentHexCode
                                },
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            // hexCode arrives as AARRGGBB, normalise to #RRGGBB
                            val hex =
                                if (currentHexCode.length >= 8) {
                                    "#${currentHexCode.takeLast(6)}"
                                } else {
                                    "#$currentHexCode"
                                }
                            onColorSelected(currentColor, hex)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}
