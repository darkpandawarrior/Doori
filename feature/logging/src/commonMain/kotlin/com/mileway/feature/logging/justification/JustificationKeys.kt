package com.mileway.feature.logging.justification

import com.siddharth.kmp.llmchat.ProviderId
import org.koin.core.Koin

/** Reuses the Android profile key store; platforms without that binding use the offline picker. */
internal expect fun justificationKeys(koin: Koin): (ProviderId) -> String?
