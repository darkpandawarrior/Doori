package com.mileway.feature.approvals.delegate

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/** Shows both identities before an action is persisted with dual attribution. */
@Composable
fun DelegateBanner(onBehalfOf: String, modifier: Modifier = Modifier) {
    Text("Acting on behalf of $onBehalfOf. Your identity will also be recorded.", modifier = modifier)
}

@Preview
@Composable
private fun DelegateBannerPreview() {
    MaterialTheme { DelegateBanner("approver-1") }
}
