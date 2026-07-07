package com.mileway.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.network.model.DemoAccount
import com.mileway.feature.profile.repository.MockAccountRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * One named step in the staged sign-in sequence, e.g. "Validating credentials…".
 *
 * @property label copy shown next to the step's checklist row.
 * @property durationMs how long this step stays active before advancing to the next one.
 */
data class AuthSignInStep(
    val label: String,
    val durationMs: Long,
)

/**
 * P7.2: the staged sign-in sequence a reviewer walks through on tapping "Sign In" / "Continue as
 * guest". Models the reference app's staged-overlay *pattern* — a driven multi-step checklist
 * rather than a single flat spinner — with no real network/OAuth mechanics behind it (there is
 * nothing to redirect to; every step is a scripted local delay).
 */
internal val SIGN_IN_STEPS =
    listOf(
        AuthSignInStep(label = "Validating credentials…", durationMs = 350L),
        AuthSignInStep(label = "Preparing local session…", durationMs = 350L),
        AuthSignInStep(label = "Done", durationMs = 200L),
    )

/**
 * Sealed sign-in state machine driving [LoginScreen]'s stepped-progress overlay.
 *
 * - [Idle]: the form is interactive, no sign-in attempt in flight.
 * - [Loading]: stepping through [AuthSignInStep]s; `step` is the 1-based index of the step
 *   currently active out of `totalSteps`.
 * - [Success]: every step completed; the screen should invoke the caller's completion callback.
 * - [Error]: reserved for a future real validation failure path — unused by [beginSignIn] today
 *   (every scripted step always succeeds), but keeps the state machine honest about the shape a
 *   real backend-backed flow would need.
 */
sealed interface MilewayAuthState {
    data object Idle : MilewayAuthState

    data class Loading(
        val step: Int,
        val totalSteps: Int,
        val label: String,
    ) : MilewayAuthState

    data object Success : MilewayAuthState

    data class Error(val message: String) : MilewayAuthState
}

/**
 * Drives [MilewayAuthState] through the scripted [SIGN_IN_STEPS] sequence.
 *
 * The screen calls [beginSignIn] once per tap; a second tap while already [MilewayAuthState.Loading]
 * is a no-op (the in-flight [Job] guards re-entrancy) so a double-tap can't restart the sequence
 * or overlap two runs. [reset] returns to [MilewayAuthState.Idle], used if the screen is left before
 * the sequence completes (e.g. process recreation of a fresh composition).
 *
 * PLAN_V22 P7.3: also drives [LoginScreen]'s "Demo mode" persona picker. [personas] exposes the
 * same Room-backed, P1.1-seeded personas [com.mileway.feature.profile.viewmodel.ProfileViewModel]
 * switches between; [selectPersona] records which one the reviewer picked *before* tapping Sign
 * In. If a persona was picked, [beginSignIn] marks it active (via [MockAccountRepository.setActive]
 * and [ActiveAccountSource.setActiveAccountId] — the same two calls
 * [com.mileway.feature.profile.viewmodel.ProfileViewModel]'s `CommitAccountSwitch` makes) before
 * reporting success, so that persona is what's active once the app shell loads. Picking nothing
 * (the default, no-picker-interaction path) leaves today's behavior unchanged: whichever account
 * was already active stays active.
 */
class AuthViewModel(
    private val mockAccountRepository: MockAccountRepository,
    private val activeAccountSource: ActiveAccountSource,
) : ViewModel() {
    private val _state = MutableStateFlow<MilewayAuthState>(MilewayAuthState.Idle)
    val state: StateFlow<MilewayAuthState> = _state.asStateFlow()

    /** Live, DAO-ordered list of switchable demo personas the picker offers. */
    val personas: StateFlow<List<DemoAccount>> =
        mockAccountRepository
            .observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedPersonaId = MutableStateFlow<String?>(null)

    /** The persona id picked from the "Demo mode" sheet, or null if the reviewer never opened it. */
    val selectedPersonaId: StateFlow<String?> = _selectedPersonaId.asStateFlow()

    private var signInJob: Job? = null

    init {
        viewModelScope.launch { mockAccountRepository.seedIfEmpty() }
    }

    /** Records the picked persona; the picker sheet then dismisses itself. */
    fun selectPersona(accountId: String) {
        _selectedPersonaId.value = accountId
    }

    /** Begins the staged sign-in sequence. No-op if a sequence is already in flight. */
    fun beginSignIn() {
        if (_state.value is MilewayAuthState.Loading) return
        signInJob =
            viewModelScope.launch {
                SIGN_IN_STEPS.forEachIndexed { index, step ->
                    _state.value =
                        MilewayAuthState.Loading(
                            step = index + 1,
                            totalSteps = SIGN_IN_STEPS.size,
                            label = step.label,
                        )
                    delay(step.durationMs)
                }
                _selectedPersonaId.value?.let { accountId ->
                    mockAccountRepository.setActive(accountId)
                    activeAccountSource.setActiveAccountId(accountId)
                }
                _state.value = MilewayAuthState.Success
            }
    }

    /** Returns to [MilewayAuthState.Idle], cancelling any in-flight sequence. */
    fun reset() {
        signInJob?.cancel()
        signInJob = null
        _state.value = MilewayAuthState.Idle
    }
}

/** Koin module for [AuthViewModel] — registered alongside the other screen-scoped ViewModels. */
val authModule =
    module {
        viewModelOf(::AuthViewModel)
    }
