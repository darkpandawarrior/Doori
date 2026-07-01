package com.mileway.feature.logging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mileway.core.ui.AppHost
import com.mileway.feature.logging.ui.screens.LogMilesScreen

class LoggingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppHost { LogMilesScreen() } }
    }
}
