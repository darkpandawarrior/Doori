package com.mileway.feature.logging.ui.screens

import androidx.compose.runtime.Composable

/**
 * Opens the platform's file picker for a CSV/TSV bulk-import file (P2.4). Returns a callback that
 * launches the picker; [onPicked] is invoked with the picked file's raw text contents once the
 * user selects one (never invoked on cancel or a read failure).
 *
 * Android: `ActivityResultContracts.GetContent()` (system Storage Access Framework picker, no
 * storage permission needed).
 *
 * iOS: no UI hook exists yet for a native document-picker launch from a `ComposeUIViewController`
 * host (mirrors the documented gap in `core/platform`'s `IosDocumentScanner` — V26 P26.SITE.2
 * closed the equivalent gap in [rememberReceiptAttachmentLauncher] via `core:media`'s Peekaboo
 * gallery picker, but that doesn't cover raw-file/CSV picking) — returns a truthful no-op until a
 * document-picker hook lands, rather than a fragile stand-in.
 */
@Composable
expect fun rememberExpenseCsvImportLauncher(onPicked: (String) -> Unit): () -> Unit
