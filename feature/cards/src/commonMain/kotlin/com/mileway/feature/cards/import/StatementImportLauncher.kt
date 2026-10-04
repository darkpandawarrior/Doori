package com.mileway.feature.cards.import

import androidx.compose.runtime.Composable

/** Reads a bounded local UTF-8 statement; null means the host supports pasted text only. */
@Composable
internal expect fun rememberStatementImportLauncher(
    onPicked: (String, String) -> Unit,
    onError: () -> Unit,
): (() -> Unit)?
