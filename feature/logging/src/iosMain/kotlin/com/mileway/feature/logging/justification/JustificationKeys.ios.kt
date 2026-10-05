package com.mileway.feature.logging.justification

import com.siddharth.kmp.llmchat.ProviderId
import org.koin.core.Koin

internal actual fun justificationKeys(koin: Koin): (ProviderId) -> String? = { null }
