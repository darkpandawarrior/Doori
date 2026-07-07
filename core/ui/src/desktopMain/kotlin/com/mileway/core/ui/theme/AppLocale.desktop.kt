package com.mileway.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private val default: String = Locale.getDefault().toString()
    private val local = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = local.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = if (value.isNullOrBlank()) default else value
        Locale.setDefault(Locale(new))
        return local provides new
    }
}
