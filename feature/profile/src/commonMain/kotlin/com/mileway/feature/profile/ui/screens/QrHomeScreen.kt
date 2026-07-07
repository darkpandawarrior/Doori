package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.profile.model.CorporateCard
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val QR_SIZE = 21

private fun finderBit(
    row: Int,
    col: Int,
): Boolean =
    when {
        row == 0 || row == 6 || col == 0 || col == 6 -> true
        row == 1 || row == 5 || col == 1 || col == 5 -> false
        else -> true
    }

private fun isFinderRegion(
    row: Int,
    col: Int,
): Boolean {
    val last = QR_SIZE - 1
    return (row in 0..6 && col in 0..6) ||
        (row in 0..6 && col in (last - 6)..last) ||
        (row in (last - 6)..last && col in 0..6)
}

private fun finderBitAt(
    row: Int,
    col: Int,
): Boolean {
    val last = QR_SIZE - 1
    return when {
        row in 0..6 && col in 0..6 -> finderBit(row, col)
        row in 0..6 && col in (last - 6)..last -> finderBit(row, col - (last - 6))
        row in (last - 6)..last && col in 0..6 -> finderBit(row - (last - 6), col)
        else -> false
    }
}

private fun isDataModule(
    row: Int,
    col: Int,
): Boolean {
    if (isFinderRegion(row, col)) return finderBitAt(row, col)
    // Deterministic data pattern, no Random()
    return ((row * 3 + col * 5) % 7 < 3) || ((row xor col) % 5 == 0)
}

private fun DrawScope.drawQrModule(
    row: Int,
    col: Int,
    cellPx: Float,
    color: Color,
) {
    val x = col * cellPx
    val y = row * cellPx
    val r = cellPx * 0.15f
    drawRoundRect(
        color = color,
        topLeft = Offset(x + 0.5f, y + 0.5f),
        size = Size(cellPx - 1f, cellPx - 1f),
        cornerRadius = CornerRadius(r, r),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHomeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdvanceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showRequestSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF004D40), Color(0xFF00796B))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("QR Pay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Scan to receive payments", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = { /* share: illustrative */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share QR", tint = Color.White)
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.xl))

                // QR code card
                Card(
                    modifier =
                        Modifier
                            .size(260.dp)
                            .padding(4.dp),
                    shape = DesignTokens.Shape.roundedMd,
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val darkColor = Color(0xFF1A1A1A)
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                            val cellPx = size.width / QR_SIZE
                            for (row in 0 until QR_SIZE) {
                                for (col in 0 until QR_SIZE) {
                                    if (isDataModule(row, col)) {
                                        drawQrModule(row, col, cellPx, darkColor)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(DesignTokens.Spacing.l))

                Text("Siddharth Pandalai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("EMP-4421 · Apex Corp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(DesignTokens.Spacing.xl))

                if (state.cards.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DesignTokens.Spacing.l),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Linked Cards", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${state.cards.size} cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(DesignTokens.Spacing.m))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.l),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                        ) {
                            items(state.cards, key = { it.id }) { card ->
                                QrCardChip(card = card)
                            }
                        }
                    }

                    Spacer(Modifier.height(DesignTokens.Spacing.xl))
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.l),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                sheetState.show()
                                showRequestSheet = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.RequestPage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Request Amount")
                    }
                }

                Spacer(Modifier.height(DesignTokens.Spacing.xl))

                QrInfoRow()

                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (showRequestSheet) {
        QrRequestSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showRequestSheet = false
                }
            },
        )
    }
}

@Composable
private fun QrCardChip(card: CorporateCard) {
    val chipGradient =
        Brush.linearGradient(
            listOf(Color(0xFF1565C0).copy(alpha = 0.85f), Color(0xFF4A148C).copy(alpha = 0.85f)),
        )
    Box(
        modifier =
            Modifier
                .width(140.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(chipGradient)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "•••• ${card.lastFourDigits}",
                    style = MaterialTheme.typography.bodySmall.dataStyle(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(card.cardType.name, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun QrInfoRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        QrInfoTile(label = "Daily Limit", value = "₹50,000", modifier = Modifier.weight(1f))
        QrInfoTile(label = "Received Today", value = "₹0", modifier = Modifier.weight(1f))
        QrInfoTile(label = "This Month", value = "₹12,400", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QrInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall.dataStyle(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrRequestSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Request Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("QR will update to show the amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF00796B).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(22.dp))
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("Generate QR") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "QR Info Tile")
@Composable
private fun PreviewQrInfoTile() {
    com.mileway.core.ui.previews.PreviewSurface {
        QrInfoTile(label = "Daily Limit", value = "₹50,000")
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "QR Info Row")
@Composable
private fun PreviewQrInfoRow() {
    com.mileway.core.ui.previews.PreviewSurface {
        QrInfoRow()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "QR Card Chip")
@Composable
private fun PreviewQrCardChip() {
    com.mileway.core.ui.previews.PreviewSurface {
        QrCardChip(
            card =
                CorporateCard(
                    id = "CARD-001",
                    lastFourDigits = "4821",
                    cardType = com.mileway.feature.profile.model.CardType.VISA,
                    holderName = "Priya Sharma",
                    balanceRupees = 48000.0,
                    status = com.mileway.feature.profile.model.CardStatus.ACTIVE,
                    expiryDate = "12/26",
                    creditLimitRupees = 100000.0,
                ),
        )
    }
}
