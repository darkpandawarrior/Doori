package com.mileway.showcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mileway.core.ui.theme.MilewayTheme

/**
 * Debug-only activity that provides a Showkase-style browser for all
 * major composables in the app. Launched from the Debug Menu or via
 * the launcher shortcut registered in the debug manifest.
 */
class ComponentShowcaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MilewayTheme {
                ComponentShowcaseScreen(onBack = { finish() })
            }
        }
    }
}
