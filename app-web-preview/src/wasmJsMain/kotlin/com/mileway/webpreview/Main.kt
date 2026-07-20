package com.mileway.webpreview

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // A dedicated mount point, not document.body: ComposeViewport clears its container's children,
    // and index.html keeps a loader + LiveEmbed decoy canvas alive as siblings.
    ComposeViewport(document.getElementById("compose-root")!!) {
        MilewayWebPreviewApp()
    }
}
