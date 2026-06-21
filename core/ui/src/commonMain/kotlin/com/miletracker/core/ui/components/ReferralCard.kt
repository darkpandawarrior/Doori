package com.miletracker.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * RF.4: "refer a friend" section: shows the user's own code, a Share button (routed to the platform
 * [com.miletracker.core.platform.ShareSheet]), and a field to redeem someone else's code (local mock).
 * Pure/stateless so the hosting screen owns the manager + share wiring.
 */
@Composable
fun ReferralCard(
    myCode: String,
    onShare: () -> Unit,
    onRedeem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf("") }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Refer a friend", style = MaterialTheme.typography.titleMedium)
            Text("Your referral code", style = MaterialTheme.typography.labelMedium)
            Text(
                text = myCode.ifEmpty { "…" },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text("Share invite")
            }
            OutlinedTextField(
                value = entered,
                onValueChange = { entered = it },
                label = { Text("Enter a friend's code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    onRedeem(entered)
                    entered = ""
                },
                enabled = entered.isNotBlank(),
            ) {
                Text("Redeem")
            }
        }
    }
}

/** Pure invite-message builder (embeds the referral code + the App-Links URL). */
fun buildReferralInvite(
    code: String,
    linkBase: String = "https://miletracker.example.com",
): String = "Join me on Mileway! Use my referral code $code or open $linkBase/referral?code=$code"
