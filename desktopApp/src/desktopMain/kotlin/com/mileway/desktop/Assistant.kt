package com.mileway.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mileway.core.ai.assistant.buildCloudFallback
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.theme.DesignTokens
import com.siddharth.kmp.designsystem.ai.AiSettingsSection
import com.siddharth.kmp.designsystem.ai.AiSettingsState
import com.siddharth.kmp.llmchat.ProviderId
import com.siddharth.kmp.result.fold
import kotlinx.coroutines.launch

/**
 * PLAN_V23 D.2 follow-up (lane mileway-ai-settings-and-desktop): the assistant "quietly does not
 * exist" on desktop today (see the lane brief's audit of feature:agent) — this is the minimal real
 * version rather than porting feature:agent's whole chat screen + its four feature-module data
 * dependencies (approvals/cards/advances/logging), which desktopApp deliberately doesn't carry
 * (PLAN_V23 D.1 Option b).
 *
 * No on-device model on the JVM (no Nano/Foundation-Models equivalent), so [aiSettingsState]'s
 * on-device card always reads "No downloadable model on this platform" — the BYOK cloud key this
 * same [AiSettingsSection] manages is the only way this card ever answers a prompt, via
 * [buildCloudFallback].
 */
@Composable
fun AssistantCard(
    aiSettingsState: AiSettingsState,
    getProviderKey: (ProviderId) -> String?,
) {
    // collectAsState (not collectAsStateWithLifecycle): this app has no Activity/Fragment
    // lifecycle to pause/resume against — it's a plain JVM window — and core:ui only exposes
    // lifecycle-viewmodel-compose as `implementation`, not `api`, so it isn't on this module's
    // compile classpath transitively anyway.
    val uiState by aiSettingsState.uiState.collectAsState()
    var prompt by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<String?>(null) }
    var asking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SectionCard(title = "Assistant") {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            AiSettingsSection(
                uiState = uiState,
                onConsentChange = aiSettingsState::setAiConsent,
                onStartDownload = aiSettingsState::startDownload,
                onPauseDownload = aiSettingsState::pauseDownload,
                onDeleteModel = aiSettingsState::deleteModel,
                onSelectProvider = aiSettingsState::selectProvider,
                onProviderKeyChange = aiSettingsState::setProviderKey,
                onClearProviderKey = aiSettingsState::clearProviderKey,
                onTestProviderKey = aiSettingsState::testKey,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask the assistant…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = prompt.isNotBlank() && !asking && uiState.aiConsentGiven,
                    onClick = {
                        val question = prompt
                        asking = true
                        reply = null
                        // Rebuilt on every ask (not hoisted): buildCloudFallback reads the BYOK
                        // key(s) at construction time, and AiSettingsSection above can change them
                        // any time between two asks — a cached instance would answer with a stale
                        // or since-cleared key.
                        val fallback = buildCloudFallback(getProviderKey, uiState.selectedProvider)
                        scope.launch {
                            val result = fallback.generate(question)
                            reply = result.fold(onSuccess = { it }, onFailure = { failure -> "(${failure.name}) no reply" })
                            asking = false
                        }
                    },
                ) { Text("Ask") }
            }
            reply?.let { text -> Text(text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
