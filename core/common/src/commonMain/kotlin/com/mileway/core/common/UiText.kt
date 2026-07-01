package com.mileway.core.common

/**
 * Platform-agnostic text holder so ViewModel state/effects never carry raw, pre-resolved strings
 * (openMF pattern). [Static] wraps a literal; [Res] names a string key resolved at the UI edge.
 *
 * This is a demo without a shared resource table in `commonMain`, so [asString] falls back to the
 * key for [Res]. A production app would wire [Res] to moko-resources / Compose `stringResource`.
 */
sealed interface UiText {
    data class Static(val value: String) : UiText

    data class Res(val key: String, val args: List<String> = emptyList()) : UiText

    companion object {
        val Empty: UiText = Static("")

        fun of(value: String): UiText = Static(value)
    }
}

fun UiText.asString(): String =
    when (this) {
        is UiText.Static -> value
        is UiText.Res -> key
    }
