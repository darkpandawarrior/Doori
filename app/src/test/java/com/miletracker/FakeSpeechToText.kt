package com.miletracker

import com.miletracker.feature.agent.voice.SpeechEvent
import com.miletracker.feature.agent.voice.SpeechToText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeSpeechToText : SpeechToText {
    override fun listen(): Flow<SpeechEvent> = emptyFlow()
    override fun stop() {}
}
