package com.mileway.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import org.koin.compose.koinInject

/**
 * Overrides the Compose Multiplatform resource locale at runtime so a user-picked language switches
 * every `stringResource()` instantly — with **no app restart** — on Android, iOS and Desktop. This
 * is the cross-platform *apply* that pairs with [LocaleController]'s state (which owns the selected
 * tag + persistence). Implemented per platform via `expect`/`actual`, since each needs its own
 * native locale API (JVM `Locale.setDefault`, iOS `NSUserDefaults` AppleLanguages).
 *
 * (Temporary community pattern until Compose Multiplatform ships a public resource-locale API.)
 */
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/**
 * Wraps the app root so the [LocaleController]'s selected language drives every resource lookup.
 * The `key(tag)` restarts the subtree on change, so all strings re-resolve immediately. Wrap once
 * at the composition root (Android host + iOS `AppHost`).
 */
@Composable
fun AppLocaleEnvironment(content: @Composable () -> Unit) {
    val localeController = koinInject<LocaleController>()
    val tag by localeController.currentTag.collectAsState()
    CompositionLocalProvider(LocalAppLocale provides tag) {
        key(tag) { content() }
    }
}
