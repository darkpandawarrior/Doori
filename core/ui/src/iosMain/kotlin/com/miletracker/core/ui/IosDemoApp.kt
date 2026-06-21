@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.tracking.CompassGauge
import com.miletracker.core.ui.components.tracking.GaugeSignal

@Composable
fun IosDemoApp() {
    val infiniteTransition = rememberInfiniteTransition(label = "compass_demo")
    val bearing by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing)),
        label = "bearing",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { DemoHeader() }
        item {
            SectionCard(
                title = "Compass: Skia Canvas",
                leadingIcon = Icons.Default.Explore,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompassGauge(
                        bearingDegrees = bearing,
                        speedKmh = 42.5f,
                        signalQuality = GaugeSignal.GOOD,
                        isActive = true,
                        diameter = 220.dp,
                    )
                }
            }
        }
        item { DemoStatsCard() }
        item { TechStackCard() }
    }
}

@Composable
private fun DemoHeader() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A1A2E), MaterialTheme.colorScheme.primary)),
                )
                .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White,
            )
            Text(
                text = "Mileway",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = "iOS · Compose Multiplatform · Skia",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DemoStatsCard() {
    SectionCard(
        title = "Today's Overview",
        leadingIcon = Icons.Default.Timeline,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatCell("14.2 km", "Distance")
            StatCell("3", "Trips")
            StatCell("42 km/h", "Avg Speed")
        }
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TechStackCard() {
    SectionCard(
        title = "Tech Stack",
        leadingIcon = Icons.Default.CheckCircle,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Kotlin Multiplatform" to "Shared business logic: Android + iOS",
                "Compose Multiplatform" to "Declarative UI: one codebase",
                "Skia" to "High-performance Canvas rendering on iOS",
                "Room KMP" to "SQLite via SQLiteConnection API",
                "DataStore KMP" to "Preferences via NSTemporaryDirectory",
                "Koin KMP" to "Dependency injection across targets",
            ).forEach { (tech, detail) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = tech,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
