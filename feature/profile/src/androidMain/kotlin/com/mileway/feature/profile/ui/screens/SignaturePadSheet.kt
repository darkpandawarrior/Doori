package com.mileway.feature.profile.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.io.FileOutputStream

/**
 * PLAN_V24 P12.7: the digital-signature tile in Personal Details. Shows the saved signature PNG with
 * re-sign / delete when present, else an "Add signature" affordance. Both open [SignaturePadSheet].
 */
@Composable
fun SignatureCard(
    signaturePath: String?,
    onOpenPad: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    sgRes("signature_title", "Digital signature"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = DesignTokens.Spacing.s),
                )
            }
            val bitmap = remember(signaturePath) { signaturePath?.let { BitmapFactory.decodeFile(it) } }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = sgRes("signature_saved", "Saved signature"),
                    modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = DesignTokens.Spacing.s),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    OutlinedButton(onClick = onOpenPad) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text(sgRes("signature_resign", "Re-sign"), modifier = Modifier.padding(start = 4.dp))
                    }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text(sgRes("signature_delete", "Delete"), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            } else {
                Text(
                    sgRes("signature_none", "No signature on file yet."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = DesignTokens.Spacing.s),
                )
                OutlinedButton(onClick = onOpenPad) {
                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text(sgRes("signature_add", "Add signature"), modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

/**
 * A draw-to-sign pad: a white canvas capturing pointer strokes, with undo / clear, rasterised to a
 * PNG in the app files dir on save (the path is handed back to the caller to persist).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignaturePadSheet(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val inkColor = Color(0xFF111111)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.l).padding(bottom = DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(sgRes("signature_pad_title", "Draw your signature"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> current = listOf(offset) },
                                onDrag = { change, _ -> current = current + change.position },
                                onDragEnd = {
                                    if (current.size > 1) strokes.add(current)
                                    current = emptyList()
                                },
                            )
                        },
            ) {
                (strokes + listOf(current)).forEach { pts ->
                    if (pts.size > 1) {
                        val path =
                            Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                pts.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                        drawPath(path, color = inkColor, style = Stroke(width = 5f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
                    Text(sgRes("signature_undo", "Undo"))
                }
                TextButton(onClick = { strokes.clear() }) { Text(sgRes("signature_clear", "Clear")) }
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(sgRes("signature_cancel", "Cancel")) }
                Button(
                    enabled = strokes.isNotEmpty() && canvasSize.width > 0,
                    onClick = {
                        val path = rasterizeSignature(context, strokes, canvasSize.width, canvasSize.height)
                        onSave(path)
                    },
                ) {
                    Text(sgRes("signature_save", "Save"))
                }
            }
        }
    }
}

/** Rasterises the drawn strokes to a white-background PNG in the app files dir; returns its path. */
private fun rasterizeSignature(
    context: Context,
    strokes: List<List<Offset>>,
    width: Int,
    height: Int,
): String {
    val bitmap = android.graphics.Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint =
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(0x11, 0x11, 0x11)
            strokeWidth = 5f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
    strokes.forEach { pts ->
        if (pts.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
    }
    val dir = File(context.filesDir, "signatures").apply { mkdirs() }
    val file = File(dir, "signature_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    return file.absolutePath
}

/** Screen-internal labels via the dynamic resolver with an English fallback (no generated symbols). */
@Composable
private fun sgRes(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
