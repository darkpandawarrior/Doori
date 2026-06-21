package com.miletracker.core.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared in-app locale state (UX.6), the single source of truth for the currently-selected language tag,
 * held app-wide as a Koin single so any feature can observe it (the cards module already reads
 * `currentLocaleTag()` for its mock data; pointing that at this flow keeps everything in sync).
 *
 * State only: the *platform apply* stays platform-specific, Android calls `AppCompatDelegate
 * .setApplicationLocales` (the cross-API per-app-locale API, already wired in Settings) right after
 * [setLocale]; iOS observes [currentTag] (a full `NSUserDefaults` AppleLanguages apply needs an app
 * relaunch). This avoids pulling appcompat into a core module just to re-implement the apply.
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
