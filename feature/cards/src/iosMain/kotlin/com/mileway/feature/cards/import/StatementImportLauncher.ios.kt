package com.mileway.feature.cards.import

import androidx.compose.runtime.Composable

/** The current iOS host has no raw document-picker hook; the shared text importer remains usable. */
@Composable
internal actual fun rememberStatementImportLauncher(
    onPicked: (String, String) -> Unit,
    onError: () -> Unit,
): (() -> Unit)? = null
