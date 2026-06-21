@file:Suppress("ktlint:standard:max-line-length")

package com.miletracker.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.CollapsibleSectionCard
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import kotlinx.coroutines.launch

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
fun HelpScreen(onBack: () -> Unit) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = "Help & Support",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                placeholder = { Text("Search FAQs…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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
                        "No results for \"$searchQuery\"",
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
                        subtitle = "${catFaqs.size} questions",
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Need more help?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Our support team is available Mon–Fri, 9 AM–6 PM IST.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { scope.launch { snackbarHostState.showSnackbar("Email sent to support@miletracker.app") } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Contact Support")
                    }
                    Button(
                        onClick = { scope.launch { snackbarHostState.showSnackbar("Bug report submitted: thank you!") } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text("Report a Bug")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
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
        androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Show less" else "Show answer", style = MaterialTheme.typography.labelSmall)
        }
    }
}
