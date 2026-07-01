package com.mileway.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

/**
 * Embedded web view screen. Multiplatform (Android WebView / iOS WKWebView via
 * compose-webview-multiplatform). JS is enabled through the common `webSettings` API rather than the
 * Android-only `onCreated { webView.settings }` so this composable lives in commonMain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedWebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
) {
    val state = rememberWebViewState(url)
    val navigator = rememberWebViewNavigator()

    LaunchedEffect(Unit) {
        state.webSettings.isJavaScriptEnabled = true
        state.webSettings.androidWebSettings.domStorageEnabled = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.canGoBack) {
                            navigator.navigateBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize(),
                navigator = navigator,
            )

            if (state.loadingState is LoadingState.Loading) {
                val progress = (state.loadingState as LoadingState.Loading).progress
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                )
            }
        }
    }
}
