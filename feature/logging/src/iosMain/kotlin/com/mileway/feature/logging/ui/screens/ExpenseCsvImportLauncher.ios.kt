package com.mileway.feature.logging.ui.screens

import androidx.compose.runtime.Composable

/**
 * iOS actual: no native document-picker UI hook exists yet from a `ComposeUIViewController` host
 * (same documented gap as `core/platform`'s `IosDocumentScanner` — presenting a
 * `UIDocumentPickerViewController` needs a live `UIViewController` to present from, which isn't
 * wired here). Returns a callback that is a truthful no-op — [onPicked] is never invoked — rather
 * than a fragile stand-in.
 */
@Composable
actual fun rememberExpenseCsvImportLauncher(onPicked: (String) -> Unit): () -> Unit = { }
