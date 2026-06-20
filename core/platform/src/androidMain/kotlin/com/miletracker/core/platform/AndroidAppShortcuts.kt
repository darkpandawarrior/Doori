package com.miletracker.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Android home-screen quick actions (SH.3) via ShortcutManagerCompat. Each [AppShortcut] becomes a dynamic
 * shortcut whose launch intent is an `ACTION_VIEW` of its `miletracker://…` deep link (package-scoped), so
 * tapping it routes through the app's existing deep-link intent-filter.
 */
class AndroidAppShortcuts(private val context: Context) : AppShortcuts {
    override fun setDynamicShortcuts(shortcuts: List<AppShortcut>) {
        val infos =
            shortcuts.map { shortcut ->
                ShortcutInfoCompat.Builder(context, shortcut.id)
                    .setShortLabel(shortcut.shortLabel)
                    .setLongLabel(shortcut.longLabel)
                    .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass))
                    .setIntent(
                        Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.deepLink)).setPackage(context.packageName),
                    )
                    .build()
            }
        ShortcutManagerCompat.setDynamicShortcuts(context, infos)
    }
}
