package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_activity_analysis
import com.mileway.core.ui.resources.tracking_activity_driving_style
import com.mileway.core.ui.resources.tracking_activity_harsh_events
import com.mileway.core.ui.resources.tracking_activity_no_issues
import com.mileway.core.ui.resources.tracking_activity_speed_consistency
import com.mileway.core.ui.resources.tracking_activity_system_impact
import com.mileway.core.ui.resources.tracking_activity_time_breakdown
import com.mileway.core.ui.resources.tracking_dq_abnormal
import com.mileway.core.ui.resources.tracking_dq_cleaned_ratio
import com.mileway.core.ui.resources.tracking_dq_mock
import com.mileway.core.ui.resources.tracking_dq_reliable
import com.mileway.core.ui.resources.tracking_dq_title
import com.mileway.core.ui.resources.tracking_impact_app_killed
import com.mileway.core.ui.resources.tracking_impact_battery_optimization
import com.mileway.core.ui.resources.tracking_impact_mock_location
import com.mileway.core.ui.resources.tracking_impact_network_issues
import com.mileway.core.ui.resources.tracking_impact_phone_restart
import com.mileway.core.ui.resources.tracking_impact_poor_gps
import com.mileway.core.ui.resources.tracking_impact_power_saver
import com.mileway.core.ui.resources.tracking_quality_abnormal_points
import com.mileway.core.ui.resources.tracking_quality_analysis
import com.mileway.core.ui.resources.tracking_quality_data_completeness
import com.mileway.core.ui.resources.tracking_quality_data_quality
import com.mileway.core.ui.resources.tracking_quality_mock_locations
import com.mileway.core.ui.resources.tracking_quality_reliability
import com.mileway.core.ui.resources.tracking_quality_score_factors
import com.mileway.core.ui.resources.tracking_quality_total_points
import com.mileway.core.ui.resources.tracking_saved_reimbursable
import com.mileway.core.ui.resources.tracking_stat_avg_speed
import com.mileway.core.ui.resources.tracking_stat_distance
import com.mileway.core.ui.resources.tracking_stat_duration
import com.mileway.core.ui.resources.tracking_status_draft
import com.mileway.core.ui.resources.tracking_status_submitted
import com.mileway.core.ui.resources.tracking_track_miles_label
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.tracking.insights.ActivityResult
import com.mileway.feature.tracking.insights.DistanceQualityResult
import com.mileway.feature.tracking.insights.QualityResult
import com.mileway.feature.tracking.insights.SystemImpactResult
import com.mileway.feature.tracking.insights.SystemImpactType
import org.jetbrains.compose.resources.stringResource

@Composable
fun SavedTrackOverviewCard(
    track: TrackDisplayData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.name ?: stringResource(Res.string.tracking_track_miles_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        DateUtils.epochToDisplayDate(track.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    text = if (track.isSubmitted) stringResource(Res.string.tracking_status_submitted) else stringResource(Res.string.tracking_status_draft),
                    color = if (track.isSubmitted) MilewayColors.success else MilewayColors.warning,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatItem(
                    icon = Icons.Default.Place,
                    label = stringResource(Res.string.tracking_stat_distance),
                    value = track.getFormattedDistance(),
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                )
                StatItem(
                    icon = Icons.Default.Schedule,
                    label = stringResource(Res.string.tracking_stat_duration),
                    value = track.getFormattedDuration(),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
                StatItem(
                    icon = Icons.Default.Speed,
                    label = stringResource(Res.string.tracking_stat_avg_speed),
                    value = "${track.avgSpeedKmh.formatDecimal(1)} km/h",
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
            }

            if (track.isSubmitted && track.reimbursableAmount > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MilewayColors.success,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(stringResource(Res.string.tracking_saved_reimbursable), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "₹${track.reimbursableAmount.formatDecimal(2)}",
                        style = MaterialTheme.typography.titleMedium.dataStyle(),
                        fontWeight = FontWeight.Bold,
                        color = MilewayColors.success,
                    )
                }
            }
        }
    }
}

@Composable
fun DataQualityReportCard(
    qualityScore: Int,
    locationCount: Int,
    mockCount: Int,
    abnormalCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.tracking_quality_data_quality), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QualityDot(qualityScore)
                    Text(
                        " $qualityScore%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor(qualityScore),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            DataQualityItem(label = stringResource(Res.string.tracking_quality_total_points), value = "$locationCount")
            DataQualityItem(
                label = stringResource(Res.string.tracking_quality_mock_locations),
                value = "$mockCount",
                trend = if (mockCount == 0) TrendDirection.STABLE else TrendDirection.DOWN,
            )
            DataQualityItem(
                label = stringResource(Res.string.tracking_quality_abnormal_points),
                value = "$abnormalCount",
                trend = if (abnormalCount < 5) TrendDirection.STABLE else TrendDirection.DOWN,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Richer insight cards powered by the new analyzers
// ---------------------------------------------------------------------------

/**
 * Expanded quality card that shows the score, completeness, reliability and
 * the individual score-factor deductions surfaced by [JourneyQualityAnalyzer].
 */
@Composable
fun QualityDetailCard(
    qualityResult: QualityResult,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.tracking_quality_analysis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QualityDot(qualityResult.qualityScore)
                    Text(
                        " ${qualityResult.qualityScore}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor(qualityResult.qualityScore),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            DataQualityItem(
                label = stringResource(Res.string.tracking_quality_data_completeness),
                value = "${(qualityResult.dataCompleteness * 100).formatDecimal(0)}%",
                trend =
                    when {
                        qualityResult.dataCompleteness >= 0.8 -> TrendDirection.STABLE
                        else -> TrendDirection.DOWN
                    },
            )
            DataQualityItem(
                label = stringResource(Res.string.tracking_quality_reliability),
                value = "${qualityResult.reliabilityScore}",
                trend =
                    when {
                        qualityResult.reliabilityScore >= 75 -> TrendDirection.STABLE
                        else -> TrendDirection.DOWN
                    },
            )
            if (qualityResult.scoreFactors.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.tracking_quality_score_factors),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                qualityResult.scoreFactors.forEach { factor ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            factor.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "−${factor.deduction}",
                            style = MaterialTheme.typography.bodySmall.dataStyle(),
                            fontWeight = FontWeight.Medium,
                            color = MilewayColors.danger,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Activity classification card from [ActivityAnalyzer].
 * Shows dominant activity, speed-consistency, and acceleration profile.
 */
@Composable
fun ActivityBreakdownCard(
    activityResult: ActivityResult,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.tracking_activity_analysis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    activityResult.dominantActivity.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            DataQualityItem(
                label = stringResource(Res.string.tracking_activity_speed_consistency),
                value = "${(activityResult.speedConsistency * 100).formatDecimal(0)}%",
                trend =
                    when {
                        activityResult.speedConsistency >= 0.7 -> TrendDirection.STABLE
                        else -> TrendDirection.DOWN
                    },
            )
            DataQualityItem(
                label = stringResource(Res.string.tracking_activity_driving_style),
                value = activityResult.accelerationProfile.dominantLabel,
                trend =
                    when (activityResult.accelerationProfile.dominantLabel) {
                        "SMOOTH" -> TrendDirection.STABLE
                        "MODERATE" -> TrendDirection.STABLE
                        else -> TrendDirection.DOWN
                    },
            )
            DataQualityItem(
                label = stringResource(Res.string.tracking_activity_harsh_events),
                value = "${activityResult.accelerationProfile.harshEvents.size}",
                trend = if (activityResult.accelerationProfile.harshEvents.isEmpty()) TrendDirection.STABLE else TrendDirection.DOWN,
            )
            if (activityResult.activityBreakdown.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.tracking_activity_time_breakdown),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                activityResult.activityBreakdown.entries
                    .filter { it.value > 0.5 }
                    .sortedByDescending { it.value }
                    .forEach { (type, pct) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${pct.formatDecimal(0)}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
            }
        }
    }
}

/**
 * System-impact card from [SystemImpactAnalyzer].
 * Lists each detected impact with its estimated effect percentage.
 */
@Composable
fun SystemImpactCard(
    systemImpactResult: SystemImpactResult,
    modifier: Modifier = Modifier,
) {
    if (systemImpactResult.impacts.isEmpty() && systemImpactResult.batteryImpact == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.tracking_activity_system_impact),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (systemImpactResult.impacts.isEmpty()) {
                Text(
                    stringResource(Res.string.tracking_activity_no_issues),
                    style = MaterialTheme.typography.bodySmall,
                    color = MilewayColors.success,
                )
            } else {
                systemImpactResult.impacts.forEach { impact ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                impact.type.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                impact.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "~${impact.estimatedImpactPct.formatDecimal(0)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = impactColor(impact.estimatedImpactPct),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            systemImpactResult.batteryImpact?.recommendation?.let { rec ->
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    rec,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Distance-quality card from [DistanceQualityAnalyzer].
 */
@Composable
fun DistanceQualityCard(
    distanceQualityResult: DistanceQualityResult,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.tracking_dq_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QualityDot(distanceQualityResult.score)
                    Text(
                        " ${distanceQualityResult.score}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor(distanceQualityResult.score),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                distanceQualityResult.assessment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            DataQualityItem(
                label = stringResource(Res.string.tracking_dq_cleaned_ratio),
                value = "${(distanceQualityResult.cleanedDistanceRatio * 100).formatDecimal(0)}%",
                trend = if (distanceQualityResult.cleanedDistanceRatio >= 0.8) TrendDirection.STABLE else TrendDirection.DOWN,
            )
            DataQualityItem(
                label = stringResource(Res.string.tracking_dq_mock),
                value = "${distanceQualityResult.mockPct.formatDecimal(1)}%",
                trend = if (distanceQualityResult.mockPct < 1.0) TrendDirection.STABLE else TrendDirection.DOWN,
            )
            DataQualityItem(
                label = stringResource(Res.string.tracking_dq_abnormal),
                value = "${distanceQualityResult.abnormalPct.formatDecimal(1)}%",
                trend = if (distanceQualityResult.abnormalPct < 5.0) TrendDirection.STABLE else TrendDirection.DOWN,
            )
            if (distanceQualityResult.isReliableForBusiness) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(Res.string.tracking_dq_reliable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MilewayColors.success,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SystemImpactType.displayName(): String =
    when (this) {
        SystemImpactType.BATTERY_OPTIMIZATION -> stringResource(Res.string.tracking_impact_battery_optimization)
        SystemImpactType.POWER_SAVER -> stringResource(Res.string.tracking_impact_power_saver)
        SystemImpactType.APP_KILLED -> stringResource(Res.string.tracking_impact_app_killed)
        SystemImpactType.PHONE_RESTART -> stringResource(Res.string.tracking_impact_phone_restart)
        SystemImpactType.MOCK_LOCATION -> stringResource(Res.string.tracking_impact_mock_location)
        SystemImpactType.POOR_GPS_ACCURACY -> stringResource(Res.string.tracking_impact_poor_gps)
        SystemImpactType.NETWORK_ISSUES -> stringResource(Res.string.tracking_impact_network_issues)
    }

private fun impactColor(pct: Double): Color =
    when {
        pct >= 30.0 -> DesignTokens.StatusColors.error
        pct >= 15.0 -> DesignTokens.StatusColors.warning
        else -> DesignTokens.StatusColors.warning
    }
