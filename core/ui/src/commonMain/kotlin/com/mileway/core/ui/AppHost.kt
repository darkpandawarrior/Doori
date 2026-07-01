package com.mileway.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mileway.core.ui.theme.MilewayTheme

@Composable
fun AppHost(content: @Composable () -> Unit) {
    MilewayTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
