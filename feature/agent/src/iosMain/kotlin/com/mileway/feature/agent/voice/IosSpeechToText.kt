package com.mileway.feature.agent.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class IosSpeechToText : SpeechToText {
    override fun listen(): Flow<SpeechEvent> = emptyFlow() // TODO(ios): SFSpeechRecognizer
    override fun stop() {}
}
