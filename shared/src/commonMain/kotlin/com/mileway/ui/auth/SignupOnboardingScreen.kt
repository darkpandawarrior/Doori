package com.mileway.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P2.1 — the config-driven signup onboarding form (per the reference app's signup onboarding form).
 * The field set + required/optional gating come entirely from [OnboardingFormConfig] (resolved
 * from the onboarding plugins by the host). Submit persists via the ViewModel; Skip is offered
 * only when the persona allows it.
 *
 * ponytail: DOB uses a "today minus 25y" demo value on tap rather than a full date-picker dialog —
 * the config-driven validation (dob present-or-not) is what matters here; a real picker is a
 * follow-up. Referral redemption via LocalReferralManager is deferred to the referral phase (P5).
 */
@Composable
fun SignupOnboardingScreen(
    config: OnboardingFormConfig,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    demoDobMillis: Long = 0L,
    viewModel: SignupOnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(config) { viewModel.configure(config) }
    LaunchedEffect(state.done) { if (state.done) onComplete() }

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
            Text(
                text = ob("onboarding_title", "Complete your profile"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = ob("onboarding_subtitle", "A few details to get you started"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))

            Field(state.firstName, viewModel::onFirstNameChange, ob("onboarding_first_name", "First name"), state.errors.contains(OnboardingField.FIRST_NAME))
            Field(
                state.lastName,
                viewModel::onLastNameChange,
                ob("onboarding_last_name", "Last name") + optionalSuffix(config.lastNameOptional),
                state.errors.contains(OnboardingField.LAST_NAME),
            )
            Field(
                state.email,
                viewModel::onEmailChange,
                ob("onboarding_email", "Email") + optionalSuffix(config.emailOptional),
                state.errors.contains(OnboardingField.EMAIL),
            )

            GenderField(
                value = state.gender,
                onSelect = viewModel::onGenderChange,
                required = config.genderRequired,
                isError = state.errors.contains(OnboardingField.GENDER),
            )

            // DOB: a tap sets a demo date (see class doc); required only when dobRequired.
            OutlinedTextField(
                value = if (state.dateOfBirthMillis != null) ob("onboarding_dob_set", "Date of birth set") else "",
                onValueChange = {},
                readOnly = true,
                isError = state.errors.contains(OnboardingField.DOB),
                label = { Text(ob("onboarding_dob", "Date of birth") + optionalSuffix(!config.dobRequired)) },
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onDobChange(demoDobMillis) },
                shape = DesignTokens.Shape.roundedMd,
            )

            if (config.showPromo) {
                Field(state.referralCode, viewModel::onReferralChange, ob("onboarding_referral", "Referral code (optional)"), false)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedMd,
            ) { Text(ob("onboarding_submit", "Continue"), fontWeight = FontWeight.SemiBold) }

            if (config.showSkip) {
                TextButton(onClick = viewModel::skip, modifier = Modifier.fillMaxWidth()) {
                    Text(ob("onboarding_skip", "Skip for now"))
                }
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
    )
}

@Composable
private fun GenderField(
    value: String,
    onSelect: (String) -> Unit,
    required: Boolean,
    isError: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    val options = listOf("Male", "Female", "Other")
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            isError = isError,
            label = { Text(ob("onboarding_gender", "Gender") + optionalSuffix(!required)) },
            modifier = Modifier.fillMaxWidth().clickable { open = true },
            shape = DesignTokens.Shape.roundedMd,
        )
        Box(modifier = Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun optionalSuffix(optional: Boolean): String = if (optional) " " + ob("onboarding_optional", "(optional)") else ""

@Composable
private fun ob(
    key: String,
    fallback: String,
): String {
    val resource = Res.allStringResources[key] ?: return fallback
    return stringResource(resource)
}
