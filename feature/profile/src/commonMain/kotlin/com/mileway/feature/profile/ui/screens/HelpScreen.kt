@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_help_back
import com.mileway.core.ui.resources.profile_help_bug_submitted
import com.mileway.core.ui.resources.profile_help_cancel
import com.mileway.core.ui.resources.profile_help_clear
import com.mileway.core.ui.resources.profile_help_contact_support
import com.mileway.core.ui.resources.profile_help_describe_issue
import com.mileway.core.ui.resources.profile_help_my_tickets_count
import com.mileway.core.ui.resources.profile_help_need_more_help
import com.mileway.core.ui.resources.profile_help_no_results
import com.mileway.core.ui.resources.profile_help_pause
import com.mileway.core.ui.resources.profile_help_play
import com.mileway.core.ui.resources.profile_help_questions_count
import com.mileway.core.ui.resources.profile_help_report_bug
import com.mileway.core.ui.resources.profile_help_resume
import com.mileway.core.ui.resources.profile_help_search_placeholder
import com.mileway.core.ui.resources.profile_help_show_answer
import com.mileway.core.ui.resources.profile_help_show_less
import com.mileway.core.ui.resources.profile_help_subject
import com.mileway.core.ui.resources.profile_help_submit_ticket
import com.mileway.core.ui.resources.profile_help_subtitle
import com.mileway.core.ui.resources.profile_help_support_hours
import com.mileway.core.ui.resources.profile_help_ticket_submitted
import com.mileway.core.ui.resources.profile_help_title
import com.mileway.core.ui.resources.profile_help_video_tutorials
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.data.VIDEO_TUTORIALS
import com.mileway.feature.profile.data.VideoTutorial
import com.mileway.feature.profile.viewmodel.SupportTicketViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private data class Faq(val q: String, val a: String, val category: String)

private val ALL_FAQS =
    listOf(
        // Getting Started
        Faq(
            "What permissions does Mileway need?",
            "Location (Fine + Background) for continuous GPS tracking. Notification permission on Android 13+ for the tracking notification. Camera for odometer photo capture.",
            "Getting Started",
        ),
        Faq(
            "How do I start my first tracking session?",
            "Open the Track tab, tap 'Start Journey', grant location permission if prompted, and drive. Tap the floating bubble to pause or stop.",
            "Getting Started",
        ),
        Faq(
            "Where can I find the setup guide?",
            "Tap 'Setup Guide' in the Quick Links section at the bottom of this screen or visit Settings → Demo Settings for interactive feature flags.",
            "Getting Started",
        ),
        Faq(
            "Can I switch between accounts?",
            "Yes: go to Profile and tap your name to switch between saved demo accounts. Each account maintains its own journey history.",
            "Getting Started",
        ),
        // Track Miles
        Faq(
            "Why is GPS accuracy low?",
            "Ensure Location is set to Fine (not Coarse), background access is allowed, and Battery Optimisation is off for this app. The GPS Drift Simulation toggle in Demo Settings adds artificial noise.",
            "Track Miles",
        ),
        Faq(
            "How do I pause and resume tracking?",
            "Tap the pause button in the floating bubble or on the Live Tracking screen. The timer stops, but your route is preserved. Tap Resume to continue.",
            "Track Miles",
        ),
        Faq(
            "What is the Odometer feature?",
            "On the Submission screen, tap the camera button to photograph your vehicle's odometer. OCR reads the value and calculates distance as a cross-check.",
            "Track Miles",
        ),
        Faq(
            "Does tracking work offline?",
            "Yes. All GPS data is stored locally in Room. You can track with no network; the data persists across app restarts.",
            "Track Miles",
        ),
        Faq(
            "What happens to draft journeys?",
            "Drafts appear in Saved Tracks with a Draft badge. You can submit or discard them. The Auto-Discard toggle in Demo Settings discards active journeys at 22:00.",
            "Track Miles",
        ),
        // Log Miles
        Faq(
            "How do I log miles manually?",
            "Go to the Log tab, enter start/end locations and the distance, select your vehicle, and submit. No GPS needed.",
            "Log Miles",
        ),
        Faq(
            "Why is my distance rejected?",
            "Policy limits check the entered distance against the straight-line distance between locations. Large deviations flag a policy violation.",
            "Log Miles",
        ),
        Faq(
            "Can I save frequent routes?",
            "Frequent routes are listed in the Log Miles start screen. Tap a route to prefill the form. Add new routes via the + button.",
            "Log Miles",
        ),
        // Expenses & Approvals
        Faq(
            "How do I submit a mileage expense?",
            "After tracking or logging a journey, tap 'Submit' on the track detail screen, fill in the submission form, and tap 'Submit Claim'.",
            "Expenses & Approvals",
        ),
        Faq(
            "What does a policy violation mean?",
            "A violation is flagged when a journey exceeds the reimbursement cap, uses a disallowed vehicle, or the GPS data quality is poor (e.g., mock location detected).",
            "Expenses & Approvals",
        ),
        Faq(
            "How do vouchers work?",
            "Go to an approved journey → Create Voucher. Select multiple submitted expenses, give the voucher a title, and submit. Vouchers are stored locally in this demo.",
            "Expenses & Approvals",
        ),
        Faq(
            "How do approvals work?",
            "Submitted expenses appear in your manager's Approvals queue. They can approve, reject, or seek clarification. Status updates appear in your Saved Tracks list.",
            "Expenses & Approvals",
        ),
        // Account & Settings
        Faq(
            "Can I change the app theme?",
            "Yes. Go to Profile → Settings → Appearance. Choose Light, Dark, or System Default. The theme is persisted in DataStore.",
            "Account & Settings",
        ),
        Faq(
            "How do I export my data?",
            "On any journey's Hardware Events screen, tap the download icon to export events as CSV or JSON. The share sheet lets you send it via email or Drive.",
            "Account & Settings",
        ),
        Faq(
            "What is multi-account mode?",
            "Demo Settings → Account Switcher lets you switch between demo personas with different journey histories, policies, and approval chains.",
            "Account & Settings",
        ),
    )

private val CATEGORIES = listOf("Getting Started", "Track Miles", "Log Miles", "Expenses & Approvals", "Account & Settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenMyTickets: () -> Unit = {},
    viewModel: SupportTicketViewModel = koinViewModel(),
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ticketState by viewModel.state.collectAsState()
    var showContactForm by rememberSaveable { mutableStateOf(false) }
    var subjectInput by rememberSaveable { mutableStateOf("") }
    var bodyInput by rememberSaveable { mutableStateOf("") }
    val ticketSubmittedMsg = stringResource(Res.string.profile_help_ticket_submitted)
    val bugSubmittedMsg = stringResource(Res.string.profile_help_bug_submitted)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_help_title),
                subtitle = stringResource(Res.string.profile_help_subtitle),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.AutoMirrored.Filled.HelpOutline,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_help_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(Res.string.profile_help_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.profile_help_clear))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = DesignTokens.Shape.roundedSm,
            )

            val filtered =
                if (searchQuery.isBlank()) {
                    ALL_FAQS
                } else {
                    ALL_FAQS.filter {
                        it.q.contains(searchQuery, ignoreCase = true) ||
                            it.a.contains(searchQuery, ignoreCase = true)
                    }
                }

            if (searchQuery.isNotBlank()) {
                if (filtered.isEmpty()) {
                    Text(
                        stringResource(Res.string.profile_help_no_results, searchQuery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    filtered.forEach { faq -> FaqItem(faq) }
                }
            } else {
                CATEGORIES.forEach { category ->
                    val catFaqs = ALL_FAQS.filter { it.category == category }
                    CollapsibleSectionCard(
                        title = category,
                        subtitle = stringResource(Res.string.profile_help_questions_count, catFaqs.size),
                        initiallyExpanded = category == "Getting Started",
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            catFaqs.forEach { faq -> FaqItem(faq) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(stringResource(Res.string.profile_help_video_tutorials), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                items(VIDEO_TUTORIALS, key = { it.id }) { tutorial -> VideoTutorialCard(tutorial) }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(Res.string.profile_help_need_more_help), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(Res.string.profile_help_support_hours),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (ticketState.submitError != null) {
                        Text(
                            ticketState.submitError.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (showContactForm) {
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text(stringResource(Res.string.profile_help_subject)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = bodyInput,
                            onValueChange = { bodyInput = it },
                            label = { Text(stringResource(Res.string.profile_help_describe_issue)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            shape = DesignTokens.Shape.button,
                            onClick = {
                                viewModel.submit(subject = subjectInput, body = bodyInput)
                                if (subjectInput.isNotBlank() && bodyInput.isNotBlank()) {
                                    subjectInput = ""
                                    bodyInput = ""
                                    showContactForm = false
                                    scope.launch { snackbarHostState.showSnackbar(ticketSubmittedMsg) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.profile_help_submit_ticket))
                        }
                        TextButton(
                            onClick = {
                                showContactForm = false
                                viewModel.clearSubmitError()
                            },
                            shape = DesignTokens.Shape.button,
                        ) {
                            Text(stringResource(Res.string.profile_help_cancel))
                        }
                    } else {
                        Button(
                            shape = DesignTokens.Shape.button,
                            onClick = { showContactForm = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.profile_help_contact_support))
                        }
                        Button(
                            shape = DesignTokens.Shape.button,
                            onClick = {
                                viewModel.submit(
                                    subject = "Bug report",
                                    body = "Auto-filed from Help & Support's Report a Bug action.",
                                )
                                scope.launch { snackbarHostState.showSnackbar(bugSubmittedMsg) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) {
                            Text(stringResource(Res.string.profile_help_report_bug))
                        }
                        TextButton(onClick = onOpenMyTickets, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.profile_help_my_tickets_count, ticketState.tickets.size))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * A single "Video Tutorials" card — tapping toggles a locally simulated playback progress
 * (see [rememberSimulatedPlayback]), not a real video stream (see [VideoTutorial]'s doc for why).
 */
@Composable
private fun VideoTutorialCard(tutorial: VideoTutorial) {
    val (isPlaying, progress, toggle) = rememberSimulatedPlayback()

    Card(modifier = Modifier.width(180.dp)) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayCircle,
                    contentDescription = if (isPlaying) stringResource(Res.string.profile_help_pause) else stringResource(Res.string.profile_help_play),
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(tutorial.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(tutorial.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(tutorial.durationLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isPlaying || progress > 0f) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = toggle, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    if (isPlaying) {
                        stringResource(Res.string.profile_help_pause)
                    } else if (progress > 0f) {
                        stringResource(Res.string.profile_help_resume)
                    } else {
                        stringResource(Res.string.profile_help_play)
                    },
                )
            }
        }
    }
}

private data class SimulatedPlayback(val isPlaying: Boolean, val progress: Float, val toggle: () -> Unit)

/**
 * Local playback-state simulation for [VideoTutorialCard]: while "playing", [progress] advances
 * from its current value to 1f over a few seconds, then stops — a working local play/pause state
 * without a real video stream or codec dependency (see [VideoTutorial]'s doc).
 */
@Composable
private fun rememberSimulatedPlayback(): SimulatedPlayback {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying && progress < 1f) {
            delay(200L)
            progress = (progress + 0.05f).coerceAtMost(1f)
        }
        if (progress >= 1f) isPlaying = false
    }

    return SimulatedPlayback(
        isPlaying = isPlaying,
        progress = progress,
        toggle = {
            if (progress >= 1f) progress = 0f
            isPlaying = !isPlaying
        },
    )
}

@Composable
private fun FaqItem(faq: Faq) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            faq.q,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        Modifier.padding(bottom = if (expanded) 4.dp else 0.dp),
                    ),
        )
        if (expanded) {
            Text(
                faq.a,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { expanded = !expanded }, shape = DesignTokens.Shape.button) {
            Text(
                if (expanded) stringResource(Res.string.profile_help_show_less) else stringResource(Res.string.profile_help_show_answer),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
