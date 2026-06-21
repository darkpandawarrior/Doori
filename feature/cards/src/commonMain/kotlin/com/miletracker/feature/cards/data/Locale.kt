package com.miletracker.feature.cards.data

/**
 * Q.2: current app/system language tag (e.g. "en", "ar", "hi"). Exposed via an expect/actual so the
 * mock provider factory stays in commonMain. Android: `Locale`; iOS: `NSLocale`.
 */
expect fun currentLocaleTag(): String
