package com.miletracker.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidUrlOpener(private val context: Context) : UrlOpener {
    override fun open(url: String) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}
