package com.mileway.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.report.expenseReportPolicy
import com.mileway.feature.profile.admin.RateTables

/** Summarizes the existing demo expense policy and effective device-local employer mileage rates. */
fun signupPolicySummary(
    tables: RateTables,
    atMillis: Long,
): List<String> =
    buildList {
        val expensePolicy = expenseReportPolicy().versionFor(atMillis)
        add("Expense limits (local demo policy)")
        expensePolicy.maxExpenseAmountMinor?.let {
            add("Expenses above ${formatMinorCurrency(it, expensePolicy.currency)} block submission.")
        }
        expensePolicy.receiptRequiredAboveMinor?.let {
            add("Attach a receipt above ${formatMinorCurrency(it, expensePolicy.currency)}. This check warns before submission.")
        }
        expensePolicy.perHeadLimitMinor?.let {
            add("Per-head expenses above ${formatMinorCurrency(it, expensePolicy.currency)} trigger a warning.")
        }
        add("Local mileage rates")
        val effectiveVersions = tables.mileage.policy.filter { it.policyVersion().effectiveFrom <= atMillis }
        if (effectiveVersions.isEmpty()) {
            add("No employer mileage rate is effective on this device yet.")
        } else {
            val effectivePolicy = PolicyEngine(effectiveVersions.map { it.policyVersion() }).versionFor(atMillis)
            val selected = effectiveVersions.first { it.policyVersion().effectiveFrom == effectivePolicy.effectiveFrom }
            add("Effective from ${selected.effectiveFrom} (INR).")
            selected.ratesMinorPerKm.entries.sortedBy { it.key }.forEach { (vehicle, rateMinor) ->
                add("$vehicle: ${formatMinorCurrency(rateMinor, effectivePolicy.currency)} / km")
            }
        }
        add("Mileage rates are saved on this device. Claim screens may use different rate sources.")
        add("These are local settings, not a verified employer policy. Government rate references are not employer limits.")
    }

/** The second signup step. Completion remains disabled until local policy data has loaded. */
@Composable
fun PolicySummaryScreen(
    state: OnboardingUiState,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text("Step 2 of 2", style = MaterialTheme.typography.labelLarge)
            Text("Review policy summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.policyLoading) Text("Loading local policy rates...")
            state.policySummary?.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            state.policyError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, enabled = !state.policyLoading) { Text("Retry policy load") }
            }
            state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onContinue,
                enabled = state.policySummary != null && !state.policyLoading && !state.saving && !state.done,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedMd,
            ) { Text(if (state.saving) "Saving..." else "I understand, continue") }
            TextButton(onClick = onBack, enabled = !state.saving && !state.policyLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Back to profile")
            }
        }
    }
}
