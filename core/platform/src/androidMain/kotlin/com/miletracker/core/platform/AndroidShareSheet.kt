package com.miletracker.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Android share via the system chooser (SH.1) — `ACTION_SEND` with optional subject and a `content://`
 * stream. Launched from the application context, so the chooser carries `FLAG_ACTIVITY_NEW_TASK`; a file
 * stream also grants read permission to the receiving app.
 */
class AndroidShareSheet(private val context: Context) : ShareSheet {
    override fun share(
        text: String,
        subject: String?,
        fileUri: String?,
    ) {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = if (fileUri != null) "*/*" else "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                fileUri?.let {
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        val chooser =
            Intent.createChooser(send, subject ?: "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    }
}
