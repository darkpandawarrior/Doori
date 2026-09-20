package com.mileway.feature.profile.di

import com.mileway.feature.profile.viewmodel.StorageManagementViewModel
import com.mileway.feature.profile.viewmodel.StorageViewModel
import com.siddharth.kmp.ai.MediaPipeModelManager
import com.siddharth.kmp.ai.onDeviceLlmModule
import com.siddharth.kmp.designsystem.ai.AiSettingsState
import com.siddharth.kmp.llmchat.SecureKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The Android-only remainder of [profileModule], split out so the rest could be hoisted to
 * commonMain and registered on iOS. Register it alongside [profileModule] on Android.
 *
 * Two reasons a binding is down here, not up there:
 *  - the AI settings card — `onDeviceLlmModule`/`SecureKeyStore`/`AiSettingsState` come from the
 *    toolkit's Android artifacts, and `SecureKeyStore` needs `androidContext()`;
 *  - the storage ViewModels — core:data's `StorageRepository` takes an `android.content.Context`,
 *    so both they and the screens that use them are still Android-only.
 */
val profileAndroidModule =
    module {
        // AI Settings card (consent, on-device model download, BYOK cloud key). Binds
        // ModelManager/OnDeviceLlm — MediaPipe Gemma + ML Kit GenAI, detection-ordered — the same
        // real seam feature:agent's own LlmGateway wraps a narrower slice of; see the lane brief
        // for why this settles for exposing it rather than also re-routing feature:agent's chat
        // through it.
        includes(onDeviceLlmModule())
        single { SecureKeyStore(androidContext()) }
        single {
            val keyStore = get<SecureKeyStore>()
            AiSettingsState(
                modelManager = get(),
                manifest = listOf(MediaPipeModelManager.GEMMA_3_1B),
                onDeviceLlm = get(),
                getKey = keyStore::getKey,
                setKey = keyStore::setKey,
                // ponytail: process-lifetime scope, same lifecycle as every other `single` in this
                // module — AiSettingsState has no per-screen state to tear down (downloads survive
                // navigating away by design, see its pauseDownload KDoc).
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            )
        }
        // P6.6: Preferences' Storage tile/sheet (real cache-size readout + clear-cache action).
        viewModelOf(::StorageViewModel)
        // P31.MISC.2: the full tiered storage-management screen (Safe/Caution/Danger clearers).
        viewModelOf(::StorageManagementViewModel)
    }
