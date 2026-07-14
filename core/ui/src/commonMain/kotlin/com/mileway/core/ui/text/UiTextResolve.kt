package com.mileway.core.ui.text

import androidx.compose.runtime.Composable
import com.siddharth.kmp.common.UiText
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves a [UiText] to a locale-aware display string using the shared Compose Multiplatform
 * resource table ([Res]). This makes [UiText.Res] *real*: ViewModels expose keys (surviving locale
 * / config changes), and the UI resolves them at the edge — the recommended KMP/CMP pattern. An
 * unknown key falls back to the key itself (visible-in-dev, never crashes).
 */
@Composable
fun UiText.text(): String =
    when (this) {
        is UiText.Dynamic -> value
        is UiText.Res ->
            Res.allStringResources[key]
                ?.let { stringResource(it, *args.toTypedArray()) }
                ?: key
        UiText.Empty -> ""
    }

/**
 * Non-composable (suspend) resolver — for the rare case a ViewModel/effect needs the resolved
 * string off the UI edge (e.g. a notification body). Prefer [text] inside composables.
 */
suspend fun UiText.getText(): String =
    when (this) {
        is UiText.Dynamic -> value
        is UiText.Res ->
            Res.allStringResources[key]
                ?.let { getString(it, *args.toTypedArray()) }
                ?: key
        UiText.Empty -> ""
    }
