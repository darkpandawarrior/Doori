package com.mileway.feature.cards.data

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun currentLocaleTag(): String = NSLocale.currentLocale.languageCode
