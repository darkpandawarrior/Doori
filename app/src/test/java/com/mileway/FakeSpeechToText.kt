package com.mileway

import com.mileway.feature.agent.voice.SpeechEvent
import com.mileway.feature.agent.voice.SpeechToText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeSpeechToText : SpeechToText {
    override fun listen(): Flow<SpeechEvent> = emptyFlow()
    override fun stop() {}
}
