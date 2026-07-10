package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.eco_co2_saved
import com.mileway.core.ui.resources.eco_distance
import com.mileway.core.ui.resources.eco_empty
import com.mileway.core.ui.resources.eco_fuel_saved
import com.mileway.core.ui.resources.eco_subtitle
import com.mileway.core.ui.resources.eco_title
import com.mileway.core.ui.resources.eco_trips
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.EcoDashboardViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P11.4: the Ecometer dashboard. Mileway-styled stat cards over totals computed from the
 * user's REAL completed trips × seeded per-vehicle-type emission/fuel factors (see
 * [com.mileway.core.data.vehicle.EcometerRepository]) — CO₂ saved, fuel cost saved, distance and
 * green-trip count vs a petrol-car baseline. Reached from a plugin-gated (`ecometerEnabled`)
 * profile-hub tile. Zero trips render an honest empty state, not fake numbers.
 */
@Composable
fun EcoDashboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EcoDashboardViewModel = koinViewModel(),
) {
    val totals by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        stringResource(Res.string.eco_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(Res.string.eco_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                EcoStatCard(
                    icon = Icons.Filled.Co2,
                    accent = Color(0xFF16A34A),
                    label = stringResource(Res.string.eco_co2_saved),
                    value = "${round1(totals.co2SavedKg)} kg",
                )
                EcoStatCard(
                    icon = Icons.Filled.LocalGasStation,
                    accent = Color(0xFFEA580C),
                    label = stringResource(Res.string.eco_fuel_saved),
                    value = "₹${round0(totals.fuelSavedInr)}",
                )
                EcoStatCard(
                    icon = Icons.Filled.Route,
                    accent = Color(0xFF2563EB),
                    label = stringResource(Res.string.eco_distance),
                    value = "${round1(totals.distanceKm)} km",
                )
                EcoStatCard(
                    icon = Icons.Filled.Spa,
                    accent = Color(0xFF0F766E),
                    label = stringResource(Res.string.eco_trips),
                    value = totals.trips.toString(),
                )

                if (totals.trips == 0) {
                    Text(
                        stringResource(Res.string.eco_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EcoStatCard(
    icon: ImageVector,
    accent: Color,
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun round1(v: Double): Double = (v * 10).toLong() / 10.0

private fun round0(v: Double): Long = (v + 0.5).toLong()
