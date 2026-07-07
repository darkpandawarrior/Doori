package com.mileway.core.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared in-app locale state (UX.6), the single source of truth for the currently-selected language tag,
 * held app-wide as a Koin single so any feature can observe it (the cards module already reads
 * `currentLocaleTag()` for its mock data; pointing that at this flow keeps everything in sync).
 *
 * State only: the *in-app apply* is driven by the `LocalAppLocale` Compose environment
 * (`AppLocaleEnvironment`), which observes [currentTag] and re-resolves every `stringResource` on
 * both Android and iOS instantly — no restart. Android additionally calls `AppCompatDelegate
 * .setApplicationLocales` in Settings so the OS per-app locale (and non-Compose resources) match;
 * iOS mirrors the tag into `NSUserDefaults` AppleLanguages for the same reason. Keeping this class
 * state-only avoids pulling appcompat into a core module.
 */
class LocaleController(initialTag: String = AppLanguage.ENGLISH.tag) {
    private val _currentTag = MutableStateFlow(initialTag)
    val currentTag: StateFlow<String> = _currentTag.asStateFlow()

    /** The resolved [AppLanguage] for the current tag (falls back to English for unknown tags). */
    val currentLanguage: AppLanguage
        get() = AppLanguage.entries.firstOrNull { it.tag == _currentTag.value } ?: AppLanguage.ENGLISH

    fun setLocale(tag: String) {
        _currentTag.value = tag
    }

    fun setLanguage(language: AppLanguage) = setLocale(language.tag)
}
