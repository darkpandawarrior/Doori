package com.mileway.feature.logging.justification

import com.siddharth.kmp.llmchat.ProviderId
import com.siddharth.kmp.llmchat.SecureKeyStore
import org.koin.core.Koin

internal actual fun justificationKeys(koin: Koin): (ProviderId) -> String? = { provider -> koin.getOrNull<SecureKeyStore>()?.getKey(provider) }
