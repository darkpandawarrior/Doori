package com.mileway.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.network.model.DemoAccount
import com.mileway.core.ui.components.DotsIndicator
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.auth_continue_guest
import com.mileway.core.ui.resources.auth_email
import com.mileway.core.ui.resources.auth_password
import com.mileway.core.ui.resources.auth_sign_in
import com.mileway.core.ui.resources.shared_auth_demo_code
import com.mileway.core.ui.resources.shared_auth_email_error
import com.mileway.core.ui.resources.shared_auth_hide_password
import com.mileway.core.ui.resources.shared_auth_otp_sent
import com.mileway.core.ui.resources.shared_auth_password_error
import com.mileway.core.ui.resources.shared_auth_password_placeholder
import com.mileway.core.ui.resources.shared_auth_phone_error_empty
import com.mileway.core.ui.resources.shared_auth_phone_error_length
import com.mileway.core.ui.resources.shared_auth_phone_label
import com.mileway.core.ui.resources.shared_auth_send_otp
import com.mileway.core.ui.resources.shared_auth_show_password
import com.mileway.core.ui.resources.shared_auth_signing_in
import com.mileway.core.ui.resources.shared_auth_use_email
import com.mileway.core.ui.resources.shared_auth_use_phone
import com.mileway.core.ui.resources.shared_login_cd_choose_persona
import com.mileway.core.ui.resources.shared_login_demo_mode_subtitle
import com.mileway.core.ui.resources.shared_login_demo_mode_title
import com.mileway.core.ui.resources.shared_login_duplicate_create_new
import com.mileway.core.ui.resources.shared_login_duplicate_subtitle
import com.mileway.core.ui.resources.shared_login_duplicate_title
import com.mileway.core.ui.resources.shared_login_tagline
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Prefilled demo identity so the fake login works without typing anything. */
private const val DEMO_EMAIL = "demo@mileway.app"

/** App name shown beside the logo mark. */
private const val APP_NAME = "Mileway"

/** How long each onboarding slide is shown before auto-advancing. */
private const val ONBOARDING_AUTO_ADVANCE_MS = 2_600L

/** Brief pause after [MilewayAuthState.Success] so the completed checklist is visible. */
private const val POST_SUCCESS_PAUSE_MS = 250L

/**
 * A single onboarding slide: a simple drawn illustration (Material icon on a tinted disc),
 * a short title, and a supporting caption about tracking miles offline.
 */
private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val caption: String,
)

/** The three onboarding slides cycled by the welcome pager. */
private val ONBOARDING_SLIDES =
    listOf(
        OnboardingSlide(
            icon = Icons.Filled.Explore,
            title = "Track every mile",
            caption = "Start a trip and Mileway records your route, distance, and duration automatically.",
        ),
        OnboardingSlide(
            icon = Icons.Filled.CloudOff,
            title = "Works fully offline",
            caption = "No signal, no problem. Trips are saved on your device and are ready whenever you are.",
        ),
        OnboardingSlide(
            icon = Icons.Filled.LocationOn,
            title = "Your data stays put",
            caption = "Everything lives locally on your phone: capture mileage anywhere, sync nothing.",
        ),
    )

/**
 * Polished fake login screen for the demo. No real authentication happens: any non-empty
 * email and password are accepted, the prefilled [DEMO_EMAIL] hint lets a reviewer sign in
 * with one tap, and the guest button skips credentials entirely.
 *
 * Layout, top to bottom:
 * - app mark + name header,
 * - a 3-page auto-advancing onboarding [HorizontalPager] with drawn illustrations and captions,
 *   plus a shared [DotsIndicator],
 * - email + password [OutlinedTextField]s (non-empty validation only),
 * - a full-width "Sign In" button with a brief fake-loading state,
 * - a "Continue as guest" text button.
 *
 * The screen owns its insets directly: the scrolling [Column] applies [statusBarsPadding] and
 * [imePadding] so fields lift above the keyboard, and the pinned bottom actions add
 * [navigationBarsPadding]. It is hosted full-screen by the integrator (no bottom bar), so it is
 * stateless apart from internal field/loading/animation state, no ViewModel is required.
 *
 * PLAN_V22 P7.5: [WelcomeDisclaimerSheet] shows exactly once per install (gated by
 * [com.mileway.core.data.session.SessionState.hasShownWelcomeDisclaimer]), before the reviewer
 * ever signs in. It is non-blocking — dismissing it ("Not now") proceeds straight through — and
 * queues any sign-in tap made while it's showing so the tap resumes automatically once the sheet
 * (and, if "Continue" was chosen, the real system permission dialog) finishes.
 *
 * @param onSignInWithCredentials invoked with the entered email once a credentials sign-in
 *   completes. Captured via [rememberUpdatedState] so the fake-loading coroutine always calls the
 *   latest lambda.
 * @param onContinueAsGuest invoked when the user chooses the guest path. The integrator persists
 *   the session so it survives navigation, deep links and process recreation.
 * @param hasShownWelcomeDisclaimer whether [WelcomeDisclaimerSheet] has already been shown on a
 *   prior composition of this screen (persisted session state); when false the sheet shows once.
 * @param onWelcomeDisclaimerShown invoked the first time the sheet is dismissed (either path), so
 *   the integrator can persist [hasShownWelcomeDisclaimer] and never show it again this session.
 */
@Composable
fun LoginScreen(
    onSignInWithCredentials: (email: String) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
    hasShownWelcomeDisclaimer: Boolean = true,
    onWelcomeDisclaimerShown: () -> Unit = {},
    // Android-only runtime permission request, injected by the host (:app's PermissionsController).
    // Defaulted so the screen stays commonMain/previewable and iOS (no runtime prompt) is a no-op.
    onRequestPermissions: () -> Unit = {},
    // PLAN_V24 P1.1: phone-OTP login mode. Gated by the `phoneLoginEnabled` plugin — the host reads
    // the registry and passes the resolved value. Defaulted off so email/password stays the
    // baseline (and direct-construction tests/previews see today's flow unchanged).
    phoneLoginEnabled: Boolean = false,
    // P1.2: "Get OTP via call" secondary action on the OTP screen, gated by its own plugin.
    otpViaCallEnabled: Boolean = false,
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val currentOnCredentials by rememberUpdatedState(onSignInWithCredentials)
    val currentOnGuest by rememberUpdatedState(onContinueAsGuest)

    var email by remember { mutableStateOf(DEMO_EMAIL) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isGuestPath by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    // P1.1: phone-login mode state (only reachable when phoneLoginEnabled).
    var usePhoneMode by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(DEFAULT_COUNTRY_CODE) }
    var phoneError by remember { mutableStateOf<PhoneValidation?>(null) }
    val lastLoginOtp by authViewModel.lastLoginOtp.collectAsStateWithLifecycle()
    // P1.2: phone flow sub-step — false = enter phone, true = enter OTP.
    var showOtpEntry by remember { mutableStateOf(false) }
    // P1.6: personas found matching the verified phone → show the duplicate-resolution sheet.
    var duplicateAccounts by remember { mutableStateOf<List<DemoAccount>>(emptyList()) }

    // P1.6: complete a phone-OTP login as [identity], optionally as a picked persona.
    fun completePhoneLogin(
        identity: String,
        personaId: String?,
    ) {
        personaId?.let { authViewModel.selectPersona(it) }
        email = identity
        isGuestPath = false
        showOtpEntry = false
        duplicateAccounts = emptyList()
        authViewModel.beginSignIn()
    }

    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val isSigningIn = authState !is MilewayAuthState.Idle

    // P7.3: "Demo mode" persona picker. Personas come from the same Room-backed, P1.1-seeded
    // list ProfileScreen's PersonaSwitcherRow renders; picking one before signing in changes
    // which seeded account AuthViewModel.beginSignIn marks active once the sequence completes.
    val personas by authViewModel.personas.collectAsStateWithLifecycle()
    val selectedPersonaId by authViewModel.selectedPersonaId.collectAsStateWithLifecycle()
    var showPersonaPicker by remember { mutableStateOf(false) }

    // P7.5: gates WelcomeDisclaimerSheet + queues a sign-in tap made while it's showing.
    val disclaimerState =
        rememberWelcomeDisclaimerState(
            initiallyShown = hasShownWelcomeDisclaimer,
            onShown = onWelcomeDisclaimerShown,
            onResumeSignIn = { isGuest ->
                isGuestPath = isGuest
                authViewModel.beginSignIn()
            },
        )
    val emailValid = email.isNotBlank()
    val passwordValid = password.isNotBlank()
    val canSubmit = emailValid && passwordValid && !isSigningIn

    fun attemptCredentialsSignIn() {
        attemptedSubmit = true
        if (emailValid && passwordValid) disclaimerState.beginSignInOrQueue(isGuest = false)
    }

    // P7.2: staged sign-in. AuthViewModel steps MilewayAuthState through named Loading stages;
    // once it reports Success, report completion down the path the user actually took (guest vs
    // credentials) so the session is persisted with the right kind, then reset back to Idle.
    LaunchedEffect(authState) {
        if (authState is MilewayAuthState.Success) {
            delay(POST_SUCCESS_PAUSE_MS)
            if (isGuestPath) currentOnGuest() else currentOnCredentials(email)
            authViewModel.reset()
        }
    }

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl),
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.xl))

            BrandHeader(
                selectedPersona = personas.firstOrNull { it.id == selectedPersonaId },
                onOpenPersonaPicker = { showPersonaPicker = true },
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))

            OnboardingPager()

            Spacer(Modifier.height(DesignTokens.Spacing.xl))

            if (phoneLoginEnabled) {
                AuthModeToggle(
                    usePhoneMode = usePhoneMode,
                    onToggle = { usePhoneMode = it },
                    enabled = !isSigningIn,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.l))
            }

            if (phoneLoginEnabled && usePhoneMode && showOtpEntry && lastLoginOtp != null) {
                OtpVerificationScreen(
                    purpose = OtpPurpose.LOGIN,
                    target = lastLoginOtp!!.target,
                    delivery = lastLoginOtp,
                    otpViaCallEnabled = otpViaCallEnabled,
                    onVerified = {
                        // P1.6: if the verified number is registered to existing personas, resolve
                        // the duplicate before completing; otherwise complete as a new phone identity.
                        val target = lastLoginOtp!!.target
                        val dupes = authViewModel.duplicatesFor(target)
                        if (dupes.isNotEmpty()) {
                            duplicateAccounts = dupes
                            showOtpEntry = false
                        } else {
                            completePhoneLogin(identity = target, personaId = null)
                        }
                    },
                    onChangeNumber = { showOtpEntry = false },
                )
            } else if (phoneLoginEnabled && usePhoneMode) {
                PhoneLoginFields(
                    country = selectedCountry,
                    onCountryChange = { selectedCountry = it },
                    phone = phone,
                    onPhoneChange = {
                        phone = it
                        phoneError = null
                    },
                    validation = phoneError,
                    enabled = !isSigningIn,
                    delivery = lastLoginOtp,
                    onSendOtp = {
                        val result = authViewModel.sendLoginOtp(selectedCountry.dialCode, phone)
                        phoneError = result
                        if (result is PhoneValidation.Valid) showOtpEntry = true
                    },
                )
            } else {
                CredentialFields(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    emailError = attemptedSubmit && !emailValid,
                    passwordError = attemptedSubmit && !passwordValid,
                    enabled = !isSigningIn,
                    onImeDone = ::attemptCredentialsSignIn,
                )

                Spacer(Modifier.height(DesignTokens.Spacing.xl))

                Button(
                    onClick = ::attemptCredentialsSignIn,
                    enabled = canSubmit,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = DesignTokens.Shape.roundedMd,
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(DesignTokens.Spacing.m))
                        Text(stringResource(Res.string.shared_auth_signing_in), fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(stringResource(Res.string.auth_sign_in), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            AnimatedVisibility(visible = isSigningIn) {
                Column {
                    Spacer(Modifier.height(DesignTokens.Spacing.l))
                    SignInProgressOverlay(state = authState)
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            TextButton(
                onClick = {
                    if (!isSigningIn) disclaimerState.beginSignInOrQueue(isGuest = true)
                },
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.auth_continue_guest))
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))
        }
    }

    if (showPersonaPicker) {
        DemoModePersonaPickerSheet(
            personas = personas,
            selectedPersonaId = selectedPersonaId,
            onSelect = { accountId ->
                authViewModel.selectPersona(accountId)
                showPersonaPicker = false
            },
            onDismiss = { showPersonaPicker = false },
        )
    }

    if (duplicateAccounts.isNotEmpty()) {
        DuplicateAccountsSheet(
            accounts = duplicateAccounts,
            onContinueAs = { account -> completePhoneLogin(identity = account.employeeCode, personaId = account.id) },
            onCreateNew = { completePhoneLogin(identity = lastLoginOtp?.target ?: "", personaId = null) },
            onDismiss = { duplicateAccounts = emptyList() },
        )
    }

    if (disclaimerState.isShowing) {
        WelcomeDisclaimerSheet(
            onRequestPermissions = {
                onRequestPermissions()
                disclaimerState.dismiss()
            },
            onDismiss = { disclaimerState.dismiss() },
        )
    }
}

/**
 * Mileway's own stepped-progress affordance for the staged sign-in sequence: a vertical
 * checklist of [SIGN_IN_STEPS], each row showing a filled dot for pending, a spinner for the
 * currently active step, and an animated checkmark once that step completes. Models the
 * reference app's staged-overlay *pattern* (a driven multi-step sequence rather than one flat
 * spinner) with Mileway's own card-and-checklist visual language, not a copy of its UI.
 */
@Composable
private fun SignInProgressOverlay(state: MilewayAuthState) {
    val activeStep = (state as? MilewayAuthState.Loading)?.step ?: 0
    val isComplete = state is MilewayAuthState.Success

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            SIGN_IN_STEPS.forEachIndexed { index, step ->
                val stepNumber = index + 1
                val completed = isComplete || stepNumber < activeStep
                val active = !isComplete && stepNumber == activeStep
                SignInStepRow(label = step.label, completed = completed, active = active)
                if (stepNumber != SIGN_IN_STEPS.size) {
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                }
            }
        }
    }
}

/** One row of [SignInProgressOverlay]'s checklist: a leading status glyph plus the step label. */
@Composable
private fun SignInStepRow(
    label: String,
    completed: Boolean,
    active: Boolean,
) {
    val checkAlpha by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "signInStepCheck",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(DesignTokens.IconSize.badge),
            contentAlignment = Alignment.Center,
        ) {
            when {
                completed ->
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(DesignTokens.IconSize.badge)
                                .alpha(checkAlpha),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                active ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(DesignTokens.IconSize.inline),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                else ->
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    ) {}
            }
        }

        Spacer(Modifier.width(DesignTokens.Spacing.m))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (active || completed) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

/**
 * App mark + name, left-aligned at the top of the screen, plus P7.3's "Demo mode" persona-picker
 * affordance on the trailing edge — a small icon button (own design, not a port of the reference
 * app's Corporate/Employee/Merchant portal-selection sheet) showing the currently picked persona's
 * initial once one has been chosen.
 */
@Composable
private fun BrandHeader(
    selectedPersona: DemoAccount?,
    onOpenPersonaPicker: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompassMark(modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.shared_login_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenPersonaPicker) {
            if (selectedPersona != null) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = selectedPersona.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.SwitchAccount,
                    contentDescription = stringResource(Res.string.shared_login_cd_choose_persona),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * PLAN_V22 P7.3: "Demo mode" persona picker. The reference app's portal-selection sheet
 * (Corporate/Employee/Merchant) doesn't map onto Mileway's single-tenant design, but the
 * underlying idea — choose which identity you're entering as before landing on the main screen —
 * maps cleanly onto [P1.1's][com.mileway.core.data.model.db.MockAccountEntity] seeded personas.
 * Own bottom-sheet design language (radio-selectable list, matching [AccountDetailsSheet]'s
 * idiom), not a port of the reference app's portal-tile UI. No external navigation or Custom
 * Tabs — Mileway has no portals to link to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoModePersonaPickerSheet(
    personas: List<DemoAccount>,
    selectedPersonaId: String?,
    onSelect: (accountId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.shared_login_demo_mode_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = stringResource(Res.string.shared_login_demo_mode_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            personas.forEach { persona ->
                PersonaPickerRow(
                    persona = persona,
                    isSelected = persona.id == selectedPersonaId,
                    onClick = { onSelect(persona.id) },
                )
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
            }
        }
    }
}

/**
 * PLAN_V24 P1.6: duplicate-account resolution (the reference app `MultipleAccountsActivity`). Shown after a
 * phone-OTP login when the verified number is already registered to existing personas — the
 * reviewer either continues as one of them or creates a new identity. Own bottom-sheet design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicateAccountsSheet(
    accounts: List<DemoAccount>,
    onContinueAs: (DemoAccount) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.shared_login_duplicate_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = stringResource(Res.string.shared_login_duplicate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            accounts.forEach { account ->
                PersonaPickerRow(
                    persona = account,
                    isSelected = false,
                    onClick = { onContinueAs(account) },
                )
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))
            TextButton(
                onClick = onCreateNew,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.shared_login_duplicate_create_new))
            }
        }
    }
}

/** One selectable row in [DemoModePersonaPickerSheet]: avatar, name/organization, radio button. */
@Composable
private fun PersonaPickerRow(
    persona: DemoAccount,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = DesignTokens.Shape.roundedMd,
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize.actionTile),
            )
            Spacer(Modifier.width(DesignTokens.Spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (persona.organization.isNotBlank()) {
                    Text(
                        text = persona.organization,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

/**
 * Auto-advancing welcome carousel. Each slide is a drawn illustration plus copy; the shared
 * [DotsIndicator] tracks the current page. The pager advances on a timer keyed to the current
 * page, so a manual swipe resets the countdown naturally.
 */
@Composable
private fun OnboardingPager() {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_SLIDES.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        delay(ONBOARDING_AUTO_ADVANCE_MS)
        val next = (pagerState.currentPage + 1) % ONBOARDING_SLIDES.size
        pagerState.animateScrollToPage(next)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            OnboardingSlideContent(
                slide = ONBOARDING_SLIDES[page],
                onTap = {
                    val next = (page + 1) % ONBOARDING_SLIDES.size
                    coroutineScope.launch { pagerState.animateScrollToPage(next) }
                },
            )
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        DotsIndicator(
            pageCount = ONBOARDING_SLIDES.size,
            selectedIndex = pagerState.currentPage,
            onDotClick = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
        )
    }
}

/** A single onboarding slide: tinted icon disc, title, and caption. Tapping advances the pager. */
@Composable
private fun OnboardingSlideContent(
    slide: OnboardingSlide,
    onTap: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.s)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                )
                .semantics { contentDescription = "${slide.title}. ${slide.caption}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(DesignTokens.Spacing.s))

        Text(
            text = slide.caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
    }
}

/**
 * Email + password fields. Validation is non-empty only; errors surface only after the user
 * has attempted to submit (driven by the [emailError]/[passwordError] flags) to avoid shouting
 * at an untouched form.
 */
@Composable
private fun CredentialFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    emailError: Boolean,
    passwordError: Boolean,
    enabled: Boolean,
    onImeDone: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            isError = emailError,
            label = { Text(stringResource(Res.string.auth_email)) },
            placeholder = { Text(DEMO_EMAIL) },
            leadingIcon = {
                Icon(Icons.Outlined.MailOutline, contentDescription = null)
            },
            supportingText =
                if (emailError) {
                    { Text(stringResource(Res.string.shared_auth_email_error)) }
                } else {
                    null
                },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            shape = DesignTokens.Shape.roundedMd,
        )

        Spacer(Modifier.height(DesignTokens.Spacing.m))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            isError = passwordError,
            label = { Text(stringResource(Res.string.auth_password)) },
            placeholder = { Text(stringResource(Res.string.shared_auth_password_placeholder)) },
            leadingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector =
                            if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                        contentDescription =
                            if (passwordVisible) {
                                stringResource(Res.string.shared_auth_hide_password)
                            } else {
                                stringResource(Res.string.shared_auth_show_password)
                            },
                    )
                }
            },
            visualTransformation =
                if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            supportingText =
                if (passwordError) {
                    { Text(stringResource(Res.string.shared_auth_password_error)) }
                } else {
                    null
                },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onImeDone() }),
            shape = DesignTokens.Shape.roundedMd,
        )
    }
}

/**
 * PLAN_V24 P1.1: email ↔ phone mode switch, shown only when the `phoneLoginEnabled` plugin is on
 * (Super-App Consumer persona). A pair of text buttons rather than a segmented control — matches
 * Mileway's minimal auth idiom.
 */
@Composable
private fun AuthModeToggle(
    usePhoneMode: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onToggle(false) }, enabled = enabled && usePhoneMode) {
            Text(stringResource(Res.string.shared_auth_use_email))
        }
        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onToggle(true) }, enabled = enabled && !usePhoneMode) {
            Text(stringResource(Res.string.shared_auth_use_phone))
        }
    }
}

/**
 * PLAN_V24 P1.1: phone + country-code entry with the reference app' validation rules ([PhoneNumberValidator]),
 * a "Send OTP" action wired to the shared [com.mileway.core.data.otp.LocalOtpEngine], and — since
 * this is an offline demo — the dispatched code surfaced inline. The full 6-box entry + verify is
 * P1.2; here Send OTP proves the number is valid and dispatches the deterministic code.
 */
@Composable
private fun PhoneLoginFields(
    country: CountryDialCode,
    onCountryChange: (CountryDialCode) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    validation: PhoneValidation?,
    enabled: Boolean,
    delivery: com.mileway.core.data.otp.OtpDelivery?,
    onSendOtp: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = country.dialCode,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(country.isoCode) },
                    modifier =
                        Modifier
                            .width(104.dp)
                            .clickable(enabled = enabled) { menuOpen = true },
                    shape = DesignTokens.Shape.roundedMd,
                )
                // A transparent overlay guarantees the tap opens the menu even over the field.
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clickable(enabled = enabled) { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    LOGIN_COUNTRY_CODES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.name} (${option.dialCode})") },
                            onClick = {
                                onCountryChange(option)
                                menuOpen = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.width(DesignTokens.Spacing.m))

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                isError = validation is PhoneValidation.Empty || validation is PhoneValidation.WrongLength,
                label = { Text(stringResource(Res.string.shared_auth_phone_label)) },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                supportingText =
                    when (validation) {
                        is PhoneValidation.Empty -> {
                            { Text(stringResource(Res.string.shared_auth_phone_error_empty)) }
                        }
                        is PhoneValidation.WrongLength -> {
                            { Text(stringResource(Res.string.shared_auth_phone_error_length)) }
                        }
                        else -> null
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSendOtp() }),
                shape = DesignTokens.Shape.roundedMd,
            )
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        Button(
            onClick = onSendOtp,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            shape = DesignTokens.Shape.roundedMd,
        ) {
            Text(stringResource(Res.string.shared_auth_send_otp), fontWeight = FontWeight.SemiBold)
        }

        if (delivery != null) {
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
                    Text(
                        text = stringResource(Res.string.shared_auth_otp_sent, delivery.target),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(Res.string.shared_auth_demo_code, delivery.code),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
