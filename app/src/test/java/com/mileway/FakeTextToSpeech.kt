package com.mileway

import com.mileway.feature.agent.voice.TextToSpeech

class FakeTextToSpeech : TextToSpeech {
    override suspend fun speak(text: String) {}
    override fun stop() {}
}
